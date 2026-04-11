package com.rhsystem.domain.service;

import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.TimeEventType;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.valueobject.DailyBreakdownLine;
import com.rhsystem.domain.valueobject.TimeCalculationDetailResult;
import com.rhsystem.domain.valueobject.TimeCalculationResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class TimeCalculationEngine {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    /** Acima disso, o trecho é tratado como dado inválido (evita 24h+ por pareamento errado). */
    private static final int MAX_SEGMENT_MINUTES = 16 * 60;

    /** Teto físico por dia civil (soma dos trechos válidos). */
    private static final int MAX_DAY_WORKED_MINUTES = 20 * 60;

    public TimeCalculationResult calculate(List<TimeEvent> events, WorkSchedule workSchedule) {
        return calculateDetailed(events, workSchedule).getTotals();
    }

    public TimeCalculationDetailResult calculateDetailed(List<TimeEvent> events, WorkSchedule workSchedule) {
        if (workSchedule == null) {
            throw new IllegalArgumentException("WorkSchedule não pode ser nulo");
        }
        int dailyMinutes = normalizeDailyNormMinutes(workSchedule.getDailyHours());

        if (events == null || events.isEmpty()) {
            return new TimeCalculationDetailResult(
                    new TimeCalculationResult(0, 0, 0, 0),
                    List.of(),
                    dailyMinutes
            );
        }

        List<TimeEvent> sorted = dedupeSameMinuteAndType(events.stream()
                .filter(e -> e.getEventType() == TimeEventType.ENTRY || e.getEventType() == TimeEventType.EXIT)
                .sorted(Comparator.comparing(TimeEvent::getEventTime))
                .collect(Collectors.toList()));

        // Pareamento só dentro do mesmo dia civil. FIFO global emparelhava saída de terça
        // com entrada de segunda → trechos de dezenas de horas e extras irreais.
        Map<LocalDate, Integer> workedByEntryDate = buildWorkedMinutesPerDay(sorted);

        Map<LocalDate, List<TimeEvent>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(e -> e.getEventTime().toLocalDate()));

        TreeSet<LocalDate> datesToProcess = new TreeSet<>(eventsByDate.keySet());
        datesToProcess.addAll(workedByEntryDate.keySet());

        List<DailyBreakdownLine> lines = new ArrayList<>();

        for (LocalDate date : datesToProcess) {
            int workedDay = workedByEntryDate.getOrDefault(date, 0);

            LocalTime firstEntryOfDay = eventsByDate.getOrDefault(date, List.of()).stream()
                    .filter(e -> e.getEventType() == TimeEventType.ENTRY)
                    .min(Comparator.comparing(TimeEvent::getEventTime))
                    .map(e -> e.getEventTime().toLocalTime())
                    .orElse(null);

            String firstEntryStr = firstEntryOfDay != null ? firstEntryOfDay.format(HM) : "-";

            if (workedDay <= 0) {
                lines.add(new DailyBreakdownLine(
                        date,
                        0,
                        0,
                        0,
                        0,
                        dailyMinutes,
                        firstEntryStr,
                        "Sem trechos E→S com duração > 0 min. Ausência do dia = norma (" + dailyMinutes + " min)."
                ));
                continue;
            }

            int rawWorkedBeforeDayCap = workedDay;
            workedDay = Math.min(workedDay, MAX_DAY_WORKED_MINUTES);
            boolean dayCapped = rawWorkedBeforeDayCap > MAX_DAY_WORKED_MINUTES;

            int delayMinutes = firstEntryOfDay != null ? calculateDelay(firstEntryOfDay, workSchedule) : 0;
            int extraMinutes = Math.max(0, workedDay - dailyMinutes);
            int adjustedWorked = workedDay;
            if (extraMinutes > 0) {
                adjustedWorked = dailyMinutes;
            }

            String note = String.format(
                    "Bruto (soma trechos)=%d min | Norma=%d min | Extras=max(0,bruto−norma)=%d min | "
                            + "Trabalhadas contábeis=min(bruto,norma)=%d min | Atraso=após entrada_escala+tolerância=%d min"
                            + "%s",
                    workedDay, dailyMinutes, extraMinutes, adjustedWorked, delayMinutes,
                    dayCapped ? " | Bruto limitado a " + MAX_DAY_WORKED_MINUTES + " min/dia (soma suspeita)." : ""
            );

            lines.add(new DailyBreakdownLine(
                    date,
                    workedDay,
                    adjustedWorked,
                    extraMinutes,
                    delayMinutes,
                    0,
                    firstEntryStr,
                    note
            ));
        }

        int totalWorkedMinutes = lines.stream().mapToInt(DailyBreakdownLine::workedCountedMinutes).sum();
        int totalExtraMinutes = lines.stream().mapToInt(DailyBreakdownLine::extraMinutes).sum();
        int totalDelayMinutes = lines.stream().mapToInt(DailyBreakdownLine::delayMinutes).sum();
        int totalAbsenceMinutes = lines.stream().mapToInt(DailyBreakdownLine::absenceMinutes).sum();

        return new TimeCalculationDetailResult(
                new TimeCalculationResult(totalWorkedMinutes, totalExtraMinutes, totalDelayMinutes, totalAbsenceMinutes),
                lines,
                dailyMinutes
        );
    }

    private int calculateDelay(LocalTime entryTime, WorkSchedule schedule) {
        if (schedule.getEntryTime() == null || schedule.getToleranceLate() == null) {
            return 0;
        }

        LocalTime expectedEntry = schedule.getEntryTime();
        if (expectedEntry.isBefore(LocalTime.of(5, 0))) {
            return 0;
        }
        int tolerance = schedule.getToleranceLate();

        if (entryTime.isAfter(expectedEntry.plusMinutes(tolerance))) {
            return (int) java.time.Duration.between(
                    expectedEntry.plusMinutes(tolerance),
                    entryTime
            ).toMinutes();
        }

        return 0;
    }

    /**
     * Soma minutos trabalhados por dia: em cada data, FIFO de ENTRY→EXIT apenas entre batidas desse dia.
     */
    private static Map<LocalDate, Integer> buildWorkedMinutesPerDay(List<TimeEvent> sorted) {
        Map<LocalDate, List<TimeEvent>> byDate = sorted.stream()
                .collect(Collectors.groupingBy(e -> e.getEventTime().toLocalDate()));
        Map<LocalDate, Integer> workedByEntryDate = new HashMap<>();
        for (Map.Entry<LocalDate, List<TimeEvent>> e : byDate.entrySet()) {
            LocalDate date = e.getKey();
            List<TimeEvent> dayEvents = e.getValue().stream()
                    .sorted(Comparator.comparing(TimeEvent::getEventTime))
                    .collect(Collectors.toList());
            ArrayDeque<LocalDateTime> openEntries = new ArrayDeque<>();
            for (TimeEvent ev : dayEvents) {
                if (ev.getEventType() == TimeEventType.ENTRY) {
                    openEntries.addLast(ev.getEventTime());
                } else {
                    if (!openEntries.isEmpty()) {
                        LocalDateTime entryStart = openEntries.pollFirst();
                        int segment = (int) Duration.between(entryStart, ev.getEventTime()).toMinutes();
                        if (segment > 0) {
                            int capped = Math.min(segment, MAX_SEGMENT_MINUTES);
                            workedByEntryDate.merge(date, capped, Integer::sum);
                        }
                    }
                }
            }
        }
        return workedByEntryDate;
    }

    /**
     * Banco deve usar minutos (480 = 8h). Valores 1–24 são tratados como horas inteiras (ex.: 8 → 480).
     */
    static int normalizeDailyNormMinutes(Integer dailyHoursOrMinutes) {
        if (dailyHoursOrMinutes == null || dailyHoursOrMinutes <= 0) {
            return 480;
        }
        int v = dailyHoursOrMinutes;
        if (v >= 1 && v <= 24) {
            return v * 60;
        }
        return v;
    }

    /**
     * Mesmo minuto + mesmo tipo (import duplicado) dobraria trechos no dia.
     */
    private static List<TimeEvent> dedupeSameMinuteAndType(List<TimeEvent> sorted) {
        Set<String> seen = new LinkedHashSet<>();
        List<TimeEvent> out = new ArrayList<>(sorted.size());
        for (TimeEvent e : sorted) {
            LocalDateTime t = e.getEventTime();
            if (t == null) {
                continue;
            }
            String key = e.getEventType().name() + "|" + t.truncatedTo(ChronoUnit.MINUTES);
            if (seen.add(key)) {
                out.add(e);
            }
        }
        return out;
    }
}
