package com.eneik.generated.knowledgebase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KbAuditLogRepository extends JpaRepository<KbAuditLog, Long> {
}
