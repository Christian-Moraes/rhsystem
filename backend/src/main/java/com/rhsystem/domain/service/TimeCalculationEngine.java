package com.rhsystem.domain.service;

import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.TimeEventType;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.valueobject.TimeCalculationResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class TimeCalculationEngine {

    public TimeCalculationResult calculate(List<TimeEvent> events, WorkSchedule workSchedule) {
        if (events == null || events.isEmpty()) {
            return new TimeCalculationResult(0, 0, 0, 0);
        }

        if (workSchedule == null) {
            throw new IllegalArgumentException("WorkSchedule não pode ser nulo");
        }

        int dailyMinutes = workSchedule.getDailyHours() != null ? workSchedule.getDailyHours() : 480;

        // FIFO em todo o período: saída após meia-noite fica gravada no dia civil seguinte;
        // parear só dentro do mesmo dia deixava E2 "sem saída" e a última saída antes da última entrada → 0h trabalhadas e atraso absurdo.
        List<TimeEvent> sorted = events.stream()
                .filter(e -> e.getEventType() == TimeEventType.ENTRY || e.getEventType() == TimeEventType.EXIT)
                .sorted(Comparator.comparing(TimeEvent::getEventTime))
                .collect(Collectors.toList());

        ArrayDeque<LocalDateTime> openEntries = new ArrayDeque<>();
        Map<LocalDate, Integer> workedByEntryDate = new HashMap<>();
        for (TimeEvent ev : sorted) {
            if (ev.getEventType() == TimeEventType.ENTRY) {
                openEntries.addLast(ev.getEventTime());
            } else {
                if (!openEntries.isEmpty()) {
                    LocalDateTime entryStart = openEntries.pollFirst();
                    int segment = (int) Duration.between(entryStart, ev.getEventTime()).toMinutes();
                    if (segment > 0) {
                        LocalDate attributionDate = entryStart.toLocalDate();
                        workedByEntryDate.merge(attributionDate, segment, Integer::sum);
                    }
                }
            }
        }

        Map<LocalDate, List<TimeEvent>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(e -> e.getEventTime().toLocalDate()));

        TreeSet<LocalDate> datesToProcess = new TreeSet<>(eventsByDate.keySet());
        datesToProcess.addAll(workedByEntryDate.keySet());

        int totalWorkedMinutes = 0;
        int totalExtraMinutes = 0;
        int totalDelayMinutes = 0;
        int totalAbsenceMinutes = 0;

        for (LocalDate date : datesToProcess) {
            int workedDay = workedByEntryDate.getOrDefault(date, 0);

            LocalTime firstEntryOfDay = eventsByDate.getOrDefault(date, List.of()).stream()
                    .filter(e -> e.getEventType() == TimeEventType.ENTRY)
                    .min(Comparator.comparing(TimeEvent::getEventTime))
                    .map(e -> e.getEventTime().toLocalTime())
                    .orElse(null);

            if (workedDay <= 0) {
                totalAbsenceMinutes += dailyMinutes;
                continue;
            }

            int delayMinutes = firstEntryOfDay != null ? calculateDelay(firstEntryOfDay, workSchedule) : 0;
            int extraMinutes = Math.max(0, workedDay - dailyMinutes);
            int adjustedWorked = workedDay;
            if (extraMinutes > 0) {
                adjustedWorked = dailyMinutes;
            }

            totalWorkedMinutes += adjustedWorked;
            totalExtraMinutes += extraMinutes;
            totalDelayMinutes += delayMinutes;
        }

        return new TimeCalculationResult(
                totalWorkedMinutes,
                totalExtraMinutes,
                totalDelayMinutes,
                totalAbsenceMinutes
        );
    }

    private int calculateDelay(LocalTime entryTime, WorkSchedule schedule) {
        if (schedule.getEntryTime() == null || schedule.getToleranceLate() == null) {
            return 0;
        }

        LocalTime expectedEntry = schedule.getEntryTime();
        // Horário de entrada antes das 05:00 indica escala mal configurada (ex.: 00:00).
        // Nesse caso não calculamos atraso para evitar valores absurdos (ex.: 14h por dia).
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

}
