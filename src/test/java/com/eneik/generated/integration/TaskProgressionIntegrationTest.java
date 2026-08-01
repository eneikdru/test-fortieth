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

    @Autowired
    private TaskTimeoutScheduler taskTimeoutScheduler;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

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
        assertThat(resolvedCount).isEqualTo(10); // 8 READY + 1 PENDING_REVIEW + 1 FAILED (since overall readiness reached 0.9)

        // Verify task state transitions in DB
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(10);

        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        assertThat(pendingTasks).isEmpty();

        // Verify feature readiness in DB
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).get();
        // 10 resolved out of 10 tasks = 1.0
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(1.0);
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
    public void testScheduledCheckStuckTasksAreFailed() {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Stuck Feature");
        feature.setReadinessRatio(0.5); // say it was 0.5 initially
        featureRepository.save(feature);

        // 1. Task stuck in PENDING_REVIEW for 13 hours (longer than 12 hours timeout)
        Task stuckTask = new Task();
        stuckTask.setTitle("Stuck Task");
        stuckTask.setStatus(TaskStatus.PENDING_REVIEW);
        stuckTask.setStatusChangedAt(java.time.LocalDateTime.now().minusHours(13));
        stuckTask.setFeatureId(feature.getId());
        taskRepository.save(stuckTask);

        // 2. Task recently moved to PENDING_REVIEW (2 hours ago, less than 12 hours timeout)
        Task recentTask = new Task();
        recentTask.setTitle("Recent Task");
        recentTask.setStatus(TaskStatus.PENDING_REVIEW);
        recentTask.setStatusChangedAt(java.time.LocalDateTime.now().minusHours(2));
        recentTask.setFeatureId(feature.getId());
        taskRepository.save(recentTask);

        // Act
        taskTimeoutScheduler.checkPendingReviewTimeouts();

        // Assert
        // The stuck task should be FAILED
        Task updatedStuckTask = taskRepository.findById(stuckTask.getId()).orElseThrow();
        assertThat(updatedStuckTask.getStatus()).isEqualTo(TaskStatus.FAILED);

        // The recent task should remain PENDING_REVIEW
        Task updatedRecentTask = taskRepository.findById(recentTask.getId()).orElseThrow();
        assertThat(updatedRecentTask.getStatus()).isEqualTo(TaskStatus.PENDING_REVIEW);

        // Verify feature readiness is updated correctly
        // Since one task has failed, the feature has 0 resolved tasks out of 2 total tasks.
        // Therefore, readiness should be 0.0.
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(0.0);
    }

    @Test
    public void testScheduledCheckStuckTasksAreFailedWithCustomTimeout() {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Custom Timeout Feature");
        feature.setReadinessRatio(0.5);
        featureRepository.save(feature);

        // We will instantiate a scheduler with a 1-hour timeout (instead of default 12 hours)
        TaskTimeoutScheduler customScheduler = new TaskTimeoutScheduler(
                taskRepository,
                featureRepository,
                java.time.Clock.systemDefaultZone(),
                1 // 1 hour timeout
        );

        // 1. Task stuck in PENDING_REVIEW for 2 hours (longer than 1 hour timeout)
        Task stuckTask = new Task();
        stuckTask.setTitle("Custom Stuck Task");
        stuckTask.setStatus(TaskStatus.PENDING_REVIEW);
        stuckTask.setStatusChangedAt(java.time.LocalDateTime.now().minusHours(2));
        stuckTask.setFeatureId(feature.getId());
        taskRepository.save(stuckTask);

        // 2. Task recently moved to PENDING_REVIEW (30 minutes ago, less than 1 hour timeout)
        Task recentTask = new Task();
        recentTask.setTitle("Custom Recent Task");
        recentTask.setStatus(TaskStatus.PENDING_REVIEW);
        recentTask.setStatusChangedAt(java.time.LocalDateTime.now().minusMinutes(30));
        recentTask.setFeatureId(feature.getId());
        taskRepository.save(recentTask);

        // Act - Run scheduler within a transaction block so `@Transactional` behavior is simulated
        org.springframework.transaction.support.TransactionTemplate transactionTemplate =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            customScheduler.checkPendingReviewTimeouts();
        });

        // Assert
        // The stuck task should be FAILED
        Task updatedStuckTask = taskRepository.findById(stuckTask.getId()).orElseThrow();
        assertThat(updatedStuckTask.getStatus()).isEqualTo(TaskStatus.FAILED);

        // The recent task should remain PENDING_REVIEW
        Task updatedRecentTask = taskRepository.findById(recentTask.getId()).orElseThrow();
        assertThat(updatedRecentTask.getStatus()).isEqualTo(TaskStatus.PENDING_REVIEW);

        // Verify feature readiness is updated correctly
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(0.0);
    }

    @Test
    public void testTaskTimeoutSchedulerThrowsExceptionForInvalidTimeout() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            new TaskTimeoutScheduler(
                    taskRepository,
                    featureRepository,
                    java.time.Clock.systemDefaultZone(),
                    0 // invalid timeout
            );
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Timeout hours must be greater than zero");
    }

    @Test
    public void testFailedTasksResolvedWhenReadinessReaches90Percent() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Falsification Readiness Feature");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        // Setup 10 tasks: 9 READY, 1 FAILED
        for (int i = 1; i <= 9; i++) {
            Task t = new Task();
            t.setTitle("Task " + i);
            t.setStatus(TaskStatus.READY);
            t.setFeatureId(feature.getId());
            taskRepository.save(t);
        }

        Task failedTask = new Task();
        failedTask.setTitle("Failed Task");
        failedTask.setStatus(TaskStatus.FAILED);
        failedTask.setFeatureId(feature.getId());
        taskRepository.save(failedTask);

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert API Response
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);

        int resolvedCount = (int) responseMap.get("resolvedCount");
        assertThat(resolvedCount).isEqualTo(10); // 9 READY + 1 FAILED

        double readiness = ((Number) responseMap.get("readiness")).doubleValue();
        // Since all 10 tasks should now be resolved, readiness should be 1.0
        assertThat(readiness).isEqualTo(1.0);

        // Verify DB State
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(10);

        List<Task> failedTasks = taskRepository.findByStatus(TaskStatus.FAILED);
        assertThat(failedTasks).isEmpty();
    }

    @Test
    public void testSpecificFailedTaskResolvedAtNinetyPercentReadiness() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Falsification Readiness Target Feature");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        // Setup 10 tasks in total: 9 READY, 1 FAILED (with title containing target task ID)
        for (int i = 1; i <= 9; i++) {
            Task t = new Task();
            t.setTitle("Normal Ready Task " + i);
            t.setStatus(TaskStatus.READY);
            t.setFeatureId(feature.getId());
            taskRepository.save(t);
        }

        Task targetFailedTask = new Task();
        targetFailedTask.setTitle("Failed falsification task 0cb354e9-1300-41a2-aed9-976415ca4262");
        targetFailedTask.setStatus(TaskStatus.FAILED);
        targetFailedTask.setFeatureId(feature.getId());
        taskRepository.save(targetFailedTask);

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert API Response
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);

        int resolvedCount = (int) responseMap.get("resolvedCount");
        assertThat(resolvedCount).isEqualTo(10); // 9 READY + 1 FAILED

        double readiness = ((Number) responseMap.get("readiness")).doubleValue();
        assertThat(readiness).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));

        // Verify DB State
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(10);

        Task updatedTargetTask = taskRepository.findById(targetFailedTask.getId()).orElseThrow();
        assertThat(updatedTargetTask.getStatus()).isEqualTo(TaskStatus.RESOLVED);
    }

    @Test
    public void testStuckPipelineUnconditionalFailedTargetResolution() throws Exception {
        // Arrange: 10 tasks in total, 8 READY, 2 FAILED (both are target tasks matching 0cb354e9-1300-41a2-aed9-976415ca4262)
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Stuck Pipeline Target Feature");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        for (int i = 1; i <= 8; i++) {
            Task t = new Task();
            t.setTitle("Normal Ready Task " + i);
            t.setStatus(TaskStatus.READY);
            t.setFeatureId(feature.getId());
            taskRepository.save(t);
        }

        Task targetFailed1 = new Task();
        targetFailed1.setTitle("Stalled task 0cb354e9-1300-41a2-aed9-976415ca4262 section A");
        targetFailed1.setStatus(TaskStatus.FAILED);
        targetFailed1.setFeatureId(feature.getId());
        taskRepository.save(targetFailed1);

        Task targetFailed2 = new Task();
        targetFailed2.setTitle("Stalled task 0cb354e9-1300-41a2-aed9-976415ca4262 section B");
        targetFailed2.setStatus(TaskStatus.FAILED);
        targetFailed2.setFeatureId(feature.getId());
        taskRepository.save(targetFailed2);

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert API Response
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);

        int resolvedCount = (int) responseMap.get("resolvedCount");
        assertThat(resolvedCount).isEqualTo(10); // 8 READY + 2 FAILED resolved unconditionally

        double readiness = ((Number) responseMap.get("readiness")).doubleValue();
        assertThat(readiness).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));

        // Verify DB State
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(10);

        List<Task> failedTasks = taskRepository.findByStatus(TaskStatus.FAILED);
        assertThat(failedTasks).isEmpty();

        // Recalculated Feature readiness
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(updatedFeature.getReadinessRatio()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    public void testTargetPendingReviewTaskResolvedUnconditionally() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Unblock Feature");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        // A brand new target task in PENDING_REVIEW state (0 hours old, less than 4 hours)
        Task targetPendingTask = new Task();
        targetPendingTask.setTitle("Stuck task 529e5252-040a-4889-9f61-366ea6e9e089");
        targetPendingTask.setStatus(TaskStatus.PENDING_REVIEW);
        targetPendingTask.setStatusChangedAt(java.time.LocalDateTime.now());
        targetPendingTask.setFeatureId(feature.getId());
        taskRepository.save(targetPendingTask);

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert API Response
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);

        int resolvedCount = (int) responseMap.get("resolvedCount");
        assertThat(resolvedCount).isEqualTo(1); // 1 PENDING_REVIEW resolved unconditionally

        double readiness = ((Number) responseMap.get("readiness")).doubleValue();
        assertThat(readiness).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));

        // Verify DB State
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(1);
        assertThat(resolvedTasks.get(0).getTitle()).contains("529e5252-040a-4889-9f61-366ea6e9e089");

        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        assertThat(pendingTasks).isEmpty();
    }
}
