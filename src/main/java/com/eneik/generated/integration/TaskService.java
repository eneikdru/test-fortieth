package com.eneik.generated.integration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    @Transactional
    public Task transitionTask(String id, String expectedStatus, String newStatus) {
        // Attempt an atomically-guarded conditional update first
        int updatedRows = taskRepository.updateStatusAtomic(id, newStatus, expectedStatus);

        if (updatedRows == 0) {
            // Either the task does not exist, or it is not in the expected status.
            // Fetch current to provide a detailed and accurate error message.
            Optional<Task> currentTaskOpt = taskRepository.findById(id);
            if (currentTaskOpt.isEmpty()) {
                throw new IllegalArgumentException("Task not found with ID: " + id);
            }
            Task currentTask = currentTaskOpt.get();
            throw new IllegalStateException(String.format(
                    "State transition failed. Expected status '%s' but task '%s' was in status '%s'.",
                    expectedStatus, id, currentTask.getStatus()
            ));
        }

        // Return the freshly updated task from the database
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Task vanished after successful state transition."));
    }
}
