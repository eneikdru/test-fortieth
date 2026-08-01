package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MetricService {

    private static final Logger log = LoggerFactory.getLogger(MetricService.class);

    private final TaskRepository taskRepository;
    private final FeatureRepository featureRepository;

    public MetricService(TaskRepository taskRepository, FeatureRepository featureRepository) {
        this.taskRepository = taskRepository;
        this.featureRepository = featureRepository;
    }

    public String calculateDeliverableDetail() {
        long total = taskRepository.count();
        if (total == 0) {
            return "completed";
        }
        long resolved = taskRepository.countByStatus(TaskStatus.RESOLVED);
        return (resolved == total) ? "completed" : "in_progress";
    }

    public String getDeliverableDetail() {
        return calculateDeliverableDetail();
    }

    public double calculateFeatureReadiness() {
        long total = featureRepository.count();
        if (total == 0) {
            return 1.0;
        }
        long ready = featureRepository.findAll().stream()
                .filter(f -> f.getReadinessRatio() >= 1.0 - 1e-9)
                .count();
        return (double) ready / total;
    }

    public double getFeatureReadinessRatio() {
        return calculateFeatureReadiness();
    }
}
