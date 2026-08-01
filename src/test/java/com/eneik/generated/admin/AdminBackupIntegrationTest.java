package com.eneik.generated.admin;

import com.eneik.generated.knowledgebase.KbAuditLog;
import com.eneik.generated.knowledgebase.KbAuditLogRepository;
import com.eneik.generated.knowledgebase.KbUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminBackupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private KbAuditLogRepository auditLogRepository;

    @MockBean
    private TimeProvider timeProvider;

    private static final String FIXED_TIMESTAMP_FILE = "backup_20260801_120000.zip";
    private static final Path BACKUP_FILE_PATH = Paths.get("data/backups", FIXED_TIMESTAMP_FILE);

    private String getJwtToken(String username, String role) throws Exception {
        String responseStr = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return responseStr.split("\"token\":\"")[1].split("\"")[0];
    }

    @BeforeEach
    public void setup() throws IOException {
        // Set up mock TimeProvider behavior
        LocalDateTime fixedTime = LocalDateTime.of(2026, 8, 1, 12, 0, 0);
        when(timeProvider.now()).thenReturn(fixedTime);

        // Ensure leftover test files are deleted
        Files.deleteIfExists(BACKUP_FILE_PATH);
    }

    @AfterEach
    public void cleanup() throws IOException {
        Files.deleteIfExists(BACKUP_FILE_PATH);
    }

    @Test
    public void testTriggerBackupSuccessAsAdmin() throws Exception {
        String adminToken = getJwtToken("backup_admin", "ADMINISTRATOR");

        // Trigger backup
        mockMvc.perform(post("/api/v1/admin/backup")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.backupFile").value(FIXED_TIMESTAMP_FILE))
                .andExpect(jsonPath("$.backupPath").value("data/backups/" + FIXED_TIMESTAMP_FILE))
                .andExpect(jsonPath("$.databaseBackupSize").isNumber())
                .andExpect(jsonPath("$.storageBackupSize").isNumber())
                .andExpect(jsonPath("$.totalBackupSize").isNumber())
                .andExpect(jsonPath("$.timestamp").value("2026-08-01T12:00:00"));

        // Verify file actually exists on disk
        assertTrue(Files.exists(BACKUP_FILE_PATH), "The combined backup zip file must be created on disk");

        // Open zip file and verify entries
        try (ZipFile zipFile = new ZipFile(BACKUP_FILE_PATH.toFile())) {
            assertNotNull(zipFile.getEntry("db/database_backup.zip"), "The database backup should be zipped inside db/database_backup.zip");
        }

        // Check if an audit log was successfully written for BACKUP_TRIGGER
        List<KbAuditLog> auditLogs = auditLogRepository.findAll();
        boolean hasBackupTriggerLog = auditLogs.stream()
                .anyMatch(log -> "BACKUP_TRIGGER".equals(log.getAction()) && "backup_admin".equals(log.getUser().getUsername()));
        assertTrue(hasBackupTriggerLog, "An audit log record must exist for BACKUP_TRIGGER action by backup_admin");
    }

    @Test
    public void testTriggerBackupForbiddenAsStudent() throws Exception {
        String studentToken = getJwtToken("backup_student", "STUDENT");

        mockMvc.perform(post("/api/v1/admin/backup")
                        .header("Authorization", "Bearer " + studentToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Verify no backup was created
        assertFalse(Files.exists(BACKUP_FILE_PATH), "No backup file should be created for unauthorized user");
    }

    @Test
    public void testTriggerBackupUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/v1/admin/backup")
                        .header("X-User-Role", "ADMINISTRATOR") // Role spoofing check
                        .header("X-User-Name", "hacker")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // Verify no backup was created
        assertFalse(Files.exists(BACKUP_FILE_PATH), "No backup file should be created without authentication");
    }
}
