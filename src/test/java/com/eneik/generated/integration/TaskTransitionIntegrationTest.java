package com.eneik.generated.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TaskTransitionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    private static final String STUCK_TASK_ID = "529e5252-040a-4889-9f61-366ea6e9e089";

    @BeforeEach
    public void setup() {
        // Ensure the stuck task exists in its initial pending_review state
        Task task = new Task();
        task.setId(STUCK_TASK_ID);
        task.setTitle("Stalled pipeline review task");
        task.setStatus("pending_review");
        taskRepository.save(task);
    }

    @Test
    public void testGetStalledTaskInitiallyInPendingReview() throws Exception {
        mockMvc.perform(get("/api/v1/integration/tasks/{id}", STUCK_TASK_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STUCK_TASK_ID))
                .andExpect(jsonPath("$.status").value("pending_review"));
    }

    @Test
    public void testSuccessfulTaskTransitionOutofPendingReview() throws Exception {
        // When the patched logic is applied (POST transition request)
        mockMvc.perform(post("/api/v1/integration/tasks/{id}/transition", STUCK_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"pending_review\",\"newStatus\":\"resolved\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STUCK_TASK_ID))
                .andExpect(jsonPath("$.status").value("resolved"));

        // Verify the database state has updated
        Task updated = taskRepository.findById(STUCK_TASK_ID).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("resolved");
    }

    @Test
    public void testAtomicGuardPreventsStaleTransition() throws Exception {
        // First transition to resolved succeeds
        mockMvc.perform(post("/api/v1/integration/tasks/{id}/transition", STUCK_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"pending_review\",\"newStatus\":\"resolved\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // A second transition with expectedStatus 'pending_review' must fail since the task is now 'resolved'
        mockMvc.perform(post("/api/v1/integration/tasks/{id}/transition", STUCK_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"pending_review\",\"newStatus\":\"completed\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Expected status 'pending_review' but task '" + STUCK_TASK_ID + "' was in status 'resolved'")));
    }

    @Test
    public void testNonExistentTaskReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/integration/tasks/non-existent-id/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"pending_review\",\"newStatus\":\"resolved\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testInvalidTransitionRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/integration/tasks/{id}/transition", STUCK_TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedStatus\":\"\",\"newStatus\":\"resolved\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
