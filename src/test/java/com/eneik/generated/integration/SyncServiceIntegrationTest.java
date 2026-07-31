package com.eneik.generated.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class SyncServiceIntegrationTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private LmsMetadataRepository lmsMetadataRepository;

    @Autowired
    private SyncedRoleRepository syncedRoleRepository;

    @Autowired
    private LocalUserPermissionRepository localUserPermissionRepository;

    @Autowired
    private MockLmsClient mockLmsClient;

    @Autowired
    private MockEiosClient mockEiosClient;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public LmsClient testLmsClient() {
            return new MockLmsClient();
        }

        @Bean
        @Primary
        public EiosClient testEiosClient() {
            return new MockEiosClient();
        }
    }

    static class MockLmsClient implements LmsClient {
        private List<LmsDocumentMetadataDto> list = new ArrayList<>();

        public void setList(List<LmsDocumentMetadataDto> list) {
            this.list = list;
        }

        @Override
        public List<LmsDocumentMetadataDto> fetchUpdatedDocumentMetadata() {
            return list;
        }
    }

    static class MockEiosClient implements EiosClient {
        private List<EiosUserRoleDto> list = new ArrayList<>();

        public void setList(List<EiosUserRoleDto> list) {
            this.list = list;
        }

        @Override
        public List<EiosUserRoleDto> fetchUserRoleChanges() {
            return list;
        }
    }

    @Test
    public void testSyncLmsMetadataUpdatesIndexedInLocalDatabase() {
        // Arrange
        LmsDocumentMetadataDto item1 = new LmsDocumentMetadataDto("doc-777", "subject", "Epidemiology");
        LmsDocumentMetadataDto item2 = new LmsDocumentMetadataDto("doc-777", "type", "Work Program");
        mockLmsClient.setList(List.of(item1, item2));

        // Act
        syncService.syncLmsMetadata();

        // Assert
        Optional<LmsMetadata> optSubject = lmsMetadataRepository.findByExternalIdAndMetadataKey("doc-777", "subject");
        assertThat(optSubject).isPresent();
        assertThat(optSubject.get().getMetadataValue()).isEqualTo("Epidemiology");

        Optional<LmsMetadata> optType = lmsMetadataRepository.findByExternalIdAndMetadataKey("doc-777", "type");
        assertThat(optType).isPresent();
        assertThat(optType.get().getMetadataValue()).isEqualTo("Work Program");

        // Act with an update
        LmsDocumentMetadataDto itemUpdated = new LmsDocumentMetadataDto("doc-777", "subject", "Clinical Epidemiology");
        mockLmsClient.setList(List.of(itemUpdated));
        syncService.syncLmsMetadata();

        // Assert update
        Optional<LmsMetadata> optSubjectUpdated = lmsMetadataRepository.findByExternalIdAndMetadataKey("doc-777", "subject");
        assertThat(optSubjectUpdated).isPresent();
        assertThat(optSubjectUpdated.get().getMetadataValue()).isEqualTo("Clinical Epidemiology");
    }

    @Test
    public void testSyncEiosRoleChangesUpdatesUserPermissions() {
        // Arrange Role Mappings
        SyncedRole adminRole = new SyncedRole();
        adminRole.setExternalRoleName("ext_admin");
        adminRole.setInternalEiosIdentifier("ADMINISTRATOR");
        syncedRoleRepository.save(adminRole);

        SyncedRole teacherRole = new SyncedRole();
        teacherRole.setExternalRoleName("ext_prof");
        teacherRole.setInternalEiosIdentifier("TEACHER");
        syncedRoleRepository.save(teacherRole);

        // Scenario 1: Initial Sync for User
        EiosUserRoleDto user1Sync = new EiosUserRoleDto("user-100", List.of("ext_prof"));
        mockEiosClient.setList(List.of(user1Sync));

        // Act
        syncService.syncEiosRoles();

        // Assert user permissions updated to TEACHER
        List<LocalUserPermission> permissions = localUserPermissionRepository.findByUserId("user-100");
        assertThat(permissions).hasSize(1);
        assertThat(permissions.get(0).getInternalEiosIdentifier()).isEqualTo("TEACHER");

        // Scenario 2: Role change in EIOS (e.g. promoting teacher to admin and removing ext_prof)
        EiosUserRoleDto user1SyncUpdated = new EiosUserRoleDto("user-100", List.of("ext_admin"));
        mockEiosClient.setList(List.of(user1SyncUpdated));

        // Act
        syncService.syncEiosRoles();

        // Assert permissions updated correctly
        List<LocalUserPermission> permissionsUpdated = localUserPermissionRepository.findByUserId("user-100");
        assertThat(permissionsUpdated).hasSize(1);
        assertThat(permissionsUpdated.get(0).getInternalEiosIdentifier()).isEqualTo("ADMINISTRATOR");

        // Scenario 3: Remove all roles from user
        EiosUserRoleDto user1SyncCleared = new EiosUserRoleDto("user-100", List.of());
        mockEiosClient.setList(List.of(user1SyncCleared));

        // Act
        syncService.syncEiosRoles();

        // Assert permissions are cleared
        List<LocalUserPermission> permissionsCleared = localUserPermissionRepository.findByUserId("user-100");
        assertThat(permissionsCleared).isEmpty();
    }
}
