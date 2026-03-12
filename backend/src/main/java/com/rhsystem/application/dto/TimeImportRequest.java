package com.rhsystem.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class TimeImportRequest {
    @NotNull(message = "ID do funcionário é obrigatório")
    private UUID employeeId;

    private String fileType;

    public TimeImportRequest() {
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
