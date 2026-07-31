package com.eneik.generated.knowledgebase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KbDocumentUpdateRequestRepository extends JpaRepository<KbDocumentUpdateRequest, Long> {

    List<KbDocumentUpdateRequest> findByDocumentId(Long documentId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE KbDocumentUpdateRequest r SET r.status = :newStatus, r.updatedAt = CURRENT_TIMESTAMP WHERE r.id = :id AND r.status = :oldStatus")
    int updateStatusAtomically(@Param("id") Long id, @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus);
}
