package com.rhsystem.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserCompany {
    private UUID id;
    private UUID userId;
    private UUID companyId;
    private UserRole role;
    private LocalDateTime createdAt;

    public UserCompany() {
    }

    public UserCompany(UUID id, UUID userId, UUID companyId, UserRole role, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.companyId = companyId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
