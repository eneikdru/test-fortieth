package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessengerSubscriptionRepository extends JpaRepository<MessengerSubscription, Long> {
}
