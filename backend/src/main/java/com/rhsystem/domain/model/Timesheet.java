package com.rhsystem.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Timesheet {
    private UUID id;
    private UUID employeeId;
    private Integer month;
    private Integer year;
    private Integer workedHours; // em minutos
    private Integer extraHours; // em minutos
    private Integer delayMinutes;
    private Integer absenceMinutes;
    private LocalDateTime closedAt;

    public Timesheet() {
    }

    public Timesheet(UUID id, UUID employeeId, Integer month, Integer year, 
                     Integer workedHours, Integer extraHours, Integer delayMinutes, 
                     Integer absenceMinutes, LocalDateTime closedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.month = month;
        this.year = year;
        this.workedHours = workedHours;
        this.extraHours = extraHours;
        this.delayMinutes = delayMinutes;
        this.absenceMinutes = absenceMinutes;
        this.closedAt = closedAt;
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

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWorkedHours() {
        return workedHours;
    }

    public void setWorkedHours(Integer workedHours) {
        this.workedHours = workedHours;
    }

    public Integer getExtraHours() {
        return extraHours;
    }

    public void setExtraHours(Integer extraHours) {
        this.extraHours = extraHours;
    }

    public Integer getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(Integer delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public Integer getAbsenceMinutes() {
        return absenceMinutes;
    }

    public void setAbsenceMinutes(Integer absenceMinutes) {
        this.absenceMinutes = absenceMinutes;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
