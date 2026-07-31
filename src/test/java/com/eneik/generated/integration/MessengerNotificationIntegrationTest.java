package com.eneik.generated.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class MessengerNotificationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MessengerSubscriptionRepository subscriptionRepository;

    @Autowired
    private MessengerBotService botService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        botService.clearHistory();
    }

    @Test
    public void testMessengerSubscriptionCrud() throws Exception {
        // Post a new subscription
        MessengerSubscriptionUpsert payload = new MessengerSubscriptionUpsert();
        payload.setUserId("user-123");
        payload.setChannelOrChatId("chat-555");
        payload.setNotificationType("Epidemiology");
        payload.setIsActive(true);

        mockMvc.perform(post("/api/v1/integration/messenger-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.channelOrChatId").value("chat-555"))
                .andExpect(jsonPath("$.notificationType").value("Epidemiology"))
                .andExpect(jsonPath("$.active").value(true));

        // Get subscriptions filtering by userId
        mockMvc.perform(get("/api/v1/integration/messenger-subscriptions")
                .param("userId", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channelOrChatId").value("chat-555"))
                .andExpect(jsonPath("$[0].notificationType").value("Epidemiology"));
    }

    @Test
    public void testDocumentUpdateTriggersTelegramAndMaxBots() throws Exception {
        // Create an active subscription for category "Pediatrics"
        MessengerSubscription sub1 = new MessengerSubscription();
        sub1.setUserId("resident-888");
        sub1.setChannelOrChatId("telegram-chat-99");
        sub1.setNotificationType("Pediatrics");
        sub1.setActive(true);
        subscriptionRepository.save(sub1);

        // Create an inactive subscription for category "Pediatrics" (should not receive notification)
        MessengerSubscription sub2 = new MessengerSubscription();
        sub2.setUserId("resident-777");
        sub2.setChannelOrChatId("telegram-chat-100");
        sub2.setNotificationType("Pediatrics");
        sub2.setActive(false);
        subscriptionRepository.save(sub2);

        // Trigger document update for Pediatrics
        DocumentUpdatePayload documentUpdate = new DocumentUpdatePayload();
        documentUpdate.setDocumentId("doc-abc-123");
        documentUpdate.setTitle("Methodological guidelines for clinical residency in Pediatrics");
        documentUpdate.setCategory("Pediatrics");

        mockMvc.perform(post("/api/v1/integration/documents/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(documentUpdate)))
                .andExpect(status().isOk());

        // Verify Telegram message delivery
        List<String> telegramMessages = botService.getSentTelegramMessages();
        assertThat(telegramMessages).hasSize(1);
        assertThat(telegramMessages.getFirst()).contains("telegram-chat-99");
        assertThat(telegramMessages.getFirst()).contains("Methodological guidelines for clinical residency in Pediatrics");

        // Verify Max message delivery and logging
        List<String> maxMessages = botService.getSentMaxMessages();
        assertThat(maxMessages).hasSize(1);
        assertThat(maxMessages.getFirst()).contains("resident-888");
        assertThat(maxMessages.getFirst()).contains("telegram-chat-99");
        assertThat(maxMessages.getFirst()).contains("Methodological guidelines for clinical residency in Pediatrics");
    }
}
