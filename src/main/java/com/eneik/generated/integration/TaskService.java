package com.eneik.generated.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResolutionResult resolveTasksAndCalculateReadiness() {
        log.info("Starting task resolution process...");
        List<Task> readyTasks = taskRepository.findByStatus(TaskStatus.READY);

        int resolvedCount = 0;
        for (Task task : readyTasks) {
            int updated = taskRepository.updateStatusAtomically(task.getId(), TaskStatus.READY, TaskStatus.RESOLVED);
            if (updated > 0) {
                resolvedCount++;
            }
        }
        log.info("Resolved {} out of {} ready tasks.", resolvedCount, readyTasks.size());

        double readiness = calculateFalsificationReadiness();
        log.info("Calculated falsification readiness: {}", readiness);

        return new TaskResolutionResult(resolvedCount, readiness);
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
