package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final LmsMetadataRepository lmsMetadataRepository;
    private final SyncedRoleRepository syncedRoleRepository;
    private final LocalUserPermissionRepository localUserPermissionRepository;
    private final LmsClient lmsClient;
    private final EiosClient eiosClient;

    public SyncService(LmsMetadataRepository lmsMetadataRepository,
                       SyncedRoleRepository syncedRoleRepository,
                       LocalUserPermissionRepository localUserPermissionRepository,
                       LmsClient lmsClient,
                       EiosClient eiosClient) {
        this.lmsMetadataRepository = lmsMetadataRepository;
        this.syncedRoleRepository = syncedRoleRepository;
        this.localUserPermissionRepository = localUserPermissionRepository;
        this.lmsClient = lmsClient;
        this.eiosClient = eiosClient;
    }

    /**
     * Given an updated document in LMS, When the sync job runs, Then the new metadata is indexed in the local database.
     */
    @Transactional
    public void syncLmsMetadata() {
        log.info("Starting LMS metadata synchronization job...");
        try {
            List<LmsDocumentMetadataDto> items = lmsClient.fetchUpdatedDocumentMetadata();
            if (items == null) {
                log.warn("LMS client returned null metadata items list.");
                return;
            }
            log.info("Fetched {} LMS metadata items to sync.", items.size());

            for (LmsDocumentMetadataDto item : items) {
                if (item.getExternalId() == null || item.getMetadataKey() == null) {
                    log.warn("Skipping invalid LMS metadata item with missing externalId or key.");
                    continue;
                }

                // Retrieve existing, update, or create a new one
                LmsMetadata metadata = lmsMetadataRepository.findByExternalIdAndMetadataKey(
                        item.getExternalId(), item.getMetadataKey()
                ).orElseGet(() -> {
                    LmsMetadata m = new LmsMetadata();
                    m.setExternalId(item.getExternalId());
                    m.setMetadataKey(item.getMetadataKey());
                    return m;
                });

                metadata.setMetadataValue(item.getMetadataValue());
                lmsMetadataRepository.save(metadata);
            }
            log.info("LMS metadata synchronization job completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during LMS metadata synchronization: ", e);
            throw e;
        }
    }

    /**
     * Given a role change in EIOS, When synced, Then the local user permissions are updated accordingly.
     */
    @Transactional
    public void syncEiosRoles() {
        log.info("Starting EIOS user roles synchronization job...");
        try {
            List<EiosUserRoleDto> userRoles = eiosClient.fetchUserRoleChanges();
            if (userRoles == null) {
                log.warn("EIOS client returned null user roles list.");
                return;
            }
            log.info("Fetched {} EIOS user roles changes to sync.", userRoles.size());

            for (EiosUserRoleDto userRole : userRoles) {
                String userId = userRole.getUserId();
                if (userId == null) {
                    log.warn("Skipping EIOS user role item with missing userId.");
                    continue;
                }

                // Gather target mapped permissions based on synced roles setup
                List<String> externalRoles = userRole.getExternalRoles();
                Set<String> targetInternalIdentifiers;
                if (externalRoles == null) {
                    targetInternalIdentifiers = Set.of();
                } else {
                    targetInternalIdentifiers = externalRoles.stream()
                            .map(syncedRoleRepository::findByExternalRoleName)
                            .filter(java.util.Optional::isPresent)
                            .map(opt -> opt.get().getInternalEiosIdentifier())
                            .collect(Collectors.toSet());
                }

                // Get current local user permissions
                List<LocalUserPermission> currentPermissions = localUserPermissionRepository.findByUserId(userId);

                // Permissions to delete (currently present locally but not in the mapped target)
                List<LocalUserPermission> toDelete = currentPermissions.stream()
                        .filter(p -> !targetInternalIdentifiers.contains(p.getInternalEiosIdentifier()))
                        .toList();

                // Permissions to add (in target but not currently present locally)
                Set<String> existingIdentifiers = currentPermissions.stream()
                        .map(LocalUserPermission::getInternalEiosIdentifier)
                        .collect(Collectors.toSet());

                List<LocalUserPermission> toAdd = targetInternalIdentifiers.stream()
                        .filter(id -> !existingIdentifiers.contains(id))
                        .map(id -> {
                            LocalUserPermission p = new LocalUserPermission();
                            p.setUserId(userId);
                            p.setInternalEiosIdentifier(id);
                            return p;
                        })
                        .toList();

                if (!toDelete.isEmpty()) {
                    log.info("Removing {} outdated permissions for user ID: {}", toDelete.size(), userId);
                    localUserPermissionRepository.deleteAll(toDelete);
                }

                if (!toAdd.isEmpty()) {
                    log.info("Adding {} new permissions for user ID: {}", toAdd.size(), userId);
                    localUserPermissionRepository.saveAll(toAdd);
                }
            }
            log.info("EIOS user roles synchronization job completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during EIOS user roles synchronization: ", e);
            throw e;
        }
    }
}
