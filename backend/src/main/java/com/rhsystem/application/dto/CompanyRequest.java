package com.rhsystem.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class CompanyRequest {
    @NotBlank(message = "Nome da empresa é obrigatório")
    private String name;

    @NotBlank(message = "CNPJ é obrigatório")
    private String cnpj;

    @NotBlank(message = "Nome do responsável é obrigatório")
    private String responsibleName;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    private String phone;

    @NotNull(message = "Hora extra é obrigatório")
    private Boolean enableOvertime;

    @NotNull(message = "Banco de horas é obrigatório")
    private Boolean enableTimeBank;

    @NotNull(message = "Porcentagem de hora extra é obrigatória")
    @Min(value = 0, message = "Porcentagem deve ser entre 0 e 200")
    @Max(value = 200, message = "Porcentagem deve ser entre 0 e 200")
    private Integer overtimePercentage;

    @NotNull(message = "Porcentagem de fim de semana é obrigatória")
    @Min(value = 0, message = "Porcentagem deve ser entre 0 e 200")
    @Max(value = 200, message = "Porcentagem deve ser entre 0 e 200")
    private Integer weekendPercentage;

    public CompanyRequest() {
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
}
