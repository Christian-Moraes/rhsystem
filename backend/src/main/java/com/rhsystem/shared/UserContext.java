package com.rhsystem.shared;

import java.util.UUID;

public class UserContext {
    private UUID userId;
    private UUID companyId;
    private String role;

    public UserContext() {
    }

    public UserContext(UUID userId, UUID companyId, String role) {
        this.userId = userId;
        this.companyId = companyId;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
