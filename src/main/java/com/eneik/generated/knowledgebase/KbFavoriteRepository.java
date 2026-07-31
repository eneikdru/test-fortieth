package com.eneik.generated.knowledgebase;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KbFavoriteRepository extends JpaRepository<KbFavorite, Long> {
    Optional<KbFavorite> findByUserIdAndDocumentId(Long userId, Long documentId);
    List<KbFavorite> findByUserId(Long userId);
}
