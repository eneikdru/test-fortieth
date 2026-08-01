package com.eneik.generated.admin;

import com.eneik.generated.knowledgebase.JwtService;
import com.eneik.generated.knowledgebase.KbAuditLog;
import com.eneik.generated.knowledgebase.KbAuditLogRepository;
import com.eneik.generated.knowledgebase.KbUser;
import com.eneik.generated.knowledgebase.KbUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminBackupController {

    private final JdbcTemplate jdbcTemplate;
    private final KbUserRepository userRepository;
    private final KbAuditLogRepository auditLogRepository;
    private final TimeProvider timeProvider;

    @Autowired
    private JwtService jwtService;

    @Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest request;

    public AdminBackupController(JdbcTemplate jdbcTemplate,
                                 KbUserRepository userRepository,
                                 KbAuditLogRepository auditLogRepository,
                                 TimeProvider timeProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.timeProvider = timeProvider;
    }

    private KbUser resolveUser(String usernameHeader, String roleHeader) {
        String username = null;
        String role = null;
        boolean hasValidJwt = false;

        if (request != null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                JwtService.Claims claims = jwtService.parseToken(token);
                if (claims == null) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
                }
                username = claims.getUsername();
                role = claims.getRole();
                hasValidJwt = true;
            }
        }

        boolean hasSpoofingHeader = false;
        if (request != null) {
            String xRole = request.getHeader("X-User-Role");
            String xName = request.getHeader("X-User-Name");
            if ((xRole != null && !xRole.trim().isEmpty()) || (xName != null && !xName.trim().isEmpty())) {
                hasSpoofingHeader = true;
            }
        }

        if (hasSpoofingHeader && !hasValidJwt) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Role spoofing detected or valid JWT missing");
        }

        if (username == null) {
            username = (usernameHeader != null && !usernameHeader.trim().isEmpty()) ? usernameHeader.trim() : "system_user";
        }
        if (role == null) {
            role = (roleHeader != null && !roleHeader.trim().isEmpty()) ? roleHeader.trim() : "ADMINISTRATOR";
        }

        final String finalUsername = username;
        final String finalRole = role;
        return userRepository.findByUsername(finalUsername)
            .map(user -> {
                String authHeader = request != null ? request.getHeader("Authorization") : null;
                boolean isJwtUsed = authHeader != null && authHeader.startsWith("Bearer ");

                if (isJwtUsed) {
                    if (!user.getRole().equalsIgnoreCase(finalRole)) {
                        user.setRole(finalRole.toUpperCase());
                        return userRepository.save(user);
                    }
                } else if (roleHeader != null && !roleHeader.trim().isEmpty() && !user.getRole().equalsIgnoreCase(roleHeader.trim())) {
                    user.setRole(roleHeader.trim().toUpperCase());
                    return userRepository.save(user);
                }
                return user;
            })
            .orElseGet(() -> {
                KbUser user = new KbUser();
                user.setUsername(finalUsername);
                user.setRole(finalRole.toUpperCase());
                return userRepository.save(user);
            });
    }

    @PostMapping("/backup")
    public BackupResponse triggerBackup(
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser systemUser = resolveUser(usernameHeader, roleHeader);
        if (!"ADMINISTRATOR".equals(systemUser.getRole().trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only ADMINISTRATORs can trigger system backups");
        }

        logAction(systemUser, "BACKUP_TRIGGER", "System", null, "System backup triggered by administrator");

        Path backupsDir = Paths.get("data/backups");
        String tempDbZip = "data/temp_db_backup_" + UUID.randomUUID() + ".zip";
        try {
            Files.createDirectories(backupsDir);
            Files.deleteIfExists(Paths.get(tempDbZip));

            // Execute database snapshot backup safely inside H2
            jdbcTemplate.execute("BACKUP TO '" + tempDbZip + "'");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create database snapshot", e);
        }

        LocalDateTime now = timeProvider.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = "backup_" + timestamp + ".zip";
        Path finalBackupPath = backupsDir.resolve(backupFileName);

        long dbBackupSize = 0;
        long storageBackupSize = 0;

        try {
            Path tempDbPath = Paths.get(tempDbZip);
            if (Files.exists(tempDbPath)) {
                dbBackupSize = Files.size(tempDbPath);
            }

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(finalBackupPath))) {
                // Add DB backup inside zip
                if (Files.exists(tempDbPath)) {
                    ZipEntry dbEntry = new ZipEntry("db/database_backup.zip");
                    zos.putNextEntry(dbEntry);
                    Files.copy(tempDbPath, zos);
                    zos.closeEntry();
                }

                // Add data/storage files inside zip
                Path storagePath = Paths.get("data/storage");
                if (Files.exists(storagePath) && Files.isDirectory(storagePath)) {
                    try (Stream<Path> paths = Files.walk(storagePath)) {
                        List<Path> filesList = paths.filter(Files::isRegularFile).collect(Collectors.toList());
                        for (Path file : filesList) {
                            String relativePath = "storage/" + storagePath.relativize(file).toString();
                            ZipEntry fileEntry = new ZipEntry(relativePath);
                            zos.putNextEntry(fileEntry);
                            long fileSize = Files.size(file);
                            storageBackupSize += fileSize;
                            Files.copy(file, zos);
                            zos.closeEntry();
                        }
                    }
                }
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(finalBackupPath);
            } catch (IOException ignored) {}
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to package combined backup", e);
        } finally {
            try {
                Files.deleteIfExists(Paths.get(tempDbZip));
            } catch (IOException ignored) {}
        }

        long totalBackupSize = 0;
        try {
            if (Files.exists(finalBackupPath)) {
                totalBackupSize = Files.size(finalBackupPath);
            }
        } catch (IOException ignored) {}

        return new BackupResponse(
                "success",
                backupFileName,
                finalBackupPath.toString().replace('\\', '/'),
                dbBackupSize,
                storageBackupSize,
                totalBackupSize,
                now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    private void logAction(KbUser user, String action, String targetEntity, Long targetId, String details) {
        KbAuditLog log = new KbAuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public static class BackupResponse {
        private String status;
        private String backupFile;
        private String backupPath;
        private long databaseBackupSize;
        private long storageBackupSize;
        private long totalBackupSize;
        private String timestamp;

        public BackupResponse(String status, String backupFile, String backupPath,
                              long databaseBackupSize, long storageBackupSize,
                              long totalBackupSize, String timestamp) {
            this.status = status;
            this.backupFile = backupFile;
            this.backupPath = backupPath;
            this.databaseBackupSize = databaseBackupSize;
            this.storageBackupSize = storageBackupSize;
            this.totalBackupSize = totalBackupSize;
            this.timestamp = timestamp;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getBackupFile() { return backupFile; }
        public void setBackupFile(String backupFile) { this.backupFile = backupFile; }

        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }

        public long getDatabaseBackupSize() { return databaseBackupSize; }
        public void setDatabaseBackupSize(long databaseBackupSize) { this.databaseBackupSize = databaseBackupSize; }

        public long getStorageBackupSize() { return storageBackupSize; }
        public void setStorageBackupSize(long storageBackupSize) { this.storageBackupSize = storageBackupSize; }

        public long getTotalBackupSize() { return totalBackupSize; }
        public void setTotalBackupSize(long totalBackupSize) { this.totalBackupSize = totalBackupSize; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
