package com.eneik.generated.knowledgebase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/integration/documents")
public class KbDocumentController {

    private final KbDocumentRepository documentRepository;
    private final KbDocumentVersionRepository versionRepository;
    private final KbUserRepository userRepository;
    private final KbAuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest request;

    private static final String STORAGE_DIR = "data/storage";

    private static final List<Set<String>> SYNONYM_GROUPS = List.of(
        Set.of("фбун", "федеральное бюджетное учреждение науки", "цнии эпидемиологии", "цнии"),
        Set.of("гэк", "государственная экзаменационная комиссия"),
        Set.of("гиа", "государственная итоговая аттестация"),
        Set.of("фгос", "федеральный государственный образовательный стандарт", "образовательный стандарт")
    );

    public KbDocumentController(KbDocumentRepository documentRepository,
                                KbDocumentVersionRepository versionRepository,
                                KbUserRepository userRepository,
                                KbAuditLogRepository auditLogRepository) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    private KbUser getOrCreateSystemUser() {
        return userRepository.findByUsername("system_user")
            .orElseGet(() -> {
                KbUser user = new KbUser();
                user.setUsername("system_user");
                user.setRole("ADMINISTRATOR");
                return userRepository.save(user);
            });
    }

    private KbUser resolveUser(String usernameHeader, String roleHeader) {
        String username = null;
        String role = null;

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
            }
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

