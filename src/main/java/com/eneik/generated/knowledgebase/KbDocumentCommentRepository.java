package com.eneik.generated.knowledgebase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KbDocumentCommentRepository extends JpaRepository<KbDocumentComment, Long> {
    List<KbDocumentComment> findByDocumentIdOrderByCreatedAtAsc(Long documentId);
}
