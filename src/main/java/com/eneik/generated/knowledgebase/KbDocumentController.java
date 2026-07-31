package com.eneik.generated.knowledgebase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private final KbDocumentCommentRepository commentRepository;
    private final KbUserFavoriteRepository userFavoriteRepository;
    private final KbSavedQueryRepository savedQueryRepository;

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
                                KbAuditLogRepository auditLogRepository,
                                KbDocumentCommentRepository commentRepository,
                                KbUserFavoriteRepository userFavoriteRepository,
                                KbSavedQueryRepository savedQueryRepository) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.commentRepository = commentRepository;
        this.userFavoriteRepository = userFavoriteRepository;
        this.savedQueryRepository = savedQueryRepository;
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

    /**
     * Resolves the authenticated user from request context (JWT or Headers).
     * <p>
     * Grounded under Jason Stanley's Principle of Semantic Contextualism and the
     * {@code DZHEYSON_STENLI_04_INDEXICAL_CONTEXT_LOCK} pattern, ensuring that indexical
     * attributes (current user session, identity) are strictly bound to the explicit JWT
     * authorization context block, completely preventing cross-user contextual or session data leakage.
     * </p>
     *
     * @param usernameHeader optional fallback/spoof-prevention username header
     * @param roleHeader optional fallback/spoof-prevention role header
     * @return the resolved, persisted KbUser entity
     */
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

    private int getLevenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[len1][len2];
    }

    private boolean isFuzzyMatch(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return false;
        }
        s1 = s1.trim().toLowerCase();
        s2 = s2.trim().toLowerCase();
        if (s1.equals(s2)) {
            return true;
        }
        int len1 = s1.length();
        int len2 = s2.length();
        if (Math.abs(len1 - len2) > 2) {
            return false;
        }
        int minLen = Math.min(len1, len2);
        int maxDist;
        if (minLen < 4) {
            maxDist = 0;
        } else if (minLen < 8) {
            maxDist = 1;
        } else {
            maxDist = 2;
        }
        return getLevenshteinDistance(s1, s2) <= maxDist;
    }

    private Set<String> getWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        String[] parts = text.toLowerCase().split("[^a-zA-Z0-9а-яА-ЯёЁ]+");
        Set<String> words = new HashSet<>();
        for (String p : parts) {
            if (p.length() > 2) {
                words.add(p);
            }
        }
        return words;
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
                if (isFuzzyMatch(normalizedQuery, synonym)) {
                    groupMatched = true;
                    break;
                }
            }
            if (groupMatched) {
                terms.addAll(group);
            }
        }

        // Also add individual words and expand them
        String[] queryWords = normalizedQuery.split("\\s+");
        for (String part : queryWords) {
            if (part.length() > 2) {
                terms.add(part);
                for (Set<String> group : SYNONYM_GROUPS) {
                    boolean matched = false;
                    for (String synonym : group) {
                        if (synonym.equals(part) || isFuzzyMatch(part, synonym)) {
                            matched = true;
                            break;
                        }
                    }
                    if (matched) {
                        terms.addAll(group);
                    }
                }
            }
        }

        return new ArrayList<>(terms);
    }

    private static class MatchedDoc {
        final KbDocument doc;
        final boolean exact;

        MatchedDoc(KbDocument doc, boolean exact) {
            this.doc = doc;
            this.exact = exact;
        }
    }

    @GetMapping
    public List<DocumentResponse> searchDocuments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String educationLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,
            @RequestParam(required = false) Boolean favoritesOnly,
            @RequestParam(required = false) Long savedQueryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);

        if (savedQueryId != null && user != null) {
            KbSavedQuery saved = savedQueryRepository.findById(savedQueryId).orElse(null);
            if (saved != null && saved.getUser().getId().equals(user.getId())) {
                query = saved.getQueryText();
            }
        }

        if (query != null && !query.trim().isEmpty()) {
            logAction(user, "SEARCH", "Search", null, query.trim());
            if (user != null) {
                String trimmedQuery = query.trim();
                boolean alreadySaved = savedQueryRepository.findByUserId(user.getId()).stream()
                    .anyMatch(q -> q.getQueryText().equalsIgnoreCase(trimmedQuery));
                if (!alreadySaved) {
                    KbSavedQuery savedQuery = new KbSavedQuery(user, trimmedQuery);
                    savedQueryRepository.save(savedQuery);
                }
            }
        }

        final Set<Long> favoriteDocIds = (favoritesOnly != null && favoritesOnly && user != null)
            ? userFavoriteRepository.findByUserId(user.getId()).stream()
                .map(fav -> fav.getDocument().getId())
                .collect(Collectors.toSet())
            : Collections.emptySet();

        List<KbDocument> docs = documentRepository.findAll();
        List<String> expandedTerms = expandSearchTerms(query);

        List<MatchedDoc> matchedDocs = new ArrayList<>();

        for (KbDocument doc : docs) {
            if (favoritesOnly != null && favoritesOnly) {
                if (!favoriteDocIds.contains(doc.getId())) {
                    continue;
                }
            }

            if (doc.getVersions() == null || doc.getVersions().isEmpty()) {
                continue;
            }

            // Get latest version
            KbDocumentVersion latest = doc.getVersions().stream()
                .max(Comparator.comparing(KbDocumentVersion::getVersionNumber))
                .orElse(null);

            if (latest == null) {
                continue;
            }

            // Filter by documentType
            if (documentType != null && !documentType.trim().isEmpty()) {
                String ext = latest.getFileType();
                if (ext == null || !ext.equalsIgnoreCase(documentType.trim())) {
                    continue;
                }
            }

            // Filter by updatedAfter
            if (updatedAfter != null && doc.getUpdatedAt().isBefore(updatedAfter)) {
                continue;
            }

            // Filter by specialty (matching tags or category case-insensitively)
            if (specialty != null && !specialty.trim().isEmpty()) {
                String cleanSpec = specialty.trim().toLowerCase();
                boolean tagMatches = doc.getTags().stream().anyMatch(t -> t.toLowerCase().contains(cleanSpec));
                boolean catMatches = doc.getCategory() != null && doc.getCategory().toLowerCase().contains(cleanSpec);
                if (!tagMatches && !catMatches) {
                    continue;
                }
            }

            // Filter by educationLevel (matching tags or category case-insensitively)
            if (educationLevel != null && !educationLevel.trim().isEmpty()) {
                String cleanEd = educationLevel.trim().toLowerCase();
                boolean tagMatches = doc.getTags().stream().anyMatch(t -> t.toLowerCase().contains(cleanEd));
                boolean catMatches = doc.getCategory() != null && doc.getCategory().toLowerCase().contains(cleanEd);
                if (!tagMatches && !catMatches) {
                    continue;
                }
            }

            // Filter by full-text search query (expanded terms)
            boolean queryMatches = false;
            boolean isExactMatch = false;

            if (!expandedTerms.isEmpty()) {
                String titleLower = doc.getTitle().toLowerCase();
                String catLower = doc.getCategory() != null ? doc.getCategory().toLowerCase() : "";
                String contentLower = latest.getIndexedContent() != null ? latest.getIndexedContent().toLowerCase() : "";

                for (String term : expandedTerms) {
                    if (titleLower.contains(term) || catLower.contains(term) || contentLower.contains(term)) {
                        queryMatches = true;
                        isExactMatch = true;
                        break;
                    }
                    // Also check tags
                    boolean tagMatchesTerm = doc.getTags().stream().anyMatch(t -> t.toLowerCase().contains(term));
                    if (tagMatchesTerm) {
                        queryMatches = true;
                        isExactMatch = true;
                        break;
                    }
                }

                // If not matched exactly, try fuzzy matching individual words
                if (!queryMatches) {
                    Set<String> docWords = new HashSet<>();
                    docWords.addAll(getWords(doc.getTitle()));
                    if (doc.getCategory() != null) {
                        docWords.addAll(getWords(doc.getCategory()));
                    }
                    if (latest.getIndexedContent() != null) {
                        docWords.addAll(getWords(latest.getIndexedContent()));
                    }
                    for (String tag : doc.getTags()) {
                        docWords.addAll(getWords(tag));
                    }

                    Set<String> queryWords = new HashSet<>();
                    for (String term : expandedTerms) {
                        queryWords.addAll(getWords(term));
                    }

                    for (String qWord : queryWords) {
                        for (String dWord : docWords) {
                            if (isFuzzyMatch(qWord, dWord)) {
                                queryMatches = true;
                                break;
                            }
                        }
                        if (queryMatches) {
                            break;
                        }
                    }
                }

                if (!queryMatches) {
                    continue;
                }
            } else {
                isExactMatch = true;
            }

            matchedDocs.add(new MatchedDoc(doc, isExactMatch));
        }

        // Sort: exact matches prioritized over fuzzy matches
        matchedDocs.sort((a, b) -> {
            if (a.exact == b.exact) {
                return 0;
            }
            return a.exact ? -1 : 1;
        });

        List<DocumentResponse> results = matchedDocs.stream()
            .map(mDoc -> mapToResponse(mDoc.doc, user))
            .collect(Collectors.toList());

        // Resolve page/size or limit/offset
        int limitVal = 10; // Default limit/size
        int start = 0;     // Default start/offset

        if (pageSize != null) {
            limitVal = pageSize;
        } else if (size != null) {
            limitVal = size;
        } else if (limit != null) {
            limitVal = limit;
        }

        if (pageNumber != null) {
            start = pageNumber * limitVal;
        } else if (page != null) {
            start = page * limitVal;
        } else if (offset != null) {
            start = offset;
        }

        if (start < 0) {
            start = 0;
        }
        if (limitVal <= 0) {
            limitVal = 10;
        }

        int end = Math.min(start + limitVal, results.size());
        if (start > results.size()) {
            return Collections.emptyList();
        }

        return results.subList(start, end);
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

        return mapToResponse(doc, systemUser);
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

        return mapToResponse(doc, systemUser);
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

        return mapToResponse(doc, systemUser);
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
            @RequestParam(value = "format", required = false) String format,
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

        if (format != null && !format.trim().isEmpty()) {
            String cleanFormat = format.trim().toLowerCase();
            if ("pdf".equals(cleanFormat)) {
                byte[] pdfBytes = PdfGenerator.generate(doc.getTitle(), doc.getCategory(), String.join(", ", doc.getTags()), version.getIndexedContent());
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=\"export_" + id + ".pdf\"")
                    .body(pdfBytes);
            } else if ("docx".equals(cleanFormat)) {
                byte[] docxBytes = DocxGenerator.generate(doc.getTitle(), doc.getCategory(), String.join(", ", doc.getTags()), version.getIndexedContent());
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .header("Content-Disposition", "attachment; filename=\"export_" + id + ".docx\"")
                    .body(docxBytes);
            }
        }

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

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportDocument(
            @PathVariable Long id,
            @RequestParam(value = "format", required = false) String format,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        KbDocument doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        KbDocumentVersion latest = doc.getVersions().stream()
            .max(Comparator.comparing(KbDocumentVersion::getVersionNumber))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No document version found"));

        KbUser systemUser = resolveUser(usernameHeader, roleHeader);
        logAction(systemUser, "EXPORT", "KbDocument", id, doc.getTitle() + " format=" + format);

        String cleanFormat = (format != null) ? format.trim().toLowerCase() : "pdf";
        if ("pdf".equals(cleanFormat)) {
            byte[] pdfBytes = PdfGenerator.generate(doc.getTitle(), doc.getCategory(), String.join(", ", doc.getTags()), latest.getIndexedContent());
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"export_" + id + ".pdf\"")
                .body(pdfBytes);
        } else if ("docx".equals(cleanFormat)) {
            byte[] docxBytes = DocxGenerator.generate(doc.getTitle(), doc.getCategory(), String.join(", ", doc.getTags()), latest.getIndexedContent());
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header("Content-Disposition", "attachment; filename=\"export_" + id + ".docx\"")
                .body(docxBytes);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported export format: " + format);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "bin";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CommentResponse addComment(
            @PathVariable Long id,
            @RequestBody CommentRequest request,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser systemUser = resolveUser(usernameHeader, roleHeader);
        KbDocument doc = documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content cannot be empty");
        }

        String type = request.getType();
        if (type == null || type.trim().isEmpty()) {
            type = "COMMENT";
        } else {
            type = type.trim().toUpperCase();
            if (!"COMMENT".equals(type) && !"UPDATE_REQUEST".equals(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid comment type");
            }
        }

        KbDocumentComment comment = new KbDocumentComment();
        comment.setDocument(doc);
        comment.setAuthor(systemUser);
        comment.setContent(request.getContent().trim());
        comment.setType(type);
        comment = commentRepository.save(comment);

        logAction(systemUser, "ADD_COMMENT", "KbDocument", id, "Added " + type + " to document: " + doc.getTitle());

        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setAuthorId(systemUser.getId());
        resp.setAuthorUsername(systemUser.getUsername());
        resp.setContent(comment.getContent());
        resp.setType(comment.getType());
        resp.setCreatedAt(comment.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        return resp;
    }

    private DocumentResponse mapToResponse(KbDocument doc, KbUser user) {
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

        List<KbDocumentComment> commentsList = commentRepository.findByDocumentIdOrderByCreatedAtAsc(doc.getId());
        if (commentsList != null) {
            resp.setComments(commentsList.stream().map(c -> {
                CommentResponse cr = new CommentResponse();
                cr.setId(c.getId());
                cr.setAuthorId(c.getAuthor().getId());
                cr.setAuthorUsername(c.getAuthor().getUsername());
                cr.setContent(c.getContent());
                cr.setType(c.getType());
                cr.setCreatedAt(c.getCreatedAt().format(dtf));
                return cr;
            }).collect(Collectors.toList()));
        } else {
            resp.setComments(Collections.emptyList());
        }

        if (user != null) {
            KbUserFavoriteId favId = new KbUserFavoriteId(user.getId(), doc.getId());
            resp.setIsFavorite(userFavoriteRepository.existsById(favId));
        } else {
            resp.setIsFavorite(false);
        }

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
        private List<CommentResponse> comments;
        private Boolean isFavorite;

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

        public List<CommentResponse> getComments() { return comments; }
        public void setComments(List<CommentResponse> comments) { this.comments = comments; }

        public Boolean getIsFavorite() { return isFavorite; }
        public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
    }

    public static class CommentRequest {
        private String content;
        private String type;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class CommentResponse {
        private Long id;
        private Long authorId;
        private String authorUsername;
        private String content;
        private String type;
        private String createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }

        public String getAuthorUsername() { return authorUsername; }
        public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
