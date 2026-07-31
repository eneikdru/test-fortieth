package com.eneik.generated.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocalUserPermissionRepository extends JpaRepository<LocalUserPermission, Long> {
    List<LocalUserPermission> findByUserId(String userId);
    Optional<LocalUserPermission> findByUserIdAndInternalEiosIdentifier(String userId, String internalEiosIdentifier);
    void deleteByUserId(String userId);
}
