package com.rhsystem.application.dto;

import java.util.List;

/**
 * Detalhamento do fechamento para exportação (mesmos números que {@code closeTimesheet}).
 */
public class ClosureBreakdownResponse {

    private String companyName;
    /** Configuração da empresa (cadastro); o motor pode ainda não aplicar banco/% em todos os passos. */
    private Boolean companyEnableOvertime;
    private Boolean companyEnableTimeBank;
    private Integer companyOvertimePercentage;
    private Integer companyWeekendPercentage;

    private String employeeName;
    private int month;
    private int year;

    /** Norma diária em minutos (coluna daily_hours da escala). */
    private int scheduleDailyNormMinutes;
    private String scheduleEntryTime;
    private String scheduleExitTime;
    private Integer scheduleBreakMinutes;
    private Integer scheduleToleranceLate;
    private Integer scheduleToleranceLeave;

    private List<DailyLine> dailyLines;

    private int calculatedWorkedMinutes;
    private int calculatedExtraMinutes;
    private int calculatedDelayMinutes;
    private int calculatedAbsenceMinutes;

    /** Se o mês já foi fechado: instante ISO-8601; senão null. */
    private String closedAt;
    private Integer storedWorkedMinutes;
    private Integer storedExtraMinutes;
    private Integer storedDelayMinutes;
    private Integer storedAbsenceMinutes;

    /** Texto fixo explicando as regras (espelha o motor de cálculo). */
    private List<String> formulaDocumentation;

    public static class DailyLine {
        private String date;
        /** Batidas do dia na ordem da grade (E1/S1/E2/S2…), espelhando a tela de ponto. */
        private String punchTimeline;
        private int workedRawMinutes;
        private int workedCountedMinutes;
        private int extraMinutes;
        private int delayMinutes;
        private int absenceMinutes;
        private String firstEntryTime;
        private String calculationNote;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getPunchTimeline() {
            return punchTimeline;
        }

        public void setPunchTimeline(String punchTimeline) {
            this.punchTimeline = punchTimeline;
        }

        public int getWorkedRawMinutes() {
            return workedRawMinutes;
        }

        public void setWorkedRawMinutes(int workedRawMinutes) {
            this.workedRawMinutes = workedRawMinutes;
        }

        public int getWorkedCountedMinutes() {
            return workedCountedMinutes;
        }

        public void setWorkedCountedMinutes(int workedCountedMinutes) {
            this.workedCountedMinutes = workedCountedMinutes;
        }

        public int getExtraMinutes() {
            return extraMinutes;
        }

        public void setExtraMinutes(int extraMinutes) {
            this.extraMinutes = extraMinutes;
        }

        public int getDelayMinutes() {
            return delayMinutes;
        }

        public void setDelayMinutes(int delayMinutes) {
            this.delayMinutes = delayMinutes;
        }

        public int getAbsenceMinutes() {
            return absenceMinutes;
        }

        public void setAbsenceMinutes(int absenceMinutes) {
            this.absenceMinutes = absenceMinutes;
        }

        public String getFirstEntryTime() {
            return firstEntryTime;
        }

        public void setFirstEntryTime(String firstEntryTime) {
            this.firstEntryTime = firstEntryTime;
        }

        public String getCalculationNote() {
            return calculationNote;
        }

        public void setCalculationNote(String calculationNote) {
            this.calculationNote = calculationNote;
        }
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Boolean getCompanyEnableOvertime() {
        return companyEnableOvertime;
    }

    public void setCompanyEnableOvertime(Boolean companyEnableOvertime) {
        this.companyEnableOvertime = companyEnableOvertime;
    }

    public Boolean getCompanyEnableTimeBank() {
        return companyEnableTimeBank;
    }

    public void setCompanyEnableTimeBank(Boolean companyEnableTimeBank) {
        this.companyEnableTimeBank = companyEnableTimeBank;
    }

    public Integer getCompanyOvertimePercentage() {
        return companyOvertimePercentage;
    }

    public void setCompanyOvertimePercentage(Integer companyOvertimePercentage) {
        this.companyOvertimePercentage = companyOvertimePercentage;
    }

