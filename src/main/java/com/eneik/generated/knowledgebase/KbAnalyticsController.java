package com.eneik.generated.knowledgebase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.eneik.generated.integration.EiosClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/integration/analytics")
public class KbAnalyticsController {

    private final KbAuditLogRepository auditLogRepository;
    private final KbDocumentRepository documentRepository;

    @Autowired
    private KbUserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest request;

    @Autowired
    private EiosClient eiosClient;

    public KbAnalyticsController(KbAuditLogRepository auditLogRepository, KbDocumentRepository documentRepository) {
        this.auditLogRepository = auditLogRepository;
        this.documentRepository = documentRepository;
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

        if (!hasValidJwt) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Valid JWT is missing");
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

    private String transliterate(String message) {
        if (message == null) {
            return "";
        }
        char[] abc = {'а','б','в','г','д','е','ё','ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х','ц','ч','ш','щ','ъ','ы','ь','э','ю','я'};
        String[] lat = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch","'","y","'","e","yu","ya"};
        char[] abcCap = {'А','Б','В','Г','Д','Е','Ё','Ж','З','И','Й','К','Л','М','Н','О','П','Р','С','Т','У','Ф','Х','Ц','Ч','Ш','Щ','Ъ','Ы','Ь','Э','Ю','Я'};
        String[] latCap = {"A","B","V","G","D","E","E","Zh","Z","I","Y","K","L","M","N","O","P","R","S","T","U","F","H","Ts","Ch","Sh","Sch","'","Y","'","E","Yu","Ya"};

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            boolean replaced = false;
            for (int j = 0; j < abc.length; j++) {
                if (ch == abc[j]) {
                    sb.append(lat[j]);
                    replaced = true;
                    break;
                } else if (ch == abcCap[j]) {
                    sb.append(latCap[j]);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    @GetMapping("/statistics")
    public AnalyticsStatistics getAnalyticsStatistics() {
        Pageable topTen = PageRequest.of(0, 10);

        // 1. Calculate popularSearches
        List<KbAuditLogRepository.SearchQueryProjection> searches = auditLogRepository.findPopularSearches(topTen);
        List<SearchQueryStats> popularSearches = searches.stream()
            .map(p -> new SearchQueryStats(p.getQuery(), p.getCount().intValue()))
            .collect(Collectors.toList());

        // 2. Calculate topViewedDocuments
        List<KbAuditLogRepository.DocumentStatsProjection> viewed = auditLogRepository.findTopViewedDocuments(topTen);

        // 3. Calculate topDownloadedDocuments
        List<KbAuditLogRepository.DocumentStatsProjection> downloaded = auditLogRepository.findTopDownloadedDocuments(topTen);

        // Fetch titles only for the needed document IDs
        Set<Long> neededDocIds = new HashSet<>();
        for (KbAuditLogRepository.DocumentStatsProjection p : viewed) {
            neededDocIds.add(p.getTargetId());
        }
        for (KbAuditLogRepository.DocumentStatsProjection p : downloaded) {
            neededDocIds.add(p.getTargetId());
        }

        Map<Long, String> docTitles = new HashMap<>();
        if (!neededDocIds.isEmpty()) {
            documentRepository.findAllById(neededDocIds).forEach(doc -> {
                docTitles.put(doc.getId(), doc.getTitle());
            });
        }

        List<DocumentStats> topViewedDocuments = viewed.stream()
            .map(p -> {
                Long docId = p.getTargetId();
                String title = docTitles.get(docId);
                if (title == null) {
                    List<String> details = auditLogRepository.findFirstDetailsByTargetId(docId, PageRequest.of(0, 1));
                    title = details.isEmpty() ? "Unknown Document" : details.get(0);
                }
                return new DocumentStats(docId, title, p.getCount().intValue());
            })
            .collect(Collectors.toList());

        List<DocumentStats> topDownloadedDocuments = downloaded.stream()
            .map(p -> {
                Long docId = p.getTargetId();
                String title = docTitles.get(docId);
                if (title == null) {
                    List<String> details = auditLogRepository.findFirstDetailsByTargetId(docId, PageRequest.of(0, 1));
                    String loggedTitle = details.isEmpty() ? "Unknown Document" : details.get(0);
                    if (loggedTitle.contains(" (v")) {
                        loggedTitle = loggedTitle.substring(0, loggedTitle.lastIndexOf(" (v"));
                    }
                    title = loggedTitle;
                }
                return new DocumentStats(docId, title, p.getCount().intValue());
            })
            .collect(Collectors.toList());

        return new AnalyticsStatistics(popularSearches, topViewedDocuments, topDownloadedDocuments);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAnalytics(
            @RequestParam(value = "format", defaultValue = "csv") String format,
            @RequestHeader(value = "X-User-Name", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {

        KbUser user = resolveUser(usernameHeader, roleHeader);
        if (isStudent(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only administrators can export analytics");
        }

        AnalyticsStatistics stats = getAnalyticsStatistics();
        String cleanFormat = (format != null) ? format.trim().toLowerCase() : "csv";

        if ("csv".equals(cleanFormat)) {
            byte[] csvBytes = generateCsvReport(stats);
            logAction(user, "EXPORT_ANALYTICS", "Analytics", null, "Exported as CSV");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header("Content-Disposition", "attachment; filename=\"analytics_report.csv\"")
                    .body(csvBytes);
        } else if ("pdf".equals(cleanFormat)) {
            byte[] pdfBytes = generatePdfReport(stats);
            logAction(user, "EXPORT_ANALYTICS", "Analytics", null, "Exported as PDF");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=\"analytics_report.pdf\"")
                    .body(pdfBytes);
        } else if ("eios".equals(cleanFormat)) {
            byte[] csvBytes = generateCsvReport(stats);
            String csvString = new String(csvBytes, StandardCharsets.UTF_8);
            eiosClient.syncAnalytics(csvString);
            logAction(user, "EIOS_SYNC_ANALYTICS", "Analytics", null, "Synchronized to EIOS");

            String jsonResponse = "{\"message\":\"Analytics synchronized to EIOS successfully\"}";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResponse.getBytes(StandardCharsets.UTF_8));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: " + format);
        }
    }

    private byte[] generateCsvReport(AnalyticsStatistics stats) {
        StringBuilder sb = new StringBuilder();

        sb.append("Popular Searches\n");
        sb.append("Query,Count\n");
        if (stats.getPopularSearches() != null) {
            for (SearchQueryStats q : stats.getPopularSearches()) {
                sb.append(escapeCsv(q.getQuery())).append(",").append(q.getCount()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("Top Viewed Documents\n");
        sb.append("Document ID,Title,Count\n");
        if (stats.getTopViewedDocuments() != null) {
            for (DocumentStats d : stats.getTopViewedDocuments()) {
                sb.append(d.getDocumentId()).append(",")
                  .append(escapeCsv(d.getTitle())).append(",")
                  .append(d.getCount()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("Top Downloaded Documents\n");
        sb.append("Document ID,Title,Count\n");
        if (stats.getTopDownloadedDocuments() != null) {
            for (DocumentStats d : stats.getTopDownloadedDocuments()) {
                sb.append(d.getDocumentId()).append(",")
                  .append(escapeCsv(d.getTitle())).append(",")
                  .append(d.getCount()).append("\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String val) {
        if (val == null) {
            return "";
        }
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private byte[] generatePdfReport(AnalyticsStatistics stats) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            List<Long> offsets = new ArrayList<>();

            // Header
            bos.write("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));

            // Object 1: Catalog
            offsets.add((long) bos.size());
            bos.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Object 2: Pages
            offsets.add((long) bos.size());
            bos.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Object 3: Page
            offsets.add((long) bos.size());
            bos.write("3 0 obj\n<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 612 792] /Contents 5 0 R >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Object 4: Font
            offsets.add((long) bos.size());
            bos.write("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Prepare text stream
            ByteArrayOutputStream textStream = new ByteArrayOutputStream();
            textStream.write("BT\n/F1 12 Tf\n50 720 Td\n14 TL\n".getBytes(StandardCharsets.UTF_8));

            writePdfLine(textStream, "Knowledge Base Analytics Report");
            writePdfLine(textStream, "Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writePdfLine(textStream, "==================================================");
            writePdfLine(textStream, "");

            writePdfLine(textStream, "Popular Searches:");
            writePdfLine(textStream, "--------------------------------------------------");
            if (stats.getPopularSearches() != null) {
                for (SearchQueryStats q : stats.getPopularSearches()) {
                    writePdfLine(textStream, "  - " + transliterate(q.getQuery()) + ": " + q.getCount());
                }
            }
            writePdfLine(textStream, "");

            writePdfLine(textStream, "Top Viewed Documents:");
            writePdfLine(textStream, "--------------------------------------------------");
            if (stats.getTopViewedDocuments() != null) {
                for (DocumentStats d : stats.getTopViewedDocuments()) {
                    writePdfLine(textStream, "  - ID " + d.getDocumentId() + " (" + transliterate(d.getTitle()) + "): " + d.getCount());
                }
            }
            writePdfLine(textStream, "");

            writePdfLine(textStream, "Top Downloaded Documents:");
            writePdfLine(textStream, "--------------------------------------------------");
            if (stats.getTopDownloadedDocuments() != null) {
                for (DocumentStats d : stats.getTopDownloadedDocuments()) {
                    writePdfLine(textStream, "  - ID " + d.getDocumentId() + " (" + transliterate(d.getTitle()) + "): " + d.getCount());
                }
            }

            textStream.write("ET\n".getBytes(StandardCharsets.UTF_8));
            byte[] textBytes = textStream.toByteArray();

            // Object 5: Content stream
            offsets.add((long) bos.size());
            bos.write("5 0 obj\n".getBytes(StandardCharsets.UTF_8));
            bos.write(("<< /Length " + textBytes.length + " >>\n").getBytes(StandardCharsets.UTF_8));
            bos.write("stream\n".getBytes(StandardCharsets.UTF_8));
            bos.write(textBytes);
            bos.write("\nendstream\nendobj\n".getBytes(StandardCharsets.UTF_8));

            // Xref
            long xrefOffset = bos.size();
            bos.write("xref\n".getBytes(StandardCharsets.UTF_8));
            bos.write(("0 " + (offsets.size() + 1) + "\n").getBytes(StandardCharsets.UTF_8));
            bos.write("0000000000 65535 f \n".getBytes(StandardCharsets.UTF_8));
            for (Long offset : offsets) {
                String formatted = String.format("%010d 00000 n \n", offset);
                bos.write(formatted.getBytes(StandardCharsets.UTF_8));
            }

            // Trailer
            bos.write("trailer\n".getBytes(StandardCharsets.UTF_8));
            bos.write(("<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n").getBytes(StandardCharsets.UTF_8));
            bos.write("startxref\n".getBytes(StandardCharsets.UTF_8));
            bos.write((xrefOffset + "\n").getBytes(StandardCharsets.UTF_8));
            bos.write("%%EOF\n".getBytes(StandardCharsets.UTF_8));

            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF analytics report", e);
        }
    }

    private void writePdfLine(ByteArrayOutputStream bos, String text) throws IOException {
        if (text == null) text = "";
        String escaped = text.replace("\\", "\\\\")
                             .replace("(", "\\(")
                             .replace(")", "\\)");
        bos.write(("(" + escaped + ") Tj T*\n").getBytes(StandardCharsets.UTF_8));
    }

    public static class AnalyticsStatistics {
        private List<SearchQueryStats> popularSearches;
        private List<DocumentStats> topViewedDocuments;
        private List<DocumentStats> topDownloadedDocuments;

        public AnalyticsStatistics(List<SearchQueryStats> popularSearches,
                                   List<DocumentStats> topViewedDocuments,
                                   List<DocumentStats> topDownloadedDocuments) {
            this.popularSearches = popularSearches;
            this.topViewedDocuments = topViewedDocuments;
            this.topDownloadedDocuments = topDownloadedDocuments;
        }

        public List<SearchQueryStats> getPopularSearches() { return popularSearches; }
        public void setPopularSearches(List<SearchQueryStats> popularSearches) { this.popularSearches = popularSearches; }

        public List<DocumentStats> getTopViewedDocuments() { return topViewedDocuments; }
        public void setTopViewedDocuments(List<DocumentStats> topViewedDocuments) { this.topViewedDocuments = topViewedDocuments; }

        public List<DocumentStats> getTopDownloadedDocuments() { return topDownloadedDocuments; }
        public void setTopDownloadedDocuments(List<DocumentStats> topDownloadedDocuments) { this.topDownloadedDocuments = topDownloadedDocuments; }
    }

    public static class SearchQueryStats {
        private String query;
        private Integer count;

        public SearchQueryStats(String query, Integer count) {
            this.query = query;
            this.count = count;
        }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    public static class DocumentStats {
        private Long documentId;
        private String title;
        private Integer count;

        public DocumentStats(Long documentId, String title, Integer count) {
            this.documentId = documentId;
            this.title = title;
            this.count = count;
        }

        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}
