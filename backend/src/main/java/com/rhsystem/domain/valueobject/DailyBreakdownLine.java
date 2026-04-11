package com.rhsystem.domain.valueobject;

import java.time.LocalDate;

/**
 * Uma linha do detalhamento diário — mesma lógica aplicada em {@link com.rhsystem.domain.service.TimeCalculationEngine}.
 */
public record DailyBreakdownLine(
        LocalDate date,
        int workedRawMinutes,
        int workedCountedMinutes,
        int extraMinutes,
        int delayMinutes,
        int absenceMinutes,
        String firstEntryTime,
        String calculationNote
) {}
