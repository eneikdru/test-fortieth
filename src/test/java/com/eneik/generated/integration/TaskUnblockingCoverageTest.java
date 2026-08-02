package com.eneik.generated.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private TaskService taskService;

    @Mock
    private TaskRepository mockTaskRepository;

    @Mock
    private FeatureRepository mockFeatureRepository;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        featureRepository.deleteAll();
    }

    @Test
    public void testUnitHasStuckPipelineTasksForPrReviewFallback() {
        // Arrange
        MockitoAnnotations.openMocks(this);
        Instant fixedInstant = Instant.parse("2026-08-02T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        TaskService serviceWithClock = new TaskService(mockTaskRepository, mockFeatureRepository, fixedClock);

        Task stuckTask = new Task();
        stuckTask.setId(701L);
        stuckTask.setTitle("PR review fallback task with uuid 85feb4bc-7a85-45bf-a637-476d48d00d6a");
        stuckTask.setStatus(TaskStatus.READY);
        // Stuck for 5 hours (cutoff is 4 hours)
        stuckTask.setStatusChangedAt(LocalDateTime.ofInstant(fixedInstant.minus(java.time.Duration.ofHours(5)), ZoneId.of("UTC")));

        when(mockTaskRepository.findByStatus(TaskStatus.READY)).thenReturn(Collections.singletonList(stuckTask));

        // Act
        String state = serviceWithClock.getFlowCoreState();

        // Assert
        assertThat(state).isEqualTo("SYSTEM_STALLED");
    }

    @Test
    public void testUnitHasStuckPipelineTasksForWishlistCompilation() {
        // Arrange
        MockitoAnnotations.openMocks(this);
        Instant fixedInstant = Instant.parse("2026-08-02T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        TaskService serviceWithClock = new TaskService(mockTaskRepository, mockFeatureRepository, fixedClock);

        Task stuckTask = new Task();
        stuckTask.setId(702L);
        stuckTask.setTitle("wishlist compilation helper");
        stuckTask.setStatus(TaskStatus.READY);
        stuckTask.setStatusChangedAt(LocalDateTime.ofInstant(fixedInstant.minus(java.time.Duration.ofHours(5)), ZoneId.of("UTC")));

        when(mockTaskRepository.findByStatus(TaskStatus.READY)).thenReturn(Collections.singletonList(stuckTask));

        // Act
        String state = serviceWithClock.getFlowCoreState();

        // Assert
        assertThat(state).isEqualTo("SYSTEM_STALLED");
    }

    @Test
    public void testIntegrationUnblockingStuckTasksSuccessfully() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Integrations Unblocking Feature");
        feature.setReadinessRatio(0.0);
        featureRepository.save(feature);

        Task prFallbackTask = new Task();
        prFallbackTask.setTitle("My PR review fallback");
        prFallbackTask.setStatus(TaskStatus.READY);
        prFallbackTask.setStatusChangedAt(LocalDateTime.now().minusHours(5)); // over 4 hours
        prFallbackTask.setFeatureId(feature.getId());
        taskRepository.save(prFallbackTask);

        Task wishlistCompilationTask = new Task();
        wishlistCompilationTask.setTitle("wishlist compilation stuck task 68c31f3d-be90-4949-b5df-b741cd52c4ef");
        wishlistCompilationTask.setStatus(TaskStatus.READY);
        wishlistCompilationTask.setStatusChangedAt(LocalDateTime.now().minusHours(5)); // over 4 hours
        wishlistCompilationTask.setFeatureId(feature.getId());
        taskRepository.save(wishlistCompilationTask);

        // Verify that getFlowCoreState endpoint returns SYSTEM_STALLED initially
        mockMvc.perform(get("/api/v1/tasks/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SYSTEM_STALLED"));

        // Act - Invoke resolve endpoint to unblock stuck queue/pipeline tasks
        mockMvc.perform(post("/api/v1/tasks/resolve"))
                .andExpect(status().isOk());

        // Assert - Both tasks should be RESOLVED, and feature readiness updated to 1.0
        List<Task> resolved = taskRepository.findByStatus(TaskStatus.RESOLVED);
        assertThat(resolved).hasSize(2);

        FeatureEntity updatedFeature = featureRepository.findById(feature.getId()).orElseThrow();
        assertThat(updatedFeature.getReadinessRatio()).isEqualTo(1.0);

        // State is now COMPLETED since no ready tasks remain
        mockMvc.perform(get("/api/v1/tasks/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"));
    }
}
