package com.eneik.generated.knowledgebase;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KbUserFavoriteRepository extends JpaRepository<KbUserFavorite, KbUserFavoriteId> {
    List<KbUserFavorite> findByUserId(Long userId);
    boolean existsById(KbUserFavoriteId id);
}
