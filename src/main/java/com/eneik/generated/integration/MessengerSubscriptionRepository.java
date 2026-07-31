package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessengerSubscriptionRepository extends JpaRepository<MessengerSubscription, Long> {

    List<MessengerSubscription> findByUserId(String userId);

    @Query("SELECT m FROM MessengerSubscription m WHERE m.userId = :userId AND m.channelOrChatId = :channelOrChatId AND m.notificationType = :notificationType")
    Optional<MessengerSubscription> findByUserIdAndChannelOrChatIdAndNotificationType(
            @Param("userId") String userId,
            @Param("channelOrChatId") String channelOrChatId,
            @Param("notificationType") String notificationType);

    @Query("SELECT m FROM MessengerSubscription m WHERE LOWER(m.notificationType) = LOWER(:notificationType) AND m.isActive = true")
    List<MessengerSubscription> findByNotificationTypeIgnoreCaseAndIsActiveTrue(@Param("notificationType") String notificationType);
}
