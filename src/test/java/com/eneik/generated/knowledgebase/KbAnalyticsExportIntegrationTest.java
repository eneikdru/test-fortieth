package com.eneik.generated.knowledgebase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.eneik.generated.integration.SyncServiceIntegrationTest;
import com.eneik.generated.integration.EiosClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KbAnalyticsExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    @Autowired
    private EiosClient eiosClient;

    private String getJwtToken(String username, String role) throws Exception {
        String responseStr = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return responseStr.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    public void setup() {
        auditLogRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testAdminExportCsvAndPdfSuccess() throws Exception {
        String adminToken = getJwtToken("admin_user", "ADMINISTRATOR");

        // 1. Create some audit logs to have data
        KbUser adminUser = userRepository.findByUsername("admin_user").orElseThrow();

        KbAuditLog log1 = new KbAuditLog();
        log1.setUser(adminUser);
        log1.setAction("SEARCH");
        log1.setTargetEntity("Search");
        log1.setDetails("epidemiology residency");
        auditLogRepository.save(log1);

        KbAuditLog log2 = new KbAuditLog();
        log2.setUser(adminUser);
        log2.setAction("SEARCH");
        log2.setTargetEntity("Search");
        log2.setDetails("epidemiology residency");
        auditLogRepository.save(log2);

        // 2. Export as CSV
        String csvContent = mockMvc.perform(get("/api/v1/integration/analytics/export")
                        .param("format", "csv")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("analytics_report.csv")))
                .andReturn().getResponse().getContentAsString();

        assertTrue(csvContent.contains("Popular Searches"));
        assertTrue(csvContent.contains("epidemiology residency,2"));

        // 3. Export as PDF
        byte[] pdfBytes = mockMvc.perform(get("/api/v1/integration/analytics/export")
                        .param("format", "pdf")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(header().string("Content-Disposition", containsString("analytics_report.pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(pdfBytes.length > 0);
        String pdfStr = new String(pdfBytes, StandardCharsets.UTF_8);
        assertTrue(pdfStr.contains("%PDF-1.4"));
        assertTrue(pdfStr.contains("Knowledge Base Analytics Report"));
        assertTrue(pdfStr.contains("epidemiology residency: 2"));

        // 4. Assert audit logs were created for EXPORT_ANALYTICS
        long exportLogs = auditLogRepository.findAll().stream()
                .filter(l -> "EXPORT_ANALYTICS".equals(l.getAction()))
                .count();
        assertEquals(2, exportLogs);
    }

    @Test
    public void testAdminTriggerEiosSync() throws Exception {
        String adminToken = getJwtToken("admin_sync", "ADMINISTRATOR");

        // Create some search logs
        KbUser adminUser = userRepository.findByUsername("admin_sync").orElseThrow();
        KbAuditLog log = new KbAuditLog();
        log.setUser(adminUser);
        log.setAction("SEARCH");
        log.setTargetEntity("Search");
        log.setDetails("clinical guidelines");
        auditLogRepository.save(log);

        // Export as eios
        mockMvc.perform(get("/api/v1/integration/analytics/export")
                        .param("format", "eios")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Analytics synchronized to EIOS successfully")));

        // Verify MockEiosClient received the synced data if active
        if (eiosClient instanceof SyncServiceIntegrationTest.MockEiosClient) {
            SyncServiceIntegrationTest.MockEiosClient mockEios = (SyncServiceIntegrationTest.MockEiosClient) eiosClient;
            String syncedData = mockEios.getLastSyncedAnalytics();
            assertNotNull(syncedData);
            assertTrue(syncedData.contains("clinical guidelines,1"));
        }

        // Verify audit log for EIOS_SYNC_ANALYTICS was written
        long syncLogs = auditLogRepository.findAll().stream()
                .filter(l -> "EIOS_SYNC_ANALYTICS".equals(l.getAction()))
                .count();
        assertEquals(1, syncLogs);
    }

    @Test
    public void testStudentExportReturnsForbidden() throws Exception {
        String studentToken = getJwtToken("student_user", "STUDENT");

        mockMvc.perform(get("/api/v1/integration/analytics/export")
                        .param("format", "csv")
                        .header("Authorization", "Bearer " + studentToken)
                        .accept(MediaType.ALL))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUnauthorizedRequestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/integration/analytics/export")
                        .param("format", "csv")
                        .header("X-User-Role", "ADMINISTRATOR")
                        .header("X-User-Name", "fake_admin")
                        .accept(MediaType.ALL))
                .andExpect(status().isUnauthorized());
    }
}
