package com.rhsystem.application.dto;

import java.util.UUID;

public class WorkScheduleResponse {
    private UUID id;
    private Integer dailyHours;
    private String entryTime; // HH:mm
    private String exitTime;  // HH:mm
    private Integer breakMinutes;
    private Integer toleranceLate;
    private Integer toleranceLeave;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getDailyHours() {
        return dailyHours;
    }

    public void setDailyHours(Integer dailyHours) {
        this.dailyHours = dailyHours;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public String getExitTime() {
        return exitTime;
    }

    public void setExitTime(String exitTime) {
        this.exitTime = exitTime;
    }

    public Integer getBreakMinutes() {
        return breakMinutes;
    }

    public void setBreakMinutes(Integer breakMinutes) {
        this.breakMinutes = breakMinutes;
    }

    public Integer getToleranceLate() {
        return toleranceLate;
    }

    public void setToleranceLate(Integer toleranceLate) {
        this.toleranceLate = toleranceLate;
    }

    public Integer getToleranceLeave() {
        return toleranceLeave;
    }

    public void setToleranceLeave(Integer toleranceLeave) {
        this.toleranceLeave = toleranceLeave;
    }
}

