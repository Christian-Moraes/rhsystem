package com.rhsystem.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class TimeEvent {
    private UUID id;
    private UUID employeeId;
    private TimeEventType eventType;
    private LocalDateTime eventTime;
    private String source;
    private TimeEventStatus status;
    private LocalDateTime createdAt;

    public TimeEvent() {
    }

    public TimeEvent(UUID id, UUID employeeId, TimeEventType eventType, 
                     LocalDateTime eventTime, String source, TimeEventStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.source = source;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public TimeEventType getEventType() {
        return eventType;
    }

    public void setEventType(TimeEventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TimeEventStatus getStatus() {
        return status;
    }

    public void setStatus(TimeEventStatus status) {
        this.status = status;
    }
}
