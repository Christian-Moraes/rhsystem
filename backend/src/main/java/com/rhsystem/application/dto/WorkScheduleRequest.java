package com.rhsystem.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para criar escalas de trabalho.
 * Horários devem vir no formato "HH:mm" (ex.: "14:00").
 * {@code dailyHours} = horas de jornada por dia (ex.: 8), convertidas para minutos na persistência.
 */
public class WorkScheduleRequest {

    @NotNull(message = "dailyHours é obrigatório")
    @Min(value = 1, message = "Informe pelo menos 1 hora de jornada por dia")
    @Max(value = 24, message = "Jornada diária não pode ultrapassar 24 horas")
    private Integer dailyHours;

    @NotBlank(message = "entryTime é obrigatório (HH:mm)")
    private String entryTime;

    @NotBlank(message = "exitTime é obrigatório (HH:mm)")
    private String exitTime;

    @NotNull(message = "breakMinutes é obrigatório")
    @Min(value = 0, message = "breakMinutes não pode ser negativo")
    private Integer breakMinutes;

    @NotNull(message = "toleranceLate é obrigatório")
    @Min(value = 0, message = "toleranceLate não pode ser negativo")
    private Integer toleranceLate;

    @NotNull(message = "toleranceLeave é obrigatório")
    @Min(value = 0, message = "toleranceLeave não pode ser negativo")
    private Integer toleranceLeave;

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

