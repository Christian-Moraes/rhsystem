package com.rhsystem.domain.valueobject;

import java.util.Objects;

public class TimeCalculationResult {
    private final int workedHours; // em minutos
    private final int extraHours; // em minutos
    private final int delayMinutes;
    private final int absenceMinutes;

    public TimeCalculationResult(int workedHours, int extraHours, int delayMinutes, int absenceMinutes) {
        this.workedHours = workedHours;
        this.extraHours = extraHours;
        this.delayMinutes = delayMinutes;
        this.absenceMinutes = absenceMinutes;
    }

    public int getWorkedHours() {
        return workedHours;
    }

    public int getExtraHours() {
        return extraHours;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public int getAbsenceMinutes() {
        return absenceMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeCalculationResult that = (TimeCalculationResult) o;
        return workedHours == that.workedHours &&
               extraHours == that.extraHours &&
               delayMinutes == that.delayMinutes &&
               absenceMinutes == that.absenceMinutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(workedHours, extraHours, delayMinutes, absenceMinutes);
    }
}
