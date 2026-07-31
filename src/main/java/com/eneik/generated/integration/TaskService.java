package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final FeatureRepository featureRepository;

    public TaskService(TaskRepository taskRepository, FeatureRepository featureRepository) {
        this.taskRepository = taskRepository;
        this.featureRepository = featureRepository;
    }

    @Transactional
    public TaskResolutionResult resolveTasksAndCalculateReadiness() {
        log.info("Starting task resolution process...");
        List<Task> readyTasks = taskRepository.findByStatus(TaskStatus.READY);

        int resolvedCount = 0;
        Set<Long> affectedFeatureIds = new HashSet<>();

        for (Task task : readyTasks) {
            int updated = taskRepository.updateStatusAtomically(task.getId(), TaskStatus.READY, TaskStatus.RESOLVED);
            if (updated > 0) {
                resolvedCount++;
                if (task.getFeatureId() != null) {
                    affectedFeatureIds.add(task.getFeatureId());
                }
            }
        }
        log.info("Resolved {} out of {} ready tasks.", resolvedCount, readyTasks.size());

        List<Task> pendingReviewTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        int pendingResolvedCount = 0;
        for (Task task : pendingReviewTasks) {
            int updated = taskRepository.updateStatusAtomically(task.getId(), TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED);
            if (updated > 0) {
                pendingResolvedCount++;
                if (task.getFeatureId() != null) {
                    affectedFeatureIds.add(task.getFeatureId());
                }
            }
        }
        log.info("Resolved {} out of {} pending review tasks.", pendingResolvedCount, pendingReviewTasks.size());
        resolvedCount += pendingResolvedCount;

        for (Long featureId : affectedFeatureIds) {
            updateFeatureReadiness(featureId);
        }

        double overallReadiness = calculateFalsificationReadiness();
        log.info("Calculated falsification readiness: {}", overallReadiness);

        return new TaskResolutionResult(resolvedCount, overallReadiness);
    }

    private void updateFeatureReadiness(Long featureId) {
        FeatureEntity feature = featureRepository.findById(featureId).orElse(null);
        if (feature == null) {
            return;
        }

        long totalTasks = taskRepository.countByFeatureId(featureId);
        if (totalTasks == 0) {
            return;
        }

        long resolvedTasks = taskRepository.countByFeatureIdAndStatus(featureId, TaskStatus.RESOLVED);
        double newRatio = (double) resolvedTasks / totalTasks;

        // Atomically update the readiness ratio (aggregate projection, unconditionally overwrite)
        featureRepository.updateReadinessAtomically(featureId, newRatio);
    }

    private double calculateFalsificationReadiness() {
        long totalTasks = taskRepository.count();
        if (totalTasks == 0) {
            return 1.0; // Assume 100% readiness if there are no tasks
        }

        long resolvedTasks = taskRepository.countByStatus(TaskStatus.RESOLVED);
        return (double) resolvedTasks / totalTasks;
    }

    public static class TaskResolutionResult {
        private final int resolvedCount;
        private final double readiness;

        public TaskResolutionResult(int resolvedCount, double readiness) {
            this.resolvedCount = resolvedCount;
            this.readiness = readiness;
        }

        public int getResolvedCount() {
            return resolvedCount;
        }

        public double getReadiness() {
            return readiness;
        }
    }
}
