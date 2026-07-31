package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TaskTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskTimeoutScheduler.class);

    private final TaskRepository taskRepository;
    private final FeatureRepository featureRepository;

    public TaskTimeoutScheduler(TaskRepository taskRepository, FeatureRepository featureRepository) {
        this.taskRepository = taskRepository;
        this.featureRepository = featureRepository;
    }

    /**
     * Periodically check for tasks that are stuck in PENDING_REVIEW for more than 4 hours.
     * Transition them atomically to FAILED, and update their respective features.
     * We run this scheduled check every minute (or as desired).
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkPendingReviewTimeouts() {
        log.info("Checking for pending_review timeouts...");
        List<Task> pendingReviewTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        LocalDateTime fourHoursAgo = LocalDateTime.now().minusHours(4);
        int timedOutCount = 0;
        Set<Long> affectedFeatureIds = new HashSet<>();

        for (Task task : pendingReviewTasks) {
            if (task.getStatusChangedAt() != null && task.getStatusChangedAt().isBefore(fourHoursAgo)) {
                // Atomically update status to FAILED from PENDING_REVIEW
                int updated = taskRepository.updateStatusAtomically(task.getId(), TaskStatus.PENDING_REVIEW, TaskStatus.FAILED);
                if (updated > 0) {
                    timedOutCount++;
                    if (task.getFeatureId() != null) {
                        affectedFeatureIds.add(task.getFeatureId());
                    }
                }
            }
        }

        if (timedOutCount > 0) {
            log.info("Timed out {} pending_review tasks to FAILED state.", timedOutCount);
            for (Long featureId : affectedFeatureIds) {
                updateFeatureReadiness(featureId);
            }
        }
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

        featureRepository.updateReadinessAtomically(featureId, newRatio);
    }
}
