package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureRepository extends JpaRepository<FeatureEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FeatureEntity f SET f.readinessRatio = :newRatio WHERE f.id = :id")
    int updateReadinessAtomically(@Param("id") Long id, @Param("newRatio") double newRatio);
}
