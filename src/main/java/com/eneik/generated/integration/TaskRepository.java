package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Task t SET t.status = :newStatus, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :id AND t.status = :expectedStatus")
    int updateStatusAtomic(
            @Param("id") String id,
            @Param("newStatus") String newStatus,
            @Param("expectedStatus") String expectedStatus
    );
}
