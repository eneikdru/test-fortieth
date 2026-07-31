package com.eneik.generated.knowledgebase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/integration/analytics")
public class KbAnalyticsController {

    private final KbAuditLogRepository auditLogRepository;
    private final KbDocumentRepository documentRepository;

    public KbAnalyticsController(KbAuditLogRepository auditLogRepository, KbDocumentRepository documentRepository) {
        this.auditLogRepository = auditLogRepository;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/statistics")
    public AnalyticsStatistics getAnalyticsStatistics() {
        List<KbAuditLog> allLogs = auditLogRepository.findAll();

        // 1. Calculate popularSearches
        Map<String, Long> searchCounts = allLogs.stream()
            .filter(log -> "SEARCH".equalsIgnoreCase(log.getAction()))
            .filter(log -> log.getDetails() != null && !log.getDetails().trim().isEmpty())
            .collect(Collectors.groupingBy(log -> log.getDetails().trim(), Collectors.counting()));

        List<SearchQueryStats> popularSearches = searchCounts.entrySet().stream()
            .map(entry -> new SearchQueryStats(entry.getKey(), entry.getValue().intValue()))
            .sorted(Comparator.comparing(SearchQueryStats::getCount).reversed())
            .limit(10)
            .collect(Collectors.toList());

        // Cache document titles to avoid redundant DB queries
        Map<Long, String> docTitles = documentRepository.findAll().stream()
            .collect(Collectors.toMap(KbDocument::getId, KbDocument::getTitle, (a, b) -> a));

        // 2. Calculate topViewedDocuments
        Map<Long, Long> viewCounts = allLogs.stream()
            .filter(log -> "VIEW".equalsIgnoreCase(log.getAction()))
            .filter(log -> "KbDocument".equalsIgnoreCase(log.getTargetEntity()))
            .filter(log -> log.getTargetId() != null)
            .collect(Collectors.groupingBy(KbAuditLog::getTargetId, Collectors.counting()));

        List<DocumentStats> topViewedDocuments = viewCounts.entrySet().stream()
            .map(entry -> {
                Long docId = entry.getKey();
                String title = docTitles.getOrDefault(docId, entry.getValue() != null ? "Unknown Document" : "");
                // Fallback to log details if title not found in current documents
                if ("Unknown Document".equals(title)) {
                    // Try to find a log for this targetId that has details
                    String loggedTitle = allLogs.stream()
                        .filter(l -> docId.equals(l.getTargetId()) && l.getDetails() != null)
                        .map(KbAuditLog::getDetails)
                        .findFirst()
                        .orElse("Unknown Document");
                    title = loggedTitle;
                }
                return new DocumentStats(docId, title, entry.getValue().intValue());
            })
            .sorted(Comparator.comparing(DocumentStats::getCount).reversed())
            .limit(10)
            .collect(Collectors.toList());

        // 3. Calculate topDownloadedDocuments
        Map<Long, Long> downloadCounts = allLogs.stream()
            .filter(log -> "DOWNLOAD".equalsIgnoreCase(log.getAction()))
            .filter(log -> "KbDocument".equalsIgnoreCase(log.getTargetEntity()))
            .filter(log -> log.getTargetId() != null)
            .collect(Collectors.groupingBy(KbAuditLog::getTargetId, Collectors.counting()));

        List<DocumentStats> topDownloadedDocuments = downloadCounts.entrySet().stream()
            .map(entry -> {
                Long docId = entry.getKey();
                String title = docTitles.getOrDefault(docId, "Unknown Document");
                if ("Unknown Document".equals(title)) {
                    String loggedTitle = allLogs.stream()
                        .filter(l -> docId.equals(l.getTargetId()) && l.getDetails() != null)
                        .map(KbAuditLog::getDetails)
                        .findFirst()
                        .orElse("Unknown Document");
                    // Strip version info from download details if present (e.g. "Title (v1)" -> "Title")
                    if (loggedTitle.contains(" (v")) {
                        loggedTitle = loggedTitle.substring(0, loggedTitle.lastIndexOf(" (v"));
                    }
                    title = loggedTitle;
                }
                return new DocumentStats(docId, title, entry.getValue().intValue());
            })
            .sorted(Comparator.comparing(DocumentStats::getCount).reversed())
            .limit(10)
            .collect(Collectors.toList());

        return new AnalyticsStatistics(popularSearches, topViewedDocuments, topDownloadedDocuments);
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
