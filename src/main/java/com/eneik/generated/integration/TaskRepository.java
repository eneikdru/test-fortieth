package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Task t SET t.status = :newStatus, t.statusChangedAt = CURRENT_TIMESTAMP WHERE t.id = :id AND t.status = :oldStatus")
    int updateStatusAtomically(@Param("id") Long id, @Param("oldStatus") TaskStatus oldStatus, @Param("newStatus") TaskStatus newStatus);

    List<Task> findByStatus(TaskStatus status);

    long countByStatus(TaskStatus status);

    List<Task> findByFeatureIdAndStatus(Long featureId, TaskStatus status);

    long countByFeatureId(Long featureId);

    long countByFeatureIdAndStatus(Long featureId, TaskStatus status);
}
