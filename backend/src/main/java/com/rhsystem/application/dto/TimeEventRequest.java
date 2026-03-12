package com.rhsystem.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public class TimeEventRequest {
    @NotNull(message = "ID do funcionário é obrigatório")
    private UUID employeeId;

    @NotNull(message = "Tipo de evento é obrigatório")
    private String eventType;

    @NotNull(message = "Data e hora do evento é obrigatória")
    private LocalDateTime eventTime;

    public TimeEventRequest() {
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }
}
