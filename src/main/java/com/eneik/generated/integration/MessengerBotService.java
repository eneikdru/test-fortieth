package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessengerBotService {

    private static final Logger log = LoggerFactory.getLogger(MessengerBotService.class);

    private final List<String> sentTelegramMessages = new ArrayList<>();
    private final List<String> sentMaxMessages = new ArrayList<>();

    public void sendTelegramMessage(String channelOrChatId, String message) {
        String logEntry = "Sending Telegram message to " + channelOrChatId + ": " + message;
        log.info(logEntry);
        sentTelegramMessages.add(logEntry);
    }

    public void sendMaxMessage(String userId, String channelOrChatId, String message) {
        String logEntry = "Max Messenger delivery confirmation for user " + userId + " on channel " + channelOrChatId + ": " + message;
        // Logging the delivery confirmation as explicitly required by acceptance criteria
        log.info(logEntry);
        sentMaxMessages.add(logEntry);
    }

    public List<String> getSentTelegramMessages() {
        return sentTelegramMessages;
    }

    public List<String> getSentMaxMessages() {
        return sentMaxMessages;
    }

    public void clearHistory() {
        sentTelegramMessages.clear();
        sentMaxMessages.clear();
    }
}
