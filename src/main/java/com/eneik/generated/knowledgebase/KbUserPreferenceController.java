package com.eneik.generated.knowledgebase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/integration/preferences")
public class KbUserPreferenceController {

    private final KbUserRepository userRepository;
    private final KbDocumentRepository documentRepository;
    private final KbUserFavoriteRepository userFavoriteRepository;
    private final KbSavedQueryRepository savedQueryRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest request;

    public KbUserPreferenceController(KbUserRepository userRepository,
                                      KbDocumentRepository documentRepository,
                                      KbUserFavoriteRepository userFavoriteRepository,
                                      KbSavedQueryRepository savedQueryRepository) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.userFavoriteRepository = userFavoriteRepository;
        this.savedQueryRepository = savedQueryRepository;
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

    // --- FAVORITES ---

    @PostMapping("/favorites")
    public ResponseEntity<FavoriteDocumentResponse> addFavorite(
            @RequestParam Long documentId,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);
        KbDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        KbUserFavoriteId favoriteId = new KbUserFavoriteId(user.getId(), document.getId());
        KbUserFavorite favorite;
        if (userFavoriteRepository.existsById(favoriteId)) {
            favorite = userFavoriteRepository.findById(favoriteId).orElseThrow();
        } else {
            favorite = new KbUserFavorite(user, document);
            favorite = userFavoriteRepository.save(favorite);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToFavoriteResponse(favorite));
    }

    @DeleteMapping("/favorites/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(
            @PathVariable Long documentId,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);
        KbUserFavoriteId favoriteId = new KbUserFavoriteId(user.getId(), documentId);

        if (userFavoriteRepository.existsById(favoriteId)) {
            userFavoriteRepository.deleteById(favoriteId);
        } else {
            if (!documentRepository.existsById(documentId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
            }
        }
    }

    @GetMapping("/favorites")
    public List<FavoriteDocumentResponse> getFavorites(
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);
        List<KbUserFavorite> favorites = userFavoriteRepository.findByUserId(user.getId());

        return favorites.stream()
                .map(this::mapToFavoriteResponse)
                .collect(Collectors.toList());
    }

    // --- SAVED QUERIES ---

    @PostMapping("/saved-queries")
    public ResponseEntity<SavedQueryResponse> saveQuery(
            @RequestParam String query,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        if (query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query string cannot be empty");
        }

        KbUser user = resolveUser(usernameHeader, roleHeader);
        String trimmedQuery = query.trim();

        KbSavedQuery savedQuery;
        List<KbSavedQuery> existing = savedQueryRepository.findByUserId(user.getId());
        KbSavedQuery duplicate = existing.stream()
                .filter(q -> q.getQueryText().equalsIgnoreCase(trimmedQuery))
                .findFirst()
                .orElse(null);

        if (duplicate != null) {
            savedQuery = duplicate;
        } else {
            savedQuery = new KbSavedQuery(user, trimmedQuery);
            savedQuery = savedQueryRepository.save(savedQuery);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToQueryResponse(savedQuery));
    }

    @DeleteMapping("/saved-queries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSavedQuery(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);
        KbSavedQuery savedQuery = savedQueryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved query not found"));

        if (!savedQuery.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Cannot delete other user's saved query");
        }

        savedQueryRepository.delete(savedQuery);
    }

    @GetMapping("/saved-queries")
    public List<SavedQueryResponse> getSavedQueries(
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);
        List<KbSavedQuery> queries = savedQueryRepository.findByUserId(user.getId());

        return queries.stream()
                .map(this::mapToQueryResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/profile")
    public UserProfileResponse getUserProfile(
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);
        List<KbSavedQuery> queries = savedQueryRepository.findByUserId(user.getId());
        List<SavedQueryResponse> queryResponses = queries.stream()
                .map(this::mapToQueryResponse)
                .collect(Collectors.toList());

        List<KbUserFavorite> favorites = userFavoriteRepository.findByUserId(user.getId());
        List<FavoriteDocumentResponse> favoriteResponses = favorites.stream()
                .map(this::mapToFavoriteResponse)
                .collect(Collectors.toList());

        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRole(user.getRole());
        resp.setFavorites(favoriteResponses);
        resp.setSavedQueries(queryResponses);
        return resp;
    }

    private FavoriteDocumentResponse mapToFavoriteResponse(KbUserFavorite fav) {
        KbDocument doc = fav.getDocument();
        KbDocumentVersion latest = doc.getVersions() != null ? doc.getVersions().stream()
                .max(Comparator.comparing(KbDocumentVersion::getVersionNumber))
                .orElse(null) : null;

        FavoriteDocumentResponse resp = new FavoriteDocumentResponse();
        resp.setDocumentId(doc.getId());
        resp.setTitle(doc.getTitle());
        resp.setCategory(doc.getCategory());
        resp.setTags(doc.getTags() != null ? List.copyOf(doc.getTags()) : List.of());
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

    private SavedQueryResponse mapToQueryResponse(KbSavedQuery q) {
        SavedQueryResponse resp = new SavedQueryResponse();
        resp.setId(q.getId());
        resp.setQueryText(q.getQueryText());
        resp.setCreatedAt(q.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        return resp;
    }

    // --- RESPONSE DTOS ---

    public static class FavoriteDocumentResponse {
        private Long documentId;
        private String title;
        private String category;
        private List<String> tags;
        private Long authorId;
        private Integer versionNumber;
        private String fileType;
        private String filePath;
        private String createdAt;
        private String updatedAt;

        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }

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

    public static class SavedQueryResponse {
        private Long id;
        private String queryText;
        private String createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getQueryText() { return queryText; }
        public void setQueryText(String queryText) { this.queryText = queryText; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String role;
        private List<FavoriteDocumentResponse> favorites;
        private List<SavedQueryResponse> savedQueries;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public List<FavoriteDocumentResponse> getFavorites() { return favorites; }
        public void setFavorites(List<FavoriteDocumentResponse> favorites) { this.favorites = favorites; }

        public List<SavedQueryResponse> getSavedQueries() { return savedQueries; }
        public void setSavedQueries(List<SavedQueryResponse> savedQueries) { this.savedQueries = savedQueries; }
    }
}
