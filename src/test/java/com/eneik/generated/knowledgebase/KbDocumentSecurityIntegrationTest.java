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

    private String getJwtToken(String username, String role) throws Exception {
        String responseStr = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return responseStr.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    public void setup() throws Exception {
        // Clear database in correct order
        auditLogRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        String adminToken = getJwtToken("admin_user", "ADMINISTRATOR");

        // Upload a sample document using administrator context
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
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        sampleDocId = Long.parseLong(docResponseStr.split("\"id\":")[1].split(",")[0].trim());

        // Assert an audit log for DOCUMENT_CREATE was recorded with correct admin_user
        List<KbAuditLog> creationLogs = auditLogRepository.findAll();
        assertFalse(creationLogs.isEmpty(), "Audit log should be recorded for document creation");
        KbAuditLog createLog = creationLogs.get(0);
        assertEquals("DOCUMENT_CREATE", createLog.getAction());
        assertEquals("admin_user", createLog.getUser().getUsername());
        assertEquals("ADMINISTRATOR", createLog.getUser().getRole());
    }

    @Test
    public void testStudentDeleteDocumentReturns403Forbidden() throws Exception {
        String studentToken = getJwtToken("student_john", "STUDENT");

        // Attempt to delete as student
        mockMvc.perform(delete("/api/v1/integration/documents/{id}", sampleDocId)
                        .header("Authorization", "Bearer " + studentToken))
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
        String studentToken = getJwtToken("student_john", "STUDENT");

        // Attempt to update as student
        MockMultipartFile updatedFile = new MockMultipartFile(
                "file",
                "updated_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Updated context".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents/{id}", sampleDocId)
                        .file(updatedFile)
                        .param("title", "Unauthorized Edit Title")
                        .header("Authorization", "Bearer " + studentToken)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isForbidden());

        // Verify title remained unchanged
        KbDocument doc = documentRepository.findById(sampleDocId).orElseThrow();
        assertEquals("Sample Standard Document", doc.getTitle());
    }

    @Test
    public void testStudentUploadDocumentReturns403Forbidden() throws Exception {
        String studentToken = getJwtToken("student_john", "STUDENT");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "secret.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Secret data".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "Student Upload Title")
                        .header("Authorization", "Bearer " + studentToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAdminEditAndDeleteSucceedsAndLogs() throws Exception {
        String editorToken = getJwtToken("admin_editor", "ADMINISTRATOR");
        String destroyerToken = getJwtToken("admin_destroyer", "ADMINISTRATOR");

        // 1. Update document as Admin
        MockMultipartFile updatedFile = new MockMultipartFile(
                "file",
                "updated_doc.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Updated context".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents/{id}", sampleDocId)
                        .file(updatedFile)
                        .param("title", "Authorized Edit Title")
                        .header("Authorization", "Bearer " + editorToken)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());

        // Verify update in DB
        KbDocument updatedDoc = documentRepository.findById(sampleDocId).orElseThrow();
        assertEquals("Authorized Edit Title", updatedDoc.getTitle());

        // Assert audit log for DOCUMENT_UPDATE was written
        KbAuditLog updateLog = auditLogRepository.findAll().stream()
                .filter(log -> "DOCUMENT_UPDATE".equals(log.getAction()))
                .findFirst()
                .orElse(null);
        assertNotNull(updateLog, "DOCUMENT_UPDATE audit log must exist");
        assertEquals("admin_editor", updateLog.getUser().getUsername());

        // 2. Delete document as Admin
        mockMvc.perform(delete("/api/v1/integration/documents/{id}", sampleDocId)
                        .header("Authorization", "Bearer " + destroyerToken))
                .andExpect(status().isNoContent());

        // Verify deleted from DB
        assertFalse(documentRepository.findById(sampleDocId).isPresent(), "Document should be deleted");

        // Assert audit log for DOCUMENT_DELETE was written
        KbAuditLog deleteLog = auditLogRepository.findAll().stream()
                .filter(log -> "DOCUMENT_DELETE".equals(log.getAction()))
                .findFirst()
                .orElse(null);
        assertNotNull(deleteLog, "DOCUMENT_DELETE audit log must exist");
        assertEquals("admin_destroyer", deleteLog.getUser().getUsername());
    }

    @Test
    public void testInvalidRequestTriggers400BadRequestError() throws Exception {
        String adminToken = getJwtToken("admin_user", "ADMINISTRATOR");

        // Attempt upload without file or title
        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testValidCredentialsLoginGeneratesJwtToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin_jwt_user\",\"password\":\"password\",\"role\":\"ADMINISTRATOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("admin_jwt_user"))
                .andExpect(jsonPath("$.role").value("ADMINISTRATOR"));
    }

    @Test
    public void testInvalidCredentialsLoginFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin_jwt_user\",\"password\":\"wrong_password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testJwtAuthAccessAndIgnoringSpoofedHeaders() throws Exception {
        // 1. Log in to get standard Admin JWT
        String responseStr = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin_jwt_test\",\"password\":\"password\",\"role\":\"ADMINISTRATOR\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = responseStr.split("\"token\":\"")[1].split("\"")[0];

        // 2. Perform Admin action (e.g. Upload a document) using JWT and spoofed X-User-Role: STUDENT header
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jwt_test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "JWT context".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file)
                        .param("title", "JWT Uploaded Doc")
                        .param("category", "Norms")
                        .param("tags", "jwt")
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Role", "STUDENT") // Spoofed student header
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()); // This succeeds because the system securely authorizes the user as ADMINISTRATOR from the JWT and ignores the spoofed STUDENT header!

        // 3. Log in to get standard Student JWT
        String studentResponseStr = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student_jwt_test\",\"password\":\"password\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String studentToken = studentResponseStr.split("\"token\":\"")[1].split("\"")[0];

        // 4. Perform Admin action (e.g. Upload a document) using Student JWT and spoofed X-User-Role: ADMINISTRATOR header
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "jwt_test_2.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "JWT context 2".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/integration/documents")
                        .file(file2)
                        .param("title", "Student Spoofing Upload")
                        .header("Authorization", "Bearer " + studentToken)
                        .header("X-User-Role", "ADMINISTRATOR") // Spoofed admin header
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // This is forbidden because the system authorizes them as STUDENT from the JWT and ignores the spoofed ADMINISTRATOR header!
    }

    @Test
    public void testInvalidJwtTokenReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/integration/documents")
                        .header("Authorization", "Bearer invalid-jwt-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLacksValidJwtButProvidesRoleSpoofingHeaderIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/integration/documents")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .header("X-User-Name", "some_user"))
                .andExpect(status().isUnauthorized());
    }
}
