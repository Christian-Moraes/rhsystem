package com.rhsystem.domain.model;

import java.time.LocalTime;
import java.util.UUID;

public class WorkSchedule {
    private UUID id;
    private UUID companyId;
    private Integer dailyHours; // horas diárias em minutos
    private LocalTime entryTime;
    private LocalTime exitTime;
    private Integer breakMinutes;
    private Integer toleranceLate; // tolerância de atraso em minutos
    private Integer toleranceLeave; // tolerância de saída antecipada em minutos

    public WorkSchedule() {
    }

    public WorkSchedule(UUID id, UUID companyId, Integer dailyHours, LocalTime entryTime, 
                        LocalTime exitTime, Integer breakMinutes, Integer toleranceLate, 
                        Integer toleranceLeave) {
        this.id = id;
        this.companyId = companyId;
        this.dailyHours = dailyHours;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.breakMinutes = breakMinutes;
        this.toleranceLate = toleranceLate;
        this.toleranceLeave = toleranceLeave;
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

    public Integer getDailyHours() {
        return dailyHours;
    }

    public void setDailyHours(Integer dailyHours) {
        this.dailyHours = dailyHours;
    }

    public LocalTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalTime exitTime) {
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