                if (!isJwtUsed && roleHeader != null && !roleHeader.trim().isEmpty() && !user.getRole().equalsIgnoreCase(roleHeader.trim())) {
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

    private boolean isStudent(KbUser user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String role = user.getRole().trim().toUpperCase();
        return "STUDENT".equals(role) || "ORDINATOR".equals(role) || "RESIDENT".equals(role) || "POSTGRADUATE".equals(role) || "LISTENER".equals(role);
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

    private List<String> expandSearchTerms(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedQuery = query.trim().toLowerCase();
        Set<String> terms = new LinkedHashSet<>();
        terms.add(normalizedQuery);

        // If query contains or is contained in any synonym in a group, expand with that entire group
        for (Set<String> group : SYNONYM_GROUPS) {
            boolean groupMatched = false;
            for (String synonym : group) {
                if (normalizedQuery.contains(synonym) || synonym.contains(normalizedQuery)) {
                    groupMatched = true;
                    break;
                }
            }
            if (groupMatched) {
                terms.addAll(group);
            }
        }

        // Also add individual words and expand them
        String[] parts = normalizedQuery.split("\\s+");
        for (String part : parts) {
            if (part.length() > 2) {
                terms.add(part);
                for (Set<String> group : SYNONYM_GROUPS) {
                    if (group.contains(part)) {
                        terms.addAll(group);
                    }
                }
            }
        }

        return new ArrayList<>(terms);
    }

    @GetMapping
    public List<DocumentResponse> searchDocuments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String educationLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);
        if (query != null && !query.trim().isEmpty()) {
            logAction(user, "SEARCH", "Search", null, query.trim());
        }

        List<KbDocument> docs = documentRepository.findAll();
        List<String> expandedTerms = expandSearchTerms(query);

        return docs.stream()
            .filter(doc -> {
                if (doc.getVersions() == null || doc.getVersions().isEmpty()) {
                    return false;
                }

                // Get latest version
                KbDocumentVersion latest = doc.getVersions().stream()
                    .max(Comparator.comparing(KbDocumentVersion::getVersionNumber))
                    .orElse(null);

                if (latest == null) {
                    return false;
                }

                // Filter by documentType
                if (documentType != null && !documentType.trim().isEmpty()) {
                    String ext = latest.getFileType();
                    if (ext == null || !ext.equalsIgnoreCase(documentType.trim())) {
                        return false;
                    }
                }

                // Filter by updatedAfter
                if (updatedAfter != null && doc.getUpdatedAt().isBefore(updatedAfter)) {
                    return false;
                }

                // Filter by specialty (matching tags or category case-insensitively)
                if (specialty != null && !specialty.trim().isEmpty()) {
                    String cleanSpec = specialty.trim().toLowerCase();
                    boolean tagMatches = doc.getTags().stream().anyMatch(t -> t.toLowerCase().contains(cleanSpec));
                    boolean catMatches = doc.getCategory() != null && doc.getCategory().toLowerCase().contains(cleanSpec);
                    if (!tagMatches && !catMatches) {
                        return false;
                    }
                }

                // Filter by educationLevel (matching tags or category case-insensitively)
                if (educationLevel != null && !educationLevel.trim().isEmpty()) {
                    String cleanEd = educationLevel.trim().toLowerCase();
                    boolean tagMatches = doc.getTags().stream().anyMatch(t -> t.toLowerCase().contains(cleanEd));
                    boolean catMatches = doc.getCategory() != null && doc.getCategory().toLowerCase().contains(cleanEd);
                    if (!tagMatches && !catMatches) {
                        return false;
                    }
                }

                // Filter by full-text search query (expanded terms)
                if (!expandedTerms.isEmpty()) {
                    boolean queryMatches = false;
                    String titleLower = doc.getTitle().toLowerCase();
                    String catLower = doc.getCategory() != null ? doc.getCategory().toLowerCase() : "";
                    String contentLower = latest.getIndexedContent() != null ? latest.getIndexedContent().toLowerCase() : "";

                    for (String term : expandedTerms) {
                        if (titleLower.contains(term) || catLower.contains(term) || contentLower.contains(term)) {
                            queryMatches = true;
                            break;
                        }
                        // Also check tags
                        boolean tagMatchesTerm = doc.getTags().stream().anyMatch(t -> t.toLowerCase().contains(term));
                        if (tagMatchesTerm) {
                            queryMatches = true;
                            break;
                        }
                    }

                    if (!queryMatches) {
                        return false;
                    }
                }

                return true;
            })
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        KbUser systemUser = resolveUser(usernameHeader, roleHeader);
        if (isStudent(systemUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Students are not authorized to upload documents");
        }

        // 1. Create document
        KbDocument doc = new KbDocument();
        doc.setTitle(title.trim());
        doc.setCategory(category != null ? category.trim() : null);
        if (tags != null) {
            doc.setTags(new HashSet<>(tags));
        }
        doc.setAuthor(systemUser);
        doc = documentRepository.save(doc);

        // 2. Save file and extract text
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "unnamed.bin";
        } else {
            originalFilename = new File(originalFilename).getName();
        }
        String fileType = getFileExtension(originalFilename);

        try {
            // Ensure directory exists
            Files.createDirectories(Paths.get(STORAGE_DIR));

            String uniqueFilename = "doc_" + doc.getId() + "_v1_" + originalFilename;
            Path filePath = Paths.get(STORAGE_DIR, uniqueFilename);
            Files.write(filePath, file.getBytes());

            String extractedContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 3. Create document version
            KbDocumentVersion version = new KbDocumentVersion();
            version.setDocument(doc);
            version.setVersionNumber(1);
            version.setFilePath("/api/v1/integration/documents/download/" + doc.getId() + "/version/1");
            version.setFileType(fileType);
            version.setIndexedContent(extractedContent);
            version.setCreatedBy(systemUser);

            versionRepository.save(version);

            // Sync doc versions
            doc.setVersions(new ArrayList<>(Collections.singletonList(version)));
            doc = documentRepository.save(doc);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store or process file", e);
        }

        logAction(systemUser, "DOCUMENT_CREATE", "KbDocument", doc.getId(), "Document uploaded: " + title);

        return mapToResponse(doc);
    }

    @GetMapping("/{id}")
    public DocumentResponse getDocumentById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        KbDocument doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        KbUser systemUser = resolveUser(usernameHeader, roleHeader);
        logAction(systemUser, "VIEW", "KbDocument", id, doc.getTitle());

