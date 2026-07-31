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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KbDocumentCommentIntegrationTest {

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
    private jakarta.persistence.EntityManager entityManager;

    private Long sampleDocId;

    @BeforeEach
    public void setup() throws Exception {
        // Clear database in correct order
        auditLogRepository.deleteAll();
        commentRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Create an administrator and upload a document
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "standards.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Standard content".getBytes(StandardCharsets.UTF_8)
        );

        String docResponseStr = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Clinical Guidelines 2026")
                        .param("category", "Guidelines")
                        .param("tags", "clinical")
                        .header("X-User-Name", "expert_doctor")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        sampleDocId = Long.parseLong(docResponseStr.split("\"id\":")[1].split(",")[0].trim());
    }

    @Test
    public void testSubmitCommentAndUpdateRequestAndRetrieveDocument() throws Exception {
        // 1. Submit a comment as authenticated user
        String commentJson = "{"
                + "\"content\": \"This section requires additional literature sources.\","
                + "\"type\": \"COMMENT\""
                + "}";

        mockMvc.perform(post("/api/v1/integration/documents/{id}/comments", sampleDocId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson)
                        .header("X-User-Name", "reviewer_alice")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.content", is("This section requires additional literature sources.")))
                .andExpect(jsonPath("$.type", is("COMMENT")))
                .andExpect(jsonPath("$.authorUsername", is("reviewer_alice")));

        // 2. Submit an update request as another user
        String updateRequestJson = "{"
                + "\"content\": \"Requesting actualization of pediatric dosages.\","
                + "\"type\": \"UPDATE_REQUEST\""
                + "}";

        mockMvc.perform(post("/api/v1/integration/documents/{id}/comments", sampleDocId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson)
                        .header("X-User-Name", "doctor_bob")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.content", is("Requesting actualization of pediatric dosages.")))
                .andExpect(jsonPath("$.type", is("UPDATE_REQUEST")))
                .andExpect(jsonPath("$.authorUsername", is("doctor_bob")));

        // Verify the data is saved in DB and associated with the document
        List<KbDocumentComment> commentsInDb = commentRepository.findByDocumentIdOrderByCreatedAtAsc(sampleDocId);
        assertEquals(2, commentsInDb.size(), "Two collaboration entries should be persisted in DB for the document");
        assertEquals("This section requires additional literature sources.", commentsInDb.get(0).getContent());
        assertEquals("COMMENT", commentsInDb.get(0).getType());
        assertEquals("Requesting actualization of pediatric dosages.", commentsInDb.get(1).getContent());
        assertEquals("UPDATE_REQUEST", commentsInDb.get(1).getType());

        // 3. Retrieve the document and verify the comments are correctly available
        mockMvc.perform(get("/api/v1/integration/documents/{id}", sampleDocId)
                        .header("X-User-Name", "any_user")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sampleDocId.intValue())))
                .andExpect(jsonPath("$.comments", hasSize(2)))
                .andExpect(jsonPath("$.comments[0].content", is("This section requires additional literature sources.")))
                .andExpect(jsonPath("$.comments[0].type", is("COMMENT")))
                .andExpect(jsonPath("$.comments[0].authorUsername", is("reviewer_alice")))
                .andExpect(jsonPath("$.comments[1].content", is("Requesting actualization of pediatric dosages.")))
                .andExpect(jsonPath("$.comments[1].type", is("UPDATE_REQUEST")))
                .andExpect(jsonPath("$.comments[1].authorUsername", is("doctor_bob")));

        // Verify audit logs are created for adding comments
        List<KbAuditLog> addCommentLogs = auditLogRepository.findAll().stream()
                .filter(log -> "ADD_COMMENT".equals(log.getAction()))
                .toList();
        assertEquals(2, addCommentLogs.size(), "Two ADD_COMMENT audit logs should be registered");
    }

    @Test
    public void testSubmitCommentWithEmptyContentReturns400BadRequest() throws Exception {
        String commentJson = "{"
                + "\"content\": \"   \","
                + "\"type\": \"COMMENT\""
                + "}";

        mockMvc.perform(post("/api/v1/integration/documents/{id}/comments", sampleDocId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson)
                        .header("X-User-Name", "reviewer_alice")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitCommentWithInvalidTypeReturns400BadRequest() throws Exception {
        String commentJson = "{"
                + "\"content\": \"Valid content but wrong type\","
                + "\"type\": \"INVALID_COLLABORATION_TYPE\""
                + "}";

        mockMvc.perform(post("/api/v1/integration/documents/{id}/comments", sampleDocId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson)
                        .header("X-User-Name", "reviewer_alice")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteDocumentCascadesCommentsDeletion() throws Exception {
        // 1. Submit a valid comment for the sample document
        String commentJson = "{"
                + "\"content\": \"Temporary comment to check cascaded delete.\","
                + "\"type\": \"COMMENT\""
                + "}";

        mockMvc.perform(post("/api/v1/integration/documents/{id}/comments", sampleDocId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson)
                        .header("X-User-Name", "reviewer_alice")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isCreated());

        // Verify the comment is persisted in DB and associated with the document
        List<KbDocumentComment> commentsBeforeDelete = commentRepository.findByDocumentIdOrderByCreatedAtAsc(sampleDocId);
        assertEquals(1, commentsBeforeDelete.size(), "One comment should be persisted in DB for the document before deletion");

        // 2. Delete the document
        mockMvc.perform(delete("/api/v1/integration/documents/{id}", sampleDocId)
                        .header("X-User-Name", "reviewer_alice")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        // 3. Verify that the comments are deleted as well
        List<KbDocumentComment> commentsAfterDelete = commentRepository.findByDocumentIdOrderByCreatedAtAsc(sampleDocId);
        assertTrue(commentsAfterDelete.isEmpty(), "Comments should be cascaded deleted when the document is deleted");
    }
}
