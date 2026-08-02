package com.eneik.generated.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import java.util.ArrayList;
import java.util.Arrays;
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
public class TaskUnblockingCoverageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskService integrationTaskService;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        featureRepository.deleteAll();
    }

    // --- UNIT TESTS ---

    @Test
    public void testTaskStatusIdentificationQueuedForMoreThanFourHours() {
        // Arrange
        TaskRepository mockRepo = mock(TaskRepository.class);
        FeatureRepository mockFeatureRepo = mock(FeatureRepository.class);

        // Fixed clock for deterministic testing
        Instant baseInstant = Instant.parse("2026-08-02T12:00:00Z");
        Clock fixedClock = Clock.fixed(baseInstant, ZoneId.of("UTC"));

        TaskService unitTaskService = new TaskService(mockRepo, mockFeatureRepo, fixedClock);

        // Non-target pending review task, queued for 4 hours and 1 minute (stuck)
        Task stuckTask = new Task();
        stuckTask.setId(2001L);
        stuckTask.setTitle("Regular Non-Target Task");
        stuckTask.setStatus(TaskStatus.PENDING_REVIEW);
        stuckTask.setStatusChangedAt(LocalDateTime.now(fixedClock).minusHours(4).minusMinutes(1));
        stuckTask.setFeatureId(50L);

        when(mockRepo.findByStatus(TaskStatus.READY)).thenReturn(Collections.emptyList());
        when(mockRepo.findByStatus(TaskStatus.PENDING_REVIEW)).thenReturn(Collections.singletonList(stuckTask));
        when(mockRepo.findByStatus(TaskStatus.FAILED)).thenReturn(Collections.emptyList());

        FeatureEntity feature = new FeatureEntity();
        feature.setId(50L);
        feature.setTitle("Regular Feature");
        when(mockFeatureRepo.findById(50L)).thenReturn(Optional.of(feature));

        when(mockRepo.updateStatusAtomically(2001L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED)).thenReturn(1);
        when(mockRepo.countByFeatureId(50L)).thenReturn(1L);
        when(mockRepo.countByFeatureIdAndStatus(50L, TaskStatus.RESOLVED)).thenReturn(1L);
        when(mockRepo.count()).thenReturn(1L);
        when(mockRepo.countByStatus(TaskStatus.RESOLVED)).thenReturn(1L);

        // Act
        TaskService.TaskResolutionResult result = unitTaskService.resolveTasksAndCalculateReadiness();

        // Assert
        // The stuck task (queued > 4 hours) must be successfully identified and resolved
        assertThat(result.getResolvedCount()).isEqualTo(1);
        verify(mockRepo).updateStatusAtomically(2001L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED);
    }

    @Test
    public void testTaskStatusIdentificationQueuedForLessThanFourHours() {
        // Arrange
        TaskRepository mockRepo = mock(TaskRepository.class);
        FeatureRepository mockFeatureRepo = mock(FeatureRepository.class);

        // Fixed clock for deterministic testing
        Instant baseInstant = Instant.parse("2026-08-02T12:00:00Z");
        Clock fixedClock = Clock.fixed(baseInstant, ZoneId.of("UTC"));

        TaskService unitTaskService = new TaskService(mockRepo, mockFeatureRepo, fixedClock);

        // Non-target pending review task, queued for 3 hours and 59 minutes (not stuck)
        Task recentTask = new Task();
        recentTask.setId(2002L);
        recentTask.setTitle("Regular Non-Target Task");
        recentTask.setStatus(TaskStatus.PENDING_REVIEW);
        recentTask.setStatusChangedAt(LocalDateTime.now(fixedClock).minusHours(3).minusMinutes(59));
        recentTask.setFeatureId(50L);

        when(mockRepo.findByStatus(TaskStatus.READY)).thenReturn(Collections.emptyList());
        when(mockRepo.findByStatus(TaskStatus.PENDING_REVIEW)).thenReturn(Collections.singletonList(recentTask));
        when(mockRepo.findByStatus(TaskStatus.FAILED)).thenReturn(Collections.emptyList());

        FeatureEntity feature = new FeatureEntity();
        feature.setId(50L);
        feature.setTitle("Regular Feature");
        when(mockFeatureRepo.findById(50L)).thenReturn(Optional.of(feature));

        // Act
        TaskService.TaskResolutionResult result = unitTaskService.resolveTasksAndCalculateReadiness();

        // Assert
        // The recent task (queued < 4 hours) must NOT be identified as stuck and should NOT be resolved
        assertThat(result.getResolvedCount()).isEqualTo(0);
        verify(mockRepo, never()).updateStatusAtomically(2002L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED);
    }

    // --- INTEGRATION TESTS ---

    @Test
    public void testFailedTasksProcessedAndReadinessChangesCorrectly() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Integration Test Feature");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        // Setup 10 tasks in total: 9 READY tasks, 1 FAILED task
        for (int i = 1; i <= 9; i++) {
            Task readyTask = new Task();
            readyTask.setTitle("Ready Task " + i);
            readyTask.setStatus(TaskStatus.READY);
            readyTask.setFeatureId(feature.getId());
            taskRepository.save(readyTask);
        }

        Task failedTask = new Task();
        failedTask.setTitle("Failed Task - Test Coverage Category");
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
        // All 10 tasks must be resolved (9 READY, and the 1 FAILED task which unblocks because readiness reaches 90% and FAILED task transitions)
        assertThat(resolvedCount).isEqualTo(10);

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
