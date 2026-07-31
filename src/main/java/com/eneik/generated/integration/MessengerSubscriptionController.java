package com.eneik.generated.integration;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/integration/messenger-subscriptions")
public class MessengerSubscriptionController {

    private final MessengerSubscriptionRepository repository;

    public MessengerSubscriptionController(MessengerSubscriptionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MessengerSubscription> getMessengerSubscriptions(@RequestParam(required = false) String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            return repository.findByUserId(userId);
        }
        return repository.findAll();
    }

    @PostMapping
    public MessengerSubscription upsertMessengerSubscription(@RequestBody MessengerSubscriptionUpsert payload) {
        if (payload.getUserId() == null || payload.getUserId().trim().isEmpty() ||
            payload.getChannelOrChatId() == null || payload.getChannelOrChatId().trim().isEmpty() ||
            payload.getNotificationType() == null || payload.getNotificationType().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required fields for messenger subscription.");
        }

        // Use the optimized database query instead of retrieving everything
        Optional<MessengerSubscription> existing = repository.findByUserIdAndChannelOrChatIdAndNotificationType(
                payload.getUserId(), payload.getChannelOrChatId(), payload.getNotificationType());

        MessengerSubscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
        } else {
            subscription = new MessengerSubscription();
            subscription.setUserId(payload.getUserId());
            subscription.setChannelOrChatId(payload.getChannelOrChatId());
            subscription.setNotificationType(payload.getNotificationType());
        }

        if (payload.getIsActive() != null) {
            subscription.setActive(payload.getIsActive());
        } else {
            subscription.setActive(true);
        }

        return repository.save(subscription);
    }
}
