package com.eneik.generated.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MetricServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private MetricService metricService;

    @Autowired
    private BackendObserverScript backendObserverScript;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        featureRepository.deleteAll();
    }

    @Test
    public void testMetricServiceCalculations() {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Test Feature");
        feature.setReadinessRatio(0.5);
        featureRepository.save(feature);

        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setStatus(TaskStatus.RESOLVED);
        task1.setFeatureId(feature.getId());
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setTitle("Task 2");
        task2.setStatus(TaskStatus.FAILED);
        task2.setFeatureId(feature.getId());
        taskRepository.save(task2);

        // Act & Assert before resolve
        assertThat(metricService.calculateDeliverableDetail()).isEqualTo("in_progress");
        assertThat(metricService.calculateFeatureReadiness()).isEqualTo(0.0);

        // Transition task2 to resolved
        taskRepository.updateStatusAtomically(task2.getId(), TaskStatus.FAILED, TaskStatus.RESOLVED);
        featureRepository.updateReadinessAtomically(feature.getId(), 1.0);

        // Act & Assert after resolve
        assertThat(metricService.calculateDeliverableDetail()).isEqualTo("completed");
        assertThat(metricService.calculateFeatureReadiness()).isEqualTo(1.0);
    }

    @Test
    public void testBackendObserverScriptDelegation() {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Another Feature");
        feature.setReadinessRatio(1.0);
        featureRepository.save(feature);

        Task task = new Task();
        task.setTitle("Task");
        task.setStatus(TaskStatus.RESOLVED);
        task.setFeatureId(feature.getId());
        taskRepository.save(task);

        // Act
        double readiness = backendObserverScript.calculateFeatureReadiness();
        double ratio = backendObserverScript.getFeatureReadinessRatio();

        // Assert
        assertThat(readiness).isEqualTo(1.0);
        assertThat(ratio).isEqualTo(1.0);
    }

    @Test
    public void testMetricControllerEndpoints() throws Exception {
        // Arrange
        FeatureEntity feature = new FeatureEntity();
        feature.setTitle("Feature");
        feature.setReadinessRatio(1.0);
        featureRepository.save(feature);

        Task task = new Task();
        task.setTitle("Task");
        task.setStatus(TaskStatus.RESOLVED);
        task.setFeatureId(feature.getId());
        taskRepository.save(task);

        // Act & Assert for /api/v1/metrics/feature-readiness
        String response1 = mockMvc.perform(get("/api/v1/metrics/feature-readiness"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> map1 = objectMapper.readValue(response1, Map.class);
        assertThat(map1.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) map1.get("readiness")).doubleValue()).isEqualTo(1.0);

        // Act & Assert for /api/v1/metrics/deliverables
        String response2 = mockMvc.perform(get("/api/v1/metrics/deliverables"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> map2 = objectMapper.readValue(response2, Map.class);
        assertThat(map2.get("status")).isEqualTo("completed");
    }
}
