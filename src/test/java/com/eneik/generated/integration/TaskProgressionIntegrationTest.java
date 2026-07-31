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
    private TaskTimeoutScheduler taskTimeoutScheduler;

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
        pending.setStatusChangedAt(java.time.LocalDateTime.now().minusHours(5));
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
        // Only the 8 READY tasks are resolved via API endpoint.
        assertThat(resolvedCount).isEqualTo(8);

        // Verify task state transitions in DB
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(8);

        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        assertThat(pendingTasks).hasSize(1); // Still PENDING_REVIEW!

        // Verify feature readiness in DB: 8 resolved out of 10 tasks = 0.8
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).get();
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(0.8);
    }

    @Test
    public void testNewPendingReviewTaskNotResolved() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Test Feature 2");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        // Setup 2 tasks: 1 READY, 1 PENDING_REVIEW (created just now)
        Task ready = new Task();
        ready.setTitle("Ready Task");
        ready.setStatus(TaskStatus.READY);
        ready.setFeatureId(feature.getId());
        taskRepository.save(ready);

        Task pending = new Task();
        pending.setTitle("New Pending Task");
        pending.setStatus(TaskStatus.PENDING_REVIEW);
        pending.setStatusChangedAt(java.time.LocalDateTime.now()); // brand new
        pending.setFeatureId(feature.getId());
        taskRepository.save(pending);

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert API Response
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);

        int resolvedCount = (int) responseMap.get("resolvedCount");
        // Only the READY task should be resolved, the brand new PENDING_REVIEW task should NOT be resolved!
        assertThat(resolvedCount).isEqualTo(1);

        // Verify task state in DB
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(1);
        assertThat(resolvedTasks.get(0).getTitle()).isEqualTo("Ready Task");

        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        assertThat(pendingTasks).hasSize(1);
        assertThat(pendingTasks.get(0).getTitle()).isEqualTo("New Pending Task");

        // Verify feature readiness in DB: 1 resolved out of 2 total = 0.5
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).get();
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(0.5);
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

    @Test
    public void testScheduledTimeoutTransitionToFailed() {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Timeout Feature");
        feature.setReadinessRatio(0.5);
        featureRepository.save(feature);

        // Task 1: Stuck in PENDING_REVIEW for 5 hours (should fail)
        Task stuck = new Task();
        stuck.setTitle("Stuck Review Task");
        stuck.setStatus(TaskStatus.PENDING_REVIEW);
        stuck.setStatusChangedAt(java.time.LocalDateTime.now().minusHours(5));
        stuck.setFeatureId(feature.getId());
        taskRepository.save(stuck);

        // Task 2: Recently moved to PENDING_REVIEW (should remain PENDING_REVIEW)
        Task recent = new Task();
        recent.setTitle("Recent Review Task");
        recent.setStatus(TaskStatus.PENDING_REVIEW);
        recent.setStatusChangedAt(java.time.LocalDateTime.now());
        recent.setFeatureId(feature.getId());
        taskRepository.save(recent);

        // Act
        taskTimeoutScheduler.checkPendingReviewTimeouts();

        // Assert
        Task updatedStuck = taskRepository.findById(stuck.getId()).orElseThrow();
        assertThat(updatedStuck.getStatus()).isEqualTo(TaskStatus.FAILED);

        Task updatedRecent = taskRepository.findById(recent.getId()).orElseThrow();
        assertThat(updatedRecent.getStatus()).isEqualTo(TaskStatus.PENDING_REVIEW);

        // Verify feature readiness was correctly recalculated: 0 resolved / 2 total = 0.0
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(0.0);
    }
}