    public Integer getCompanyWeekendPercentage() {
        return companyWeekendPercentage;
    }

    public void setCompanyWeekendPercentage(Integer companyWeekendPercentage) {
        this.companyWeekendPercentage = companyWeekendPercentage;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getScheduleDailyNormMinutes() {
        return scheduleDailyNormMinutes;
    }

    public void setScheduleDailyNormMinutes(int scheduleDailyNormMinutes) {
        this.scheduleDailyNormMinutes = scheduleDailyNormMinutes;
    }

    public String getScheduleEntryTime() {
        return scheduleEntryTime;
    }

    public void setScheduleEntryTime(String scheduleEntryTime) {
        this.scheduleEntryTime = scheduleEntryTime;
    }

    public String getScheduleExitTime() {
        return scheduleExitTime;
    }

    public void setScheduleExitTime(String scheduleExitTime) {
        this.scheduleExitTime = scheduleExitTime;
    }

    public Integer getScheduleBreakMinutes() {
        return scheduleBreakMinutes;
    }

    public void setScheduleBreakMinutes(Integer scheduleBreakMinutes) {
        this.scheduleBreakMinutes = scheduleBreakMinutes;
    }

    public Integer getScheduleToleranceLate() {
        return scheduleToleranceLate;
    }

    public void setScheduleToleranceLate(Integer scheduleToleranceLate) {
        this.scheduleToleranceLate = scheduleToleranceLate;
    }

    public Integer getScheduleToleranceLeave() {
        return scheduleToleranceLeave;
    }

    public void setScheduleToleranceLeave(Integer scheduleToleranceLeave) {
        this.scheduleToleranceLeave = scheduleToleranceLeave;
    }

    public List<DailyLine> getDailyLines() {
        return dailyLines;
    }

    public void setDailyLines(List<DailyLine> dailyLines) {
        this.dailyLines = dailyLines;
    }

    public int getCalculatedWorkedMinutes() {
        return calculatedWorkedMinutes;
    }

    public void setCalculatedWorkedMinutes(int calculatedWorkedMinutes) {
        this.calculatedWorkedMinutes = calculatedWorkedMinutes;
    }

    public int getCalculatedExtraMinutes() {
        return calculatedExtraMinutes;
    }

    public void setCalculatedExtraMinutes(int calculatedExtraMinutes) {
        this.calculatedExtraMinutes = calculatedExtraMinutes;
    }

    public int getCalculatedDelayMinutes() {
        return calculatedDelayMinutes;
    }

    public void setCalculatedDelayMinutes(int calculatedDelayMinutes) {
        this.calculatedDelayMinutes = calculatedDelayMinutes;
    }

    public int getCalculatedAbsenceMinutes() {
        return calculatedAbsenceMinutes;
    }

    public void setCalculatedAbsenceMinutes(int calculatedAbsenceMinutes) {
        this.calculatedAbsenceMinutes = calculatedAbsenceMinutes;
    }

    public String getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(String closedAt) {
        this.closedAt = closedAt;
    }

    public Integer getStoredWorkedMinutes() {
        return storedWorkedMinutes;
    }

    public void setStoredWorkedMinutes(Integer storedWorkedMinutes) {
        this.storedWorkedMinutes = storedWorkedMinutes;
    }

    public Integer getStoredExtraMinutes() {
        return storedExtraMinutes;
    }

    public void setStoredExtraMinutes(Integer storedExtraMinutes) {
        this.storedExtraMinutes = storedExtraMinutes;
    }

    public Integer getStoredDelayMinutes() {
        return storedDelayMinutes;
    }

    public void setStoredDelayMinutes(Integer storedDelayMinutes) {
        this.storedDelayMinutes = storedDelayMinutes;
    }

    public Integer getStoredAbsenceMinutes() {
        return storedAbsenceMinutes;
    }

    public void setStoredAbsenceMinutes(Integer storedAbsenceMinutes) {
        this.storedAbsenceMinutes = storedAbsenceMinutes;
    }

    public List<String> getFormulaDocumentation() {
        return formulaDocumentation;
    }

    public void setFormulaDocumentation(List<String> formulaDocumentation) {
        this.formulaDocumentation = formulaDocumentation;
    }
}
