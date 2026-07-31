package com.eneik.generated.integration;

import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class FinancialAccessControlService {

    private final SyncedRoleRepository syncedRoleRepository;

    public FinancialAccessControlService(SyncedRoleRepository syncedRoleRepository) {
        this.syncedRoleRepository = syncedRoleRepository;
    }

    /**
     * Checks if a user with a given role has permission to perform an action on financial blocks/documents.
     * Actions: "READ", "WRITE"
     */
    public boolean checkPermission(String externalRole, String action) {
        if (externalRole == null) {
            return false;
        }

        String normalizedRole = externalRole.trim().toLowerCase();

        // Standard student role in Moodle is "student"
        if ("student".equals(normalizedRole)) {
            return false;
        }

        // Economist role "economist" is granted read and write access to all financial block categories/documents.
        if ("economist".equals(normalizedRole)) {
            return true;
        }

        // Fallback to checking DB SyncedRoles mapping if needed
        Optional<SyncedRole> syncedRoleOpt = syncedRoleRepository.findByExternalRoleName(externalRole);
        if (syncedRoleOpt.isPresent()) {
            String internalId = syncedRoleOpt.get().getInternalEiosIdentifier().toLowerCase();
            if (internalId.contains("economist")) {
                return true;
            }
            if (internalId.contains("student")) {
                return false;
            }
        }

        return false;
    }

    public void enforcePermission(String externalRole, String action) {
        if (!checkPermission(externalRole, action)) {
            throw new FinancialAccessDeniedException("Access denied to financial documents for role: " + externalRole);
        }
    }
}
