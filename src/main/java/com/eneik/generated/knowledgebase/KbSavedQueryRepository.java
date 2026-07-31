package com.eneik.generated.knowledgebase;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KbSavedQueryRepository extends JpaRepository<KbSavedQuery, Long> {
    List<KbSavedQuery> findByUserId(Long userId);
    boolean existsByUserIdAndQueryText(Long userId, String queryText);
}
