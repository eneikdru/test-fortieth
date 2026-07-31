package com.eneik.generated.knowledgebase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KbDocumentRepository extends JpaRepository<KbDocument, Long> {
}
