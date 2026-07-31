package com.eneik.generated.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaskProgressionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        featureRepository.deleteAll();
    }

    @Test
    public void testTaskResolutionAndReadinessScore() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Test Feature");
        feature.setReadinessRatio(0.33); // Stalled at 1/3 initially
        featureRepository.save(feature);

        // Setup 10 tasks in total: 8 READY, 1 PENDING_REVIEW, 1 FAILED
        for (int i = 1; i <= 8; i++) {
            Task t = new Task();
            t.setTitle("Task " + i);
            t.setStatus(TaskStatus.READY);
            t.setFeatureId(feature.getId());
            taskRepository.save(t);
        }

        Task pending = new Task();
        pending.setTitle("Pending Task");
        pending.setStatus(TaskStatus.PENDING_REVIEW);
        pending.setFeatureId(feature.getId());
        taskRepository.save(pending);

        Task failed = new Task();
        failed.setTitle("Failed Task");
        failed.setStatus(TaskStatus.FAILED);
        failed.setFeatureId(feature.getId());
        taskRepository.save(failed);

        // Verify initial state
        assertThat(taskRepository.count()).isEqualTo(10);
        assertThat(taskRepository.findByStatus(TaskStatus.RESOLVED)).isEmpty();

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert API Response
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);

        assertThat(responseMap).containsKey("resolvedCount");
        assertThat(responseMap).containsKey("readiness");

        int resolvedCount = (int) responseMap.get("resolvedCount");
        assertThat(resolvedCount).isEqualTo(9); // 8 READY + 1 PENDING_REVIEW task now resolved!

        // Verify task state transitions in DB
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(9);

        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        assertThat(pendingTasks).isEmpty();

        // Verify feature readiness in DB
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).get();
        // 9 resolved out of 10 tasks = 0.9
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(0.9);
    }

    @Test
    public void testTaskResolutionPassesThreshold() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Threshold Feature");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        // Setup 10 tasks in total: 10 READY
        for (int i = 1; i <= 10; i++) {
            Task t = new Task();
            t.setTitle("Task " + i);
            t.setStatus(TaskStatus.READY);
            t.setFeatureId(feature.getId());
            taskRepository.save(t);
        }

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert API Response
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);

        int resolvedCount = (int) responseMap.get("resolvedCount");
        assertThat(resolvedCount).isEqualTo(10);

        // Type-safe float extraction
        double readiness = ((Number) responseMap.get("readiness")).doubleValue();

        // Assert the threshold from acceptance criteria
        assertThat(readiness).isGreaterThan(0.90);

        // Verify DB State
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(10);

        List<Task> readyTasks = taskRepository.findByStatus(TaskStatus.READY);
        assertThat(readyTasks).isEmpty();

        // Verify feature readiness in DB
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).get();
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(1.0);
    }
}
