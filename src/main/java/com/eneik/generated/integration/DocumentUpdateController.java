package com.eneik.generated.integration;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/integration/documents")
public class DocumentUpdateController {

    private final MessengerSubscriptionRepository subscriptionRepository;
    private final MessengerBotService botService;

    public DocumentUpdateController(MessengerSubscriptionRepository subscriptionRepository,
                                    MessengerBotService botService) {
        this.subscriptionRepository = subscriptionRepository;
        this.botService = botService;
    }

    @PostMapping("/update")
    public String triggerDocumentUpdate(@RequestBody DocumentUpdatePayload payload) {
        if (payload.getDocumentId() == null || payload.getDocumentId().trim().isEmpty() ||
            payload.getCategory() == null || payload.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required fields for document update trigger.");
        }

        // Retrieve active subscriptions matching the category directly from the database
        List<MessengerSubscription> activeSubs = subscriptionRepository
                .findByNotificationTypeIgnoreCaseAndIsActiveTrue(payload.getCategory());

        String updateMessage = "Document updated: \"" + payload.getTitle() + "\" in category \"" + payload.getCategory() + "\"";

        for (MessengerSubscription sub : activeSubs) {
            // "Given a document is updated, When it belongs to a subscribed category, Then a message is sent to the Telegram bot."
            // "Given the Max messenger integration, When an event triggers, Then the delivery confirmation is logged."
            botService.sendTelegramMessage(sub.getChannelOrChatId(), updateMessage);
            botService.sendMaxMessage(sub.getUserId(), sub.getChannelOrChatId(), updateMessage);
        }

        return "Notification broadcast completed for " + activeSubs.size() + " subscribers.";
    }
}
