package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@EnableScheduling
public class TaskTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskTimeoutScheduler.class);

    private final TaskRepository taskRepository;
    private final FeatureRepository featureRepository;
    private final Clock clock;
    private final int timeoutHours;

    @org.springframework.beans.factory.annotation.Autowired
    public TaskTimeoutScheduler(
            TaskRepository taskRepository,
            FeatureRepository featureRepository,
            @Value("${tasks.pending-review.timeout-hours:12}") int timeoutHours) {
        this(taskRepository, featureRepository, Clock.systemDefaultZone(), timeoutHours);
    }

    public TaskTimeoutScheduler(
            TaskRepository taskRepository,
            FeatureRepository featureRepository,
            Clock clock,
            int timeoutHours) {
        this.taskRepository = taskRepository;
        this.featureRepository = featureRepository;
        this.clock = clock;
        this.timeoutHours = timeoutHours;
    }

    @Scheduled(fixedRateString = "${tasks.pending-review.check-interval-ms:3600000}")
    @Transactional
    public void checkPendingReviewTimeouts() {
        log.info("Running scheduled check for stuck pending_review tasks...");
        LocalDateTime cutoff = LocalDateTime.now(clock).minusHours(timeoutHours);

        List<Task> pendingReviewTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        int failedCount = 0;
        Set<Long> affectedFeatureIds = new HashSet<>();

        for (Task task : pendingReviewTasks) {
            if (task.getStatusChangedAt() != null && task.getStatusChangedAt().isBefore(cutoff)) {
                int updated = taskRepository.updateStatusAtomically(task.getId(), TaskStatus.PENDING_REVIEW, TaskStatus.FAILED);
                if (updated > 0) {
                    failedCount++;
                    if (task.getFeatureId() != null) {
                        affectedFeatureIds.add(task.getFeatureId());
                    }
                }
            }
        }

        if (failedCount > 0) {
            log.info("Scheduled check failed {} stuck pending_review tasks.", failedCount);
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
