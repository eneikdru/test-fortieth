package com.eneik.generated.knowledgebase;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KbAuditLogRepository extends JpaRepository<KbAuditLog, Long> {

    @Query("SELECT TRIM(a.details) as query, COUNT(a) as count " +
           "FROM KbAuditLog a " +
           "WHERE UPPER(a.action) = 'SEARCH' AND a.details IS NOT NULL AND TRIM(a.details) <> '' " +
           "GROUP BY TRIM(a.details) " +
           "ORDER BY COUNT(a) DESC")
    List<SearchQueryProjection> findPopularSearches(Pageable pageable);

    @Query("SELECT a.targetId as targetId, COUNT(a) as count " +
           "FROM KbAuditLog a " +
           "WHERE UPPER(a.action) = 'VIEW' AND UPPER(a.targetEntity) = 'KBDOCUMENT' AND a.targetId IS NOT NULL " +
           "GROUP BY a.targetId " +
           "ORDER BY COUNT(a) DESC")
    List<DocumentStatsProjection> findTopViewedDocuments(Pageable pageable);

    @Query("SELECT a.targetId as targetId, COUNT(a) as count " +
           "FROM KbAuditLog a " +
           "WHERE UPPER(a.action) = 'DOWNLOAD' AND UPPER(a.targetEntity) = 'KBDOCUMENT' AND a.targetId IS NOT NULL " +
           "GROUP BY a.targetId " +
           "ORDER BY COUNT(a) DESC")
    List<DocumentStatsProjection> findTopDownloadedDocuments(Pageable pageable);

    @Query("SELECT a.details FROM KbAuditLog a WHERE a.targetId = :targetId AND a.details IS NOT NULL")
    List<String> findFirstDetailsByTargetId(Long targetId, Pageable pageable);

    interface SearchQueryProjection {
        String getQuery();
        Long getCount();
    }

    interface DocumentStatsProjection {
        Long getTargetId();
        Long getCount();
    }
}
