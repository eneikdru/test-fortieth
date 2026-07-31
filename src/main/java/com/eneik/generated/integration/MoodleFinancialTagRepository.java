package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MoodleFinancialTagRepository extends JpaRepository<MoodleFinancialTag, Long> {
    Optional<MoodleFinancialTag> findByName(String name);
}
