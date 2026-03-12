package com.rhsystem.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Company {
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

    public Company() {
    }

    public Company(UUID id, String name, String cnpj, String responsibleName, 
                   String email, String phone, Boolean enableOvertime, Boolean enableTimeBank,
                   Integer overtimePercentage, Integer weekendPercentage, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.cnpj = cnpj;
        this.responsibleName = responsibleName;
        this.email = email;
        this.phone = phone;
        this.enableOvertime = enableOvertime;
        this.enableTimeBank = enableTimeBank;
        this.overtimePercentage = overtimePercentage;
        this.weekendPercentage = weekendPercentage;
        this.createdAt = createdAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
}
