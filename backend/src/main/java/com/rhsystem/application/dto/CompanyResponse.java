package com.rhsystem.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class CompanyResponse {
    private UUID id;
    private String name;
    private String cnpj;
    private String responsibleName;
    private String email;
    private String phone;
    private Boolean enableOvertime;
    private Boolean enableTimeBank;
    private Integer overtimePercentage;
    private Integer weekendPercentage;
    private LocalDateTime createdAt;
    private String role;

    public CompanyResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getResponsibleName() {
        return responsibleName;
    }

    public void setResponsibleName(String responsibleName) {
        this.responsibleName = responsibleName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getEnableOvertime() {
        return enableOvertime;
    }

    public void setEnableOvertime(Boolean enableOvertime) {
        this.enableOvertime = enableOvertime;
    }

    public Boolean getEnableTimeBank() {
        return enableTimeBank;
    }

    public void setEnableTimeBank(Boolean enableTimeBank) {
        this.enableTimeBank = enableTimeBank;
    }

    public Integer getOvertimePercentage() {
        return overtimePercentage;
    }

    public void setOvertimePercentage(Integer overtimePercentage) {
        this.overtimePercentage = overtimePercentage;
    }

    public Integer getWeekendPercentage() {
        return weekendPercentage;
    }

    public void setWeekendPercentage(Integer weekendPercentage) {
        this.weekendPercentage = weekendPercentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
