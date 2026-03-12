package com.rhsystem.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public class Employee {
    private UUID id;
    private UUID companyId;
    private String name;
    private String cpf;
    private String role;
    private LocalDate hireDate;
    private UUID workScheduleId;
    private EmployeeStatus status;

    public Employee() {
    }

    public Employee(UUID id, UUID companyId, String name, String cpf, String role, 
                    LocalDate hireDate, UUID workScheduleId, EmployeeStatus status) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.cpf = cpf;
        this.role = role;
        this.hireDate = hireDate;
        this.workScheduleId = workScheduleId;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public UUID getWorkScheduleId() {
        return workScheduleId;
    }

    public void setWorkScheduleId(UUID workScheduleId) {
        this.workScheduleId = workScheduleId;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }
}
