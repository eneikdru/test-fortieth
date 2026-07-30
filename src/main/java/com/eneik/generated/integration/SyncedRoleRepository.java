package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SyncedRoleRepository extends JpaRepository<SyncedRole, Long> {
    Optional<SyncedRole> findByExternalRoleName(String externalRoleName);
}
