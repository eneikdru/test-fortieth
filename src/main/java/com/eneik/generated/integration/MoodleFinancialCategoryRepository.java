package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MoodleFinancialCategoryRepository extends JpaRepository<MoodleFinancialCategory, Long> {
    Optional<MoodleFinancialCategory> findByName(String name);
}
