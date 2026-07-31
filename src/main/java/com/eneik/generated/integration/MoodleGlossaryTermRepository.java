package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MoodleGlossaryTermRepository extends JpaRepository<MoodleGlossaryTerm, Long> {
    Optional<MoodleGlossaryTerm> findByTerm(String term);
}
