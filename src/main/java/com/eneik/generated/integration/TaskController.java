package com.eneik.generated.integration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/resolve")
    public ResponseEntity<TaskService.TaskResolutionResult> resolveTasks() {
        TaskService.TaskResolutionResult result = taskService.resolveTasksAndCalculateReadiness();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/state")
    public ResponseEntity<Map<String, String>> getFlowCoreState() {
        Map<String, String> response = new HashMap<>();
        response.put("state", taskService.getFlowCoreState());
        return ResponseEntity.ok(response);
    }
}
