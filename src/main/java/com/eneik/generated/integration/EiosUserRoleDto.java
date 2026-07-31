package com.eneik.generated.integration;

import java.util.List;

public class EiosUserRoleDto {
    private String userId;
    private List<String> externalRoles;

    public EiosUserRoleDto() {}

    public EiosUserRoleDto(String userId, List<String> externalRoles) {
        this.userId = userId;
        this.externalRoles = externalRoles;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getExternalRoles() {
        return externalRoles;
    }

    public void setExternalRoles(List<String> externalRoles) {
        this.externalRoles = externalRoles;
    }
}
