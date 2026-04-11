package com.rhsystem.domain.service;

import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.TimeEventType;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.valueobject.TimeCalculationDetailResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeCalculationEngineTest {

    private final TimeCalculationEngine engine = new TimeCalculationEngine();

    private static WorkSchedule schedule8h() {
        WorkSchedule w = new WorkSchedule();
        w.setDailyHours(480);
        w.setEntryTime(LocalTime.of(8, 0));
        w.setToleranceLate(0);
        return w;
    }

    private static TimeEvent ev(TimeEventType type, LocalDateTime t) {
        TimeEvent e = new TimeEvent();
        e.setId(UUID.randomUUID());
        e.setEventType(type);
        e.setEventTime(t);
        return e;
    }

    @Test
    void sameDay_twoSegments_typicalOffice() {
        List<TimeEvent> list = new ArrayList<>();
        LocalDateTime d = LocalDateTime.of(2026, 4, 1, 7, 0);
        list.add(ev(TimeEventType.ENTRY, d));
        list.add(ev(TimeEventType.EXIT, d.withHour(12)));
        list.add(ev(TimeEventType.ENTRY, d.withHour(13)));
        list.add(ev(TimeEventType.EXIT, d.withHour(17)));
        TimeCalculationDetailResult r = engine.calculateDetailed(list, schedule8h());
        assertEquals(540, r.getDailyLines().get(0).workedRawMinutes());
        assertEquals(60, r.getTotals().getExtraHours());
    }

    @Test
    void crossDay_doesNotPairExitWithPreviousDayEntry() {
        List<TimeEvent> list = new ArrayList<>();
        list.add(ev(TimeEventType.ENTRY, LocalDateTime.of(2026, 4, 1, 7, 0)));
        list.add(ev(TimeEventType.ENTRY, LocalDateTime.of(2026, 4, 2, 7, 0)));
        list.add(ev(TimeEventType.EXIT, LocalDateTime.of(2026, 4, 2, 17, 0)));
        TimeCalculationDetailResult r = engine.calculateDetailed(list, schedule8h());
        int sumExtras = r.getTotals().getExtraHours();
        assertTrue(sumExtras < 8 * 60, "extras não devem explodir por pareamento entre dias; soma=" + sumExtras);
        assertEquals(600, r.getDailyLines().stream()
                .filter(l -> l.date().getDayOfMonth() == 2)
                .findFirst()
                .orElseThrow()
                .workedRawMinutes());
    }

    @Test
    void longSegmentWithinDay_isCapped() {
        List<TimeEvent> list = List.of(
                ev(TimeEventType.ENTRY, LocalDateTime.of(2026, 4, 10, 5, 0)),
                ev(TimeEventType.EXIT, LocalDateTime.of(2026, 4, 10, 22, 0))
        );
        TimeCalculationDetailResult r = engine.calculateDetailed(list, schedule8h());
        assertEquals(16 * 60, r.getDailyLines().get(0).workedRawMinutes());
    }

    /**
     * Regressão: ~23 dias úteis padrão (7–12 / 13–17) com um dia sem 2º turno não deve gerar centenas de horas de extra.
     * 22 × (540 bruto, 480 norma → 60 extra) + 1 × (300 bruto → 0 extra) = 10860 trabalhadas, 1320 extras (minutos).
     */
    @Test
    void twentyThreeDays_typicalShifts_extrasStayBounded() {
        List<TimeEvent> list = new ArrayList<>();
        for (int day = 1; day <= 23; day++) {
            LocalDate d = LocalDate.of(2026, 4, day);
            if (day == 15) {
                list.add(ev(TimeEventType.ENTRY, d.atTime(7, 0)));
                list.add(ev(TimeEventType.EXIT, d.atTime(12, 0)));
                list.add(ev(TimeEventType.ENTRY, d.atTime(13, 0)));
            } else {
                list.add(ev(TimeEventType.ENTRY, d.atTime(7, 0)));
                list.add(ev(TimeEventType.EXIT, d.atTime(12, 0)));
                list.add(ev(TimeEventType.ENTRY, d.atTime(13, 0)));
                list.add(ev(TimeEventType.EXIT, d.atTime(17, 0)));
            }
        }
        TimeCalculationDetailResult r = engine.calculateDetailed(list, schedule8h());
        assertEquals(10860, r.getTotals().getWorkedHours());
        assertEquals(1320, r.getTotals().getExtraHours());
        assertTrue(r.getTotals().getExtraHours() < 50 * 60, "extras não devem passar de ~50h em cenário realista");
    }
}
