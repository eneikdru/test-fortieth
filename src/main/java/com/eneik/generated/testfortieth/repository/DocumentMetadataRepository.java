package com.eneik.generated.testfortieth.repository;

import com.eneik.generated.testfortieth.model.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {
    List<DocumentMetadata> findByDocumentId(Long documentId);
}
