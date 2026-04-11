package com.rhsystem.domain.valueobject;

import java.util.List;

/**
 * Resultado do cálculo com detalhamento por dia (export / auditoria).
 */
public final class TimeCalculationDetailResult {

    private final TimeCalculationResult totals;
    private final List<DailyBreakdownLine> dailyLines;
    /** Norma diária em minutos (campo da escala no banco). */
    private final int dailyNormMinutes;

    public TimeCalculationDetailResult(TimeCalculationResult totals,
                                       List<DailyBreakdownLine> dailyLines,
                                       int dailyNormMinutes) {
        this.totals = totals;
        this.dailyLines = dailyLines;
        this.dailyNormMinutes = dailyNormMinutes;
    }

    public TimeCalculationResult getTotals() {
        return totals;
    }

    public List<DailyBreakdownLine> getDailyLines() {
        return dailyLines;
    }

    public int getDailyNormMinutes() {
        return dailyNormMinutes;
    }
}
