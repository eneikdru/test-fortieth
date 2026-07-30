package com.eneik.generated.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class IntegrationSchemaTest {

    @Autowired
    private LmsMetadataRepository lmsMetadataRepository;

    @Autowired
    private MessengerSubscriptionRepository messengerSubscriptionRepository;

    @Autowired
    private SyncedRoleRepository syncedRoleRepository;

    @Test
    public void testLmsMetadataPersistence() {
        LmsMetadata metadata = new LmsMetadata();
        metadata.setExternalId("course-101");
        metadata.setMetadataKey("title");
        metadata.setMetadataValue("Эпидемиология и инфекционные болезни");

        LmsMetadata saved = lmsMetadataRepository.save(metadata);
        assertThat(saved.getId()).isNotNull();

        Optional<LmsMetadata> found = lmsMetadataRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getExternalId()).isEqualTo("course-101");
        assertThat(found.get().getMetadataKey()).isEqualTo("title");
        assertThat(found.get().getMetadataValue()).isEqualTo("Эпидемиология и инфекционные болезни");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    public void testMessengerSubscriptionPersistence() {
        MessengerSubscription subscription = new MessengerSubscription();
        subscription.setUserId("user-456");
        subscription.setChannelOrChatId("chat-789");
        subscription.setNotificationType("TELEGRAM_ALERT");
        subscription.setActive(true);

        MessengerSubscription saved = messengerSubscriptionRepository.save(subscription);
        assertThat(saved.getId()).isNotNull();

        Optional<MessengerSubscription> found = messengerSubscriptionRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user-456");
        assertThat(found.get().getChannelOrChatId()).isEqualTo("chat-789");
        assertThat(found.get().getNotificationType()).isEqualTo("TELEGRAM_ALERT");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    public void testSyncedRoleMappingToInternalEiosIdentifier() {
        // Given a synced role record
        SyncedRole syncedRole = new SyncedRole();
        syncedRole.setExternalRoleName("ext_postgrad_lead");
        syncedRole.setInternalEiosIdentifier("TEACHER");
        syncedRole.setDescription("Postgraduate supervisor mapped to internal TEACHER role");

        // When saved
        SyncedRole saved = syncedRoleRepository.save(syncedRole);
        assertThat(saved.getId()).isNotNull();

        // Then it correctly maps to internal EIOS identifiers
        Optional<SyncedRole> found = syncedRoleRepository.findByExternalRoleName("ext_postgrad_lead");
        assertThat(found).isPresent();
        assertThat(found.get().getInternalEiosIdentifier()).isEqualTo("TEACHER");
        assertThat(found.get().getDescription()).isEqualTo("Postgraduate supervisor mapped to internal TEACHER role");
    }
}
