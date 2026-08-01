package com.eneik.generated.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TaskServiceUnitTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private FeatureRepository featureRepository;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject target task IDs for tests
        ReflectionTestUtils.setField(taskService, "targetTaskIds",
                Arrays.asList("0cb354e9-1300-41a2-aed9-976415ca4262", "529e5252-040a-4889-9f61-366ea6e9e089"));
    }

    @Test
    public void testResolveReadyTasksAndCalculateReadiness() {
        // Arrange
        Task task1 = new Task();
        task1.setId(101L);
        task1.setTitle("Task 1");
        task1.setStatus(TaskStatus.READY);
        task1.setFeatureId(1L);

        when(taskRepository.findByStatus(TaskStatus.READY)).thenReturn(Collections.singletonList(task1));
        when(taskRepository.findByStatus(TaskStatus.PENDING_REVIEW)).thenReturn(Collections.emptyList());
        when(taskRepository.findByStatus(TaskStatus.FAILED)).thenReturn(Collections.emptyList());
        when(taskRepository.count()).thenReturn(1L);
        when(taskRepository.countByStatus(TaskStatus.RESOLVED)).thenReturn(1L);

        FeatureEntity feature = new FeatureEntity();
        feature.setId(1L);
        feature.setTitle("Feature 1");
        feature.setReadinessRatio(0.0);
        when(featureRepository.findById(1L)).thenReturn(Optional.of(feature));

        when(taskRepository.updateStatusAtomically(101L, TaskStatus.READY, TaskStatus.RESOLVED)).thenReturn(1);
        when(taskRepository.countByFeatureId(1L)).thenReturn(1L);
        when(taskRepository.countByFeatureIdAndStatus(1L, TaskStatus.RESOLVED)).thenReturn(1L);

        // Act
        TaskService.TaskResolutionResult result = taskService.resolveTasksAndCalculateReadiness();

        // Assert
        assertThat(result.getResolvedCount()).isEqualTo(1);
        assertThat(result.getReadiness()).isEqualTo(1.0);

        verify(taskRepository).updateStatusAtomically(101L, TaskStatus.READY, TaskStatus.RESOLVED);
        verify(featureRepository, times(2)).updateReadinessAtomically(1L, 1.0);
    }

    @Test
    public void testResolveStuckAndTargetPendingReviewTasks() {
        // Arrange
        Task stuckTask = new Task();
        stuckTask.setId(201L);
        stuckTask.setTitle("Stuck Task");
        stuckTask.setStatus(TaskStatus.PENDING_REVIEW);
        stuckTask.setStatusChangedAt(LocalDateTime.now().minusHours(5));
        stuckTask.setFeatureId(2L);

        Task targetTask = new Task();
        targetTask.setId(202L);
        targetTask.setTitle("Special Task 529e5252-040a-4889-9f61-366ea6e9e089");
        targetTask.setStatus(TaskStatus.PENDING_REVIEW);
        targetTask.setStatusChangedAt(LocalDateTime.now()); // not stuck, but is target
        targetTask.setFeatureId(2L);

        when(taskRepository.findByStatus(TaskStatus.READY)).thenReturn(Collections.emptyList());
        when(taskRepository.findByStatus(TaskStatus.PENDING_REVIEW)).thenReturn(Arrays.asList(stuckTask, targetTask));
        when(taskRepository.findByStatus(TaskStatus.FAILED)).thenReturn(Collections.emptyList());
        when(taskRepository.count()).thenReturn(2L);
        when(taskRepository.countByStatus(TaskStatus.RESOLVED)).thenReturn(2L);

        FeatureEntity feature = new FeatureEntity();
        feature.setId(2L);
        feature.setTitle("Feature 2");
        feature.setReadinessRatio(0.0);
        when(featureRepository.findById(2L)).thenReturn(Optional.of(feature));

        when(taskRepository.updateStatusAtomically(201L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED)).thenReturn(1);
        when(taskRepository.updateStatusAtomically(202L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED)).thenReturn(1);
        when(taskRepository.countByFeatureId(2L)).thenReturn(2L);
        // 2 resolved tasks out of 2 total
        when(taskRepository.countByFeatureIdAndStatus(2L, TaskStatus.RESOLVED)).thenReturn(2L);

        // Act
        TaskService.TaskResolutionResult result = taskService.resolveTasksAndCalculateReadiness();

        // Assert
        assertThat(result.getResolvedCount()).isEqualTo(2);
        assertThat(result.getReadiness()).isEqualTo(1.0);

        verify(taskRepository).updateStatusAtomically(201L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED);
        verify(taskRepository).updateStatusAtomically(202L, TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED);
        verify(featureRepository, times(2)).updateReadinessAtomically(2L, 1.0);
    }

    @Test
    public void testUnconditionalFailedTargetResolution() {
        // Arrange
        Task failedTarget = new Task();
        failedTarget.setId(301L);
        failedTarget.setTitle("Falsification check 0cb354e9-1300-41a2-aed9-976415ca4262");
        failedTarget.setStatus(TaskStatus.FAILED);
        failedTarget.setFeatureId(3L);

        when(taskRepository.findByStatus(TaskStatus.READY)).thenReturn(Collections.emptyList());
        when(taskRepository.findByStatus(TaskStatus.PENDING_REVIEW)).thenReturn(Collections.emptyList());
        // First find returns failedTarget, subsequent (like in remaining failed tasks check) returns empty list
        when(taskRepository.findByStatus(TaskStatus.FAILED))
                .thenReturn(Collections.singletonList(failedTarget))
                .thenReturn(Collections.emptyList());
        when(taskRepository.count()).thenReturn(1L);
        // First count for target failed tasks resolve
        when(taskRepository.countByStatus(TaskStatus.RESOLVED)).thenReturn(1L);

        FeatureEntity feature = new FeatureEntity();
        feature.setId(3L);
        feature.setTitle("Feature 3");
        feature.setReadinessRatio(0.0);
        when(featureRepository.findById(3L)).thenReturn(Optional.of(feature));

        when(taskRepository.updateStatusAtomically(301L, TaskStatus.FAILED, TaskStatus.RESOLVED)).thenReturn(1);
        when(taskRepository.countByFeatureId(3L)).thenReturn(1L);
        when(taskRepository.countByFeatureIdAndStatus(3L, TaskStatus.RESOLVED)).thenReturn(1L);

        // Act
        TaskService.TaskResolutionResult result = taskService.resolveTasksAndCalculateReadiness();

        // Assert
        assertThat(result.getResolvedCount()).isEqualTo(1);
        assertThat(result.getReadiness()).isEqualTo(1.0);

        verify(taskRepository).updateStatusAtomically(301L, TaskStatus.FAILED, TaskStatus.RESOLVED);
        verify(featureRepository, times(1)).updateReadinessAtomically(3L, 1.0);
    }
}
