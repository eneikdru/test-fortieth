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
public class KbDocumentSecurityIntegrationTest {

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

    private Long sampleDocId;
    private String adminToken;
    private String studentToken;

    private String getJwtToken(String username, String role) throws Exception {
        String loginJson = "{\"username\":\"" + username + "\", \"password\":\"password\", \"role\":\"" + role + "\"}";
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + loginResponse.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    public void setup() throws Exception {
        // Clear database in correct order
        auditLogRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // Get JWT tokens
        adminToken = getJwtToken("admin_user", "ADMINISTRATOR");
        studentToken = getJwtToken("student_john", "STUDENT");

        // Upload a sample document using administrator context and sending spoofed student headers to prove they are ignored
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Sample context".getBytes(StandardCharsets.UTF_8)
        );

        String docResponseStr = mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Sample Standard Document")
                        .param("category", "Norms")
                        .param("tags", "test")
                        .header("Authorization", adminToken)
                        .header("X-User-Name", "student_john")
                        .header("X-User-Role", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        sampleDocId = Long.parseLong(docResponseStr.split("\"id\":")[1].split(",")[0].trim());

        // Assert an audit log for DOCUMENT_CREATE was recorded with correct admin_user, confirming spoofed headers were ignored
        List<KbAuditLog> creationLogs = auditLogRepository.findAll();
        assertFalse(creationLogs.isEmpty(), "Audit log should be recorded for document creation");
        KbAuditLog createLog = creationLogs.get(0);
        assertEquals("DOCUMENT_CREATE", createLog.getAction());
        assertEquals("admin_user", createLog.getUser().getUsername());
        assertEquals("ADMINISTRATOR", createLog.getUser().getRole());
    }

    @Test
    public void testStudentDeleteDocumentReturns403Forbidden() throws Exception {
        // Attempt to delete as student, even if spoofing admin headers
        mockMvc.perform(delete("/api/v1/integration/documents/{id}", sampleDocId)
                        .header("Authorization", studentToken)
                        .header("X-User-Name", "admin_user")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isForbidden());

        // Verify document was NOT deleted from database
        assertTrue(documentRepository.findById(sampleDocId).isPresent(), "Document should not have been deleted");

        // Verify no delete audit log was created
        long deleteLogCount = auditLogRepository.findAll().stream()
                .filter(log -> "DOCUMENT_DELETE".equals(log.getAction()))
                .count();
        assertEquals(0, deleteLogCount, "Should not record a DELETE log if action was denied");
    }

    @Test
    public void testStudentUpdateDocumentReturns403Forbidden() throws Exception {
        // Attempt to update as student, even if spoofing admin headers
        MockMultipartFile updatedFile = new MockMultipartFile(
                "file",
                "updated_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Updated context".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents/{id}", sampleDocId)
                        .file(updatedFile)
                        .param("title", "Unauthorized Edit Title")
                        .header("Authorization", studentToken)
                        .header("X-User-Name", "admin_user")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isForbidden());

        // Verify title remained unchanged
        KbDocument doc = documentRepository.findById(sampleDocId).orElseThrow();
        assertEquals("Sample Standard Document", doc.getTitle());
    }

    @Test
    public void testStudentUploadDocumentReturns403Forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "secret.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Secret data".getBytes(StandardCharsets.UTF_8)
        );

        // Attempt to upload as student, even if spoofing admin headers
        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Student Upload Title")
                        .header("Authorization", studentToken)
                        .header("X-User-Name", "admin_user")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAdminEditAndDeleteSucceedsAndLogs() throws Exception {
        // 1. Update document as Admin, even if spoofing student headers
        MockMultipartFile updatedFile = new MockMultipartFile(
                "file",
                "updated_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Updated context".getBytes(StandardCharsets.UTF_8)
        );

        String adminEditorToken = getJwtToken("admin_editor", "ADMINISTRATOR");

        mockMvc.perform(multipart("/api/v1/integration/documents/{id}", sampleDocId)
                        .file(updatedFile)
                        .param("title", "Authorized Edit Title")
                        .header("Authorization", adminEditorToken)
                        .header("X-User-Name", "student_john")
                        .header("X-User-Role", "STUDENT")
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());

        // Verify update in DB
        KbDocument updatedDoc = documentRepository.findById(sampleDocId).orElseThrow();
        assertEquals("Authorized Edit Title", updatedDoc.getTitle());

        // Assert audit log for DOCUMENT_UPDATE was written with admin_editor
        KbAuditLog updateLog = auditLogRepository.findAll().stream()
                .filter(log -> "DOCUMENT_UPDATE".equals(log.getAction()))
                .findFirst()
                .orElse(null);
        assertNotNull(updateLog, "DOCUMENT_UPDATE audit log must exist");
        assertEquals("admin_editor", updateLog.getUser().getUsername());

        // 2. Delete document as Admin, even if spoofing student headers
        String adminDestroyerToken = getJwtToken("admin_destroyer", "ADMINISTRATOR");

        mockMvc.perform(delete("/api/v1/integration/documents/{id}", sampleDocId)
                        .header("Authorization", adminDestroyerToken)
                        .header("X-User-Name", "student_john")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNoContent());

        // Verify deleted from DB
        assertFalse(documentRepository.findById(sampleDocId).isPresent(), "Document should be deleted");

        // Assert audit log for DOCUMENT_DELETE was written with admin_destroyer
        KbAuditLog deleteLog = auditLogRepository.findAll().stream()
                .filter(log -> "DOCUMENT_DELETE".equals(log.getAction()))
                .findFirst()
                .orElse(null);
        assertNotNull(deleteLog, "DOCUMENT_DELETE audit log must exist");
        assertEquals("admin_destroyer", deleteLog.getUser().getUsername());
    }

    @Test
    public void testInvalidRequestTriggers400BadRequestError() throws Exception {
        // Attempt upload without file or title
        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testMissingAuthorizationHeaderReturns401Unauthorized() throws Exception {
        // Attempt to search documents without providing JWT token
        mockMvc.perform(get("/api/v1/integration/documents")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testInvalidAuthorizationHeaderReturns401Unauthorized() throws Exception {
        // Attempt to search documents with an invalid JWT token
        mockMvc.perform(get("/api/v1/integration/documents")
                        .header("Authorization", "Bearer invalid_token_value_here")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
