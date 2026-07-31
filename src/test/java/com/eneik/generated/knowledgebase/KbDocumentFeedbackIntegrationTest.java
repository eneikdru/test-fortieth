package com.eneik.generated.knowledgebase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KbDocumentFeedbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbDocumentVersionRepository versionRepository;

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    @Autowired
    private KbDocumentCommentRepository commentRepository;

    @Autowired
    private KbDocumentUpdateRequestRepository updateRequestRepository;

    @BeforeEach
    public void setup() {
        auditLogRepository.deleteAll();
        commentRepository.deleteAll();
        updateRequestRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testCommentsAndUpdateRequestsWorkflow() throws Exception {
        // 1. Upload a document
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Initial Content".getBytes(StandardCharsets.UTF_8)
        );

        String docIdStr = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Clinical Residency Pediatrics Guide")
                        .param("category", "Pediatrics")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Name", "dr_pediatrician")
                        .header("X-User-Role", "TEACHER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title").value("Clinical Residency Pediatrics Guide"))
                .andReturn().getResponse().getContentAsString();

        Long docId = Long.parseLong(docIdStr.split("\"id\":")[1].split(",")[0].trim());

        // 2. Submit a comment on the document
        String commentJson = "{ \"commentText\": \"This is an exceptionally detailed residency document.\" }";
        mockMvc.perform(post("/api/v1/integration/documents/{id}/comments", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson)
                        .header("X-User-Name", "student_john")
                        .header("X-User-Role", "LEARNER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.documentId").value(docId))
                .andExpect(jsonPath("$.username").value("student_john"))
                .andExpect(jsonPath("$.commentText").value("This is an exceptionally detailed residency document."))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        // 3. Submit an update request on the document
        String updateRequestJson = "{ \"requestText\": \"Please update Section 4 regarding clinical practice hours.\" }";
        String urResponse = mockMvc.perform(post("/api/v1/integration/documents/{id}/update-requests", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson)
                        .header("X-User-Name", "dr_pediatrician")
                        .header("X-User-Role", "TEACHER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.documentId").value(docId))
                .andExpect(jsonPath("$.username").value("dr_pediatrician"))
                .andExpect(jsonPath("$.requestText").value("Please update Section 4 regarding clinical practice hours."))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long urId = Long.parseLong(urResponse.split("\"id\":")[1].split(",")[0].trim());

        // 4. Retrieve the document and ensure comments and update requests are returned
        mockMvc.perform(get("/api/v1/integration/documents/{id}", docId)
                        .header("X-User-Name", "student_john")
                        .header("X-User-Role", "LEARNER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clinical Residency Pediatrics Guide"))
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].commentText").value("This is an exceptionally detailed residency document."))
                .andExpect(jsonPath("$.comments[0].username").value("student_john"))
                .andExpect(jsonPath("$.updateRequests", hasSize(1)))
                .andExpect(jsonPath("$.updateRequests[0].requestText").value("Please update Section 4 regarding clinical practice hours."))
                .andExpect(jsonPath("$.updateRequests[0].status").value("PENDING"));

        // 5. Test status transition atomically
        String statusPatchJson = "{ \"oldStatus\": \"PENDING\", \"newStatus\": \"RESOLVED\" }";
        mockMvc.perform(patch("/api/v1/integration/documents/update-requests/{requestId}/status", urId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusPatchJson)
                        .header("X-User-Name", "admin")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(urId))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // 6. Test conflict transition if old status does not match
        String conflictPatchJson = "{ \"oldStatus\": \"PENDING\", \"newStatus\": \"REJECTED\" }";
        mockMvc.perform(patch("/api/v1/integration/documents/update-requests/{requestId}/status", urId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictPatchJson)
                        .header("X-User-Name", "admin")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isConflict());
    }
}
