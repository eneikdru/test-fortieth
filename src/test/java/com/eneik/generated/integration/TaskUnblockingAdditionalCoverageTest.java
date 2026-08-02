package com.eneik.generated.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaskUnblockingAdditionalCoverageTest {

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

    /**
     * Given unit tests for the task status identification logic
     * When the test suite executes
     * Then it successfully identifies tasks queued for > 4 hours
     */
    @Test
    public void testTaskStatusIdentificationQueuedForMoreThanFourHours() {
        // AXIS / PHILOSOPHER MICRO-PATTERN: POL_HORVICH_01_FALSIFICATION_HARNESS / KARL_POPPER_01_FALSIFICATION_HARNESS
        // Principle of Falsification: we design targeted test scenarios to actively refute the hypothesis that tasks under 4 hours are resolved or trigger stalled state.

        TaskRepository mockRepo = mock(TaskRepository.class);
        FeatureRepository mockFeatureRepo = mock(FeatureRepository.class);

        // Fixed clock to inject for deterministic time tracking
        Instant baseInstant = Instant.parse("2026-08-02T12:00:00Z");
        Clock fixedClock = Clock.fixed(baseInstant, ZoneId.of("UTC"));

        TaskService unitTaskService = new TaskService(mockRepo, mockFeatureRepo, fixedClock);

        // 1. Task in PENDING_REVIEW state, queued for exactly 4 hours and 10 minutes (stuck)
        Task stuckPendingReviewTask = new Task();
        stuckPendingReviewTask.setId(3001L);
        stuckPendingReviewTask.setTitle("Regular pending review task");
        stuckPendingReviewTask.setStatus(TaskStatus.PENDING_REVIEW);
        stuckPendingReviewTask.setStatusChangedAt(LocalDateTime.now(fixedClock).minusHours(4).minusMinutes(10));
        stuckPendingReviewTask.setFeatureId(51L);

        // 2. Task in PENDING_REVIEW state, queued for only 3 hours (not stuck)
        Task recentPendingReviewTask = new Task();
        recentPendingReviewTask.setId(3002L);
        recentPendingReviewTask.setTitle("Another pending review task");
        recentPendingReviewTask.setStatus(TaskStatus.PENDING_REVIEW);
        recentPendingReviewTask.setStatusChangedAt(LocalDateTime.now(fixedClock).minusHours(3));
        recentPendingReviewTask.setFeatureId(51L);

        when(mockRepo.findByStatus(TaskStatus.READY)).thenReturn(Collections.emptyList());
        when(mockRepo.findByStatus(TaskStatus.PENDING_REVIEW)).thenReturn(List.of(stuckPendingReviewTask, recentPendingReviewTask));
        when(mockRepo.findByStatus(TaskStatus.FAILED)).thenReturn(Collections.emptyList());

        FeatureEntity feature = new FeatureEntity();
        feature.setId(51L);
        feature.setTitle("API Feature");
        when(mockFeatureRepo.findById(51L)).thenReturn(Optional.of(feature));

        // Mock atomic updates
        when(mockRepo.updateStatusAtomically(3001L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED)).thenReturn(1);
        when(mockRepo.countByFeatureId(51L)).thenReturn(2L);
        when(mockRepo.countByFeatureIdAndStatus(51L, TaskStatus.RESOLVED)).thenReturn(1L);
        when(mockRepo.count()).thenReturn(2L);
        when(mockRepo.countByStatus(TaskStatus.RESOLVED)).thenReturn(1L);

        // Act
        TaskService.TaskResolutionResult result = unitTaskService.resolveTasksAndCalculateReadiness();

        // Assert
        // Only the stuck pending review task (> 4 hours) should be identified and resolved.
        // The recent pending review task (<= 4 hours) should be ignored.
        assertThat(result.getResolvedCount()).isEqualTo(1);
        verify(mockRepo).updateStatusAtomically(3001L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED);
        verify(mockRepo, never()).updateStatusAtomically(3002L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED);
    }

    /**
     * Given unit tests for ready task status identification logic
     * When evaluating the system state with API Slice tasks
     * Then it successfully identifies API Slice tasks queued for > 4 hours and transitions to SYSTEM_STALLED
     */
    @Test
    public void testReadyTaskIdentificationQueuedForMoreThanFourHours() {
        TaskRepository mockRepo = mock(TaskRepository.class);
        FeatureRepository mockFeatureRepo = mock(FeatureRepository.class);

        // Fixed clock to inject for deterministic time tracking
        Instant baseInstant = Instant.parse("2026-08-02T12:00:00Z");
        Clock fixedClock = Clock.fixed(baseInstant, ZoneId.of("UTC"));

        TaskService unitTaskService = new TaskService(mockRepo, mockFeatureRepo, fixedClock);

        // 1. Ready API Slice task queued for > 4 hours (stuck)
        Task stuckApiSliceTask = new Task();
        stuckApiSliceTask.setId(4001L);
        stuckApiSliceTask.setTitle("API Slice database check");
        stuckApiSliceTask.setStatus(TaskStatus.READY);
        stuckApiSliceTask.setStatusChangedAt(LocalDateTime.now(fixedClock).minusHours(4).minusMinutes(1));
        stuckApiSliceTask.setFeatureId(52L);

        // 2. Ready API Slice task queued for <= 4 hours (not stuck)
        Task recentApiSliceTask = new Task();
        recentApiSliceTask.setId(4002L);
        recentApiSliceTask.setTitle("API Slice controller check");
        recentApiSliceTask.setStatus(TaskStatus.READY);
        recentApiSliceTask.setStatusChangedAt(LocalDateTime.now(fixedClock).minusHours(3));
        recentApiSliceTask.setFeatureId(52L);

        FeatureEntity feature = new FeatureEntity();
        feature.setId(52L);
        feature.setTitle("API Slice Feature");
        when(mockFeatureRepo.findById(52L)).thenReturn(Optional.of(feature));

        // Test with ONLY the recent task (not stuck)
        when(mockRepo.findByStatus(TaskStatus.READY)).thenReturn(List.of(recentApiSliceTask));
        when(mockRepo.countByStatus(TaskStatus.READY)).thenReturn(1L);
        assertThat(unitTaskService.getFlowCoreState()).isEqualTo("SYSTEM_STALLED"); // any READY task causes SYSTEM_STALLED, but let's test specifically the stuck state checker helper

        // Now test the hasStuckApiSliceTasks logic indirectly by checking the flow core state with NO ready tasks vs STUCK tasks
        // Since getFlowCoreState checks hasStuckApiSliceTasks first, if there are no READY tasks but we have a custom state, let's verify.
        // Let's verify via direct reflection if we want or just mocking the repo's findByStatus(READY) behavior.
        // Actually, let's check what state we get when we have a stuck API Slice task vs non-stuck one when ready tasks count is 0 (though normally they wouldn't be contradictory).
        // Let's look at getFlowCoreState:
        // public String getFlowCoreState() {
        //     if (hasStuckApiSliceTasks() || hasFailedTestCoverageTasks()) {
        //         return "SYSTEM_STALLED";
        //     }
        //     long readyCount = taskRepository.countByStatus(TaskStatus.READY);
        //     if (readyCount > 0) {
        //         return "SYSTEM_STALLED";
        //     }
        //     ...
        // }
        // If hasStuckApiSliceTasks() is true, it immediately returns "SYSTEM_STALLED".
        // Let's mock:
        when(mockRepo.findByStatus(TaskStatus.READY)).thenReturn(List.of(stuckApiSliceTask));
        when(mockRepo.countByStatus(TaskStatus.READY)).thenReturn(0L); // simulate countByStatus returning 0 but list having stuck task to verify hasStuckApiSliceTasks triggers it
        assertThat(unitTaskService.getFlowCoreState()).isEqualTo("SYSTEM_STALLED");

        // If no stuck tasks and count is 0, should be RUNNING or COMPLETED
        when(mockRepo.findByStatus(TaskStatus.READY)).thenReturn(List.of(recentApiSliceTask));
        when(mockRepo.countByStatus(TaskStatus.READY)).thenReturn(0L);
        when(mockRepo.count()).thenReturn(2L);
        when(mockRepo.countByStatus(TaskStatus.RESOLVED)).thenReturn(1L);
        assertThat(unitTaskService.getFlowCoreState()).isEqualTo("RUNNING");
    }

    /**
     * Given integration tests for the task processor
     * When processing failed tasks
     * Then the project readiness is verified to change correctly
     */
    @Test
    public void testFailedTasksProcessedAndReadinessChangesCorrectly() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Integration Test Feature");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        // Setup 10 tasks in total: 9 READY tasks, 1 FAILED task in the "Test Coverage" category
        for (int i = 1; i <= 9; i++) {
            Task readyTask = new Task();
            readyTask.setTitle("Ready Task " + i);
            readyTask.setStatus(TaskStatus.READY);
            readyTask.setFeatureId(feature.getId());
            taskRepository.save(readyTask);
        }

        Task failedTask = new Task();
        failedTask.setTitle("Failed Task - Test Coverage");
        failedTask.setStatus(TaskStatus.FAILED);
        failedTask.setFeatureId(feature.getId());
        taskRepository.save(failedTask);

        // Act - Call the task resolution API endpoint
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);

        int resolvedCount = (int) responseMap.get("resolvedCount");
        // All 10 tasks must be resolved
        assertThat(resolvedCount).isEqualTo(10);

        // Verify type-safe approximate comparison of readiness from the JSON crossing boundary (ACP-037 & Williamson Principle)
        double readiness = ((Number) responseMap.get("readiness")).doubleValue();
        assertThat(readiness).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));

        // Verify database state: all tasks are now RESOLVED
        List<Task> resolvedTasks = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolvedTasks).hasSize(10);

        List<Task> remainingFailed = taskRepository.findByStatus(TaskStatus.FAILED);
        assertThat(remainingFailed).isEmpty();

        // Verify project readiness changes correctly in DB
        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(1.0);
    }
}
