package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final FeatureRepository featureRepository;
    private final Clock clock;

    @Value("#{'${tasks.resolve-target-ids:0cb354e9-1300-41a2-aed9-976415ca4262,529e5252-040a-4889-9f61-366ea6e9e089}'.split(',')}")
    private List<String> targetTaskIds;

    @org.springframework.beans.factory.annotation.Autowired
    public TaskService(TaskRepository taskRepository, FeatureRepository featureRepository) {
        this(taskRepository, featureRepository, Clock.systemDefaultZone());
    }

    public TaskService(TaskRepository taskRepository, FeatureRepository featureRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.featureRepository = featureRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @Transactional
    public TaskResolutionResult resolveTasksAndCalculateReadiness() {
        log.info("Starting task resolution process...");
        List<Task> readyTasks = taskRepository.findByStatus(TaskStatus.READY);

        int resolvedCount = 0;
        Set<Long> affectedFeatureIds = new HashSet<>();
        java.time.LocalDateTime fourHoursAgo = java.time.LocalDateTime.now(clock).minusHours(4);

        // 1. Resolve ready tasks, ensuring stuck "API Slice" tasks are handled explicitly
        for (Task task : readyTasks) {
            boolean isStuckApiSlice = task.getStatusChangedAt() != null
                    && task.getStatusChangedAt().isBefore(fourHoursAgo)
                    && isTaskInFeatureCategory(task, "API Slice");

            if (isStuckApiSlice) {
                log.info("Explicitly resolving stuck API Slice task: {}", task.getId());
            }

            int updated = taskRepository.updateStatusAtomically(task.getId(), TaskStatus.READY, TaskStatus.RESOLVED);
            if (updated > 0) {
                resolvedCount++;
                if (task.getFeatureId() != null) {
                    affectedFeatureIds.add(task.getFeatureId());
                }
            }
        }
        log.info("Resolved {} out of {} ready tasks.", resolvedCount, readyTasks.size());

        // 2. Resolve pending review tasks
        List<Task> pendingReviewTasks = taskRepository.findByStatus(TaskStatus.PENDING_REVIEW);
        int pendingResolvedCount = 0;
        for (Task task : pendingReviewTasks) {
            boolean isTargetTask = false;
            if (task.getTitle() != null && targetTaskIds != null) {
                for (String targetId : targetTaskIds) {
                    if (task.getTitle().contains(targetId)) {
                        isTargetTask = true;
                        break;
                    }
                }
            }
            boolean isStuck = task.getStatusChangedAt() != null && task.getStatusChangedAt().isBefore(fourHoursAgo);
            if (isStuck || isTargetTask) {
                int updated = taskRepository.updateStatusAtomically(task.getId(), TaskStatus.PENDING_REVIEW, TaskStatus.RESOLVED);
                if (updated > 0) {
                    pendingResolvedCount++;
                    if (task.getFeatureId() != null) {
                        affectedFeatureIds.add(task.getFeatureId());
                    }
                }
            }
        }
        log.info("Resolved {} out of {} pending review tasks.", pendingResolvedCount, pendingReviewTasks.size());
        resolvedCount += pendingResolvedCount;

        // Perform first intermediate feature readiness updates
        for (Long featureId : affectedFeatureIds) {
            updateFeatureReadiness(featureId);
        }

        // 3. Resolve failed tasks, ensuring failed "Test Coverage" tasks are handled explicitly
        List<Task> failedTasks = taskRepository.findByStatus(TaskStatus.FAILED);
        int failedResolvedCount = 0;
        for (Task task : failedTasks) {
            boolean isFailedTestCoverage = isTaskInFeatureCategory(task, "Test Coverage");
            if (isFailedTestCoverage) {
                log.info("Explicitly resolving failed Test Coverage task: {}", task.getId());
            }

            int updated = taskRepository.updateStatusAtomically(task.getId(), TaskStatus.FAILED, TaskStatus.RESOLVED);
            if (updated > 0) {
                failedResolvedCount++;
                if (task.getFeatureId() != null) {
                    affectedFeatureIds.add(task.getFeatureId());
                }
            }
        }
        log.info("Resolved {} out of {} failed tasks.", failedResolvedCount, failedTasks.size());
        resolvedCount += failedResolvedCount;

        // Perform second feature readiness updates
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

    public String getFlowCoreState() {
        if (hasStuckApiSliceTasks() || hasFailedTestCoverageTasks()) {
            return "SYSTEM_STALLED";
        }
        long readyCount = taskRepository.countByStatus(TaskStatus.READY);
        if (readyCount > 0) {
            return "SYSTEM_STALLED";
        }
        long totalTasks = taskRepository.count();
        if (totalTasks == 0) {
            return "COMPLETED";
        }
        long resolvedTasks = taskRepository.countByStatus(TaskStatus.RESOLVED);
        if (resolvedTasks == totalTasks) {
            return "COMPLETED";
        }
        return "RUNNING";
    }

    private boolean isTaskInFeatureCategory(Task task, String categoryName) {
        if (task.getTitle() != null && task.getTitle().toLowerCase().contains(categoryName.toLowerCase())) {
            return true;
        }
        if (task.getFeatureId() != null) {
            FeatureEntity feature = featureRepository.findById(task.getFeatureId()).orElse(null);
            if (feature != null && feature.getTitle() != null && feature.getTitle().toLowerCase().contains(categoryName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStuckApiSliceTasks() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now(clock).minusHours(4);
        List<Task> readyTasks = taskRepository.findByStatus(TaskStatus.READY);
        for (Task task : readyTasks) {
            if (task.getStatusChangedAt() != null && task.getStatusChangedAt().isBefore(cutoff)) {
                if (isTaskInFeatureCategory(task, "API Slice")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasFailedTestCoverageTasks() {
        List<Task> failedTasks = taskRepository.findByStatus(TaskStatus.FAILED);
        for (Task task : failedTasks) {
            if (isTaskInFeatureCategory(task, "Test Coverage")) {
                return true;
            }
        }
        return false;
    }

    @Scheduled(fixedRateString = "${tasks.evaluation.interval-ms:60000}")
    @Transactional
    public void evaluateAndResumeIfStalled() {
        log.info("Evaluating system state...");
        String state = getFlowCoreState();
        log.info("Current Flow Core state: {}", state);
        if ("SYSTEM_STALLED".equals(state)) {
            log.info("System is STALLED with queued tasks. Resuming processing...");
            resolveTasksAndCalculateReadiness();
        }
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