        return mapToResponse(doc);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponse updateDocument(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser systemUser = resolveUser(usernameHeader, roleHeader);
        if (isStudent(systemUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Students are not authorized to edit documents");
        }

        KbDocument doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (title != null && !title.trim().isEmpty()) {
            doc.setTitle(title.trim());
        }
        if (category != null) {
            doc.setCategory(category.trim());
        }
        if (tags != null) {
            doc.getTags().clear();
            doc.getTags().addAll(tags);
        }

        if (file != null && !file.isEmpty()) {
            int newVersionNum = doc.getVersions().stream()
                .mapToInt(KbDocumentVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                originalFilename = "unnamed.bin";
            } else {
                originalFilename = new File(originalFilename).getName();
            }
            String fileType = getFileExtension(originalFilename);

            try {
                Files.createDirectories(Paths.get(STORAGE_DIR));

                String uniqueFilename = "doc_" + doc.getId() + "_v" + newVersionNum + "_" + originalFilename;
                Path filePath = Paths.get(STORAGE_DIR, uniqueFilename);
                Files.write(filePath, file.getBytes());

                String extractedContent = new String(file.getBytes(), StandardCharsets.UTF_8);

                KbDocumentVersion version = new KbDocumentVersion();
                version.setDocument(doc);
                version.setVersionNumber(newVersionNum);
                version.setFilePath("/api/v1/integration/documents/download/" + doc.getId() + "/version/" + newVersionNum);
                version.setFileType(fileType);
                version.setIndexedContent(extractedContent);
                version.setCreatedBy(systemUser);

                versionRepository.save(version);

                doc.getVersions().add(version);

            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store or process updated file", e);
            }
        }

        doc = documentRepository.save(doc);

        logAction(systemUser, "DOCUMENT_UPDATE", "KbDocument", doc.getId(), "Document updated: " + doc.getTitle());

        return mapToResponse(doc);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        KbUser systemUser = resolveUser(usernameHeader, roleHeader);
        if (isStudent(systemUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Students are not authorized to delete documents");
        }

        KbDocument doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        documentRepository.delete(doc);

        logAction(systemUser, "DOCUMENT_DELETE", "KbDocument", id, doc.getTitle());
    }

    @GetMapping("/download/{id}/version/{versionNumber}")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long id,
            @PathVariable Integer versionNumber,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        KbDocument doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        KbDocumentVersion version = doc.getVersions().stream()
            .filter(v -> v.getVersionNumber().equals(versionNumber))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        KbUser systemUser = resolveUser(usernameHeader, roleHeader);
        logAction(systemUser, "DOWNLOAD", "KbDocument", id, doc.getTitle() + " (v" + versionNumber + ")");

        // Try to load actual file from storage
        String originalFilename = version.getFilePath().substring(version.getFilePath().lastIndexOf('/') + 1);
        String uniqueFilename = "doc_" + id + "_v" + versionNumber + "_" + originalFilename;
        Path path = Paths.get(STORAGE_DIR, uniqueFilename);

        // Fallback or find file
        File storageFolder = new File(STORAGE_DIR);
        File targetFile = null;
        if (storageFolder.exists() && storageFolder.isDirectory()) {
            File[] files = storageFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith("doc_" + id + "_v" + versionNumber + "_")) {
                        targetFile = f;
                        break;
                    }
                }
            }
        }

        byte[] content;
        try {
            if (targetFile != null && targetFile.exists()) {
                content = Files.readAllBytes(targetFile.toPath());
            } else if (version.getIndexedContent() != null) {
                content = version.getIndexedContent().getBytes(StandardCharsets.UTF_8);
            } else {
                content = "Empty file content".getBytes(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            content = "Error reading file content".getBytes(StandardCharsets.UTF_8);
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content);
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "bin";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    private DocumentResponse mapToResponse(KbDocument doc) {
        KbDocumentVersion latest = doc.getVersions().stream()
            .max(Comparator.comparing(KbDocumentVersion::getVersionNumber))
            .orElse(null);

        DocumentResponse resp = new DocumentResponse();
        resp.setId(doc.getId());
        resp.setTitle(doc.getTitle());
        resp.setCategory(doc.getCategory());
        resp.setTags(new ArrayList<>(doc.getTags()));
        resp.setAuthorId(doc.getAuthor().getId());

        if (latest != null) {
            resp.setVersionNumber(latest.getVersionNumber());
            resp.setFileType(latest.getFileType());
            resp.setFilePath(latest.getFilePath());
        }

        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        resp.setCreatedAt(doc.getCreatedAt().format(dtf));
        resp.setUpdatedAt(doc.getUpdatedAt().format(dtf));

        return resp;
    }

    public static class DocumentResponse {
        private Long id;
        private String title;
        private String category;
        private List<String> tags;
        private Long authorId;
        private Integer versionNumber;
        private String fileType;
        private String filePath;
        private String createdAt;
        private String updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }

        public Integer getVersionNumber() { return versionNumber; }
        public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}
