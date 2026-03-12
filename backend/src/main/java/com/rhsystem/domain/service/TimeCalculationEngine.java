package com.rhsystem.domain.service;

import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.TimeEventType;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.valueobject.TimeCalculationResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
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

        // Agrupa eventos por data
        Map<LocalDate, List<TimeEvent>> eventsByDate = events.stream()
                .collect(Collectors.groupingBy(event -> event.getEventTime().toLocalDate()));

        int totalWorkedMinutes = 0;
        int totalExtraMinutes = 0;
        int totalDelayMinutes = 0;
        int totalAbsenceMinutes = 0;

        for (Map.Entry<LocalDate, List<TimeEvent>> entry : eventsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<TimeEvent> dayEvents = entry.getValue().stream()
                    .sorted(Comparator.comparing(TimeEvent::getEventTime))
                    .collect(Collectors.toList());

            DayCalculation dayCalc = calculateDay(dayEvents, workSchedule, date);
            
            totalWorkedMinutes += dayCalc.workedMinutes;
            totalExtraMinutes += dayCalc.extraMinutes;
            totalDelayMinutes += dayCalc.delayMinutes;
            totalAbsenceMinutes += dayCalc.absenceMinutes;
        }

        return new TimeCalculationResult(
                totalWorkedMinutes,
                totalExtraMinutes,
                totalDelayMinutes,
                totalAbsenceMinutes
        );
    }

    private DayCalculation calculateDay(List<TimeEvent> events, WorkSchedule schedule, LocalDate date) {
        LocalTime entryTime = null;
        LocalTime breakStartTime = null;
        LocalTime breakEndTime = null;
        LocalTime exitTime = null;

        // Processa eventos em ordem cronológica
        for (TimeEvent event : events) {
            switch (event.getEventType()) {
                case ENTRY:
                    entryTime = event.getEventTime().toLocalTime();
                    break;
                case BREAK_START:
                    breakStartTime = event.getEventTime().toLocalTime();
                    break;
                case BREAK_END:
                    breakEndTime = event.getEventTime().toLocalTime();
                    break;
                case EXIT:
                    exitTime = event.getEventTime().toLocalTime();
                    break;
            }
        }

        // Se não há entrada ou saída, considera ausência
        if (entryTime == null || exitTime == null) {
            int absenceMinutes = schedule.getDailyHours() != null ? schedule.getDailyHours() : 480; // 8 horas padrão
            return new DayCalculation(0, 0, 0, absenceMinutes);
        }

        // Calcula tempo trabalhado
        int workedMinutes = calculateWorkedMinutes(entryTime, exitTime, breakStartTime, breakEndTime);
        
        // Calcula atraso
        int delayMinutes = calculateDelay(entryTime, schedule);
        
        // Calcula horas extras
        int expectedMinutes = schedule.getDailyHours() != null ? schedule.getDailyHours() : 480;
        int extraMinutes = Math.max(0, workedMinutes - expectedMinutes);
        
        // Ajusta horas trabalhadas se houver horas extras
        int adjustedWorkedMinutes = workedMinutes;
        if (extraMinutes > 0) {
            adjustedWorkedMinutes = expectedMinutes;
        }

        return new DayCalculation(adjustedWorkedMinutes, extraMinutes, delayMinutes, 0);
    }

    private int calculateWorkedMinutes(LocalTime entry, LocalTime exit, 
                                       LocalTime breakStart, LocalTime breakEnd) {
        int totalMinutes = (int) java.time.Duration.between(entry, exit).toMinutes();
        
        // Subtrai o tempo de intervalo
        if (breakStart != null && breakEnd != null) {
            int breakMinutes = (int) java.time.Duration.between(breakStart, breakEnd).toMinutes();
            totalMinutes -= breakMinutes;
        }
        
        return Math.max(0, totalMinutes);
    }

    private int calculateDelay(LocalTime entryTime, WorkSchedule schedule) {
        if (schedule.getEntryTime() == null || schedule.getToleranceLate() == null) {
            return 0;
        }

        LocalTime expectedEntry = schedule.getEntryTime();
        int tolerance = schedule.getToleranceLate();
        
        if (entryTime.isAfter(expectedEntry.plusMinutes(tolerance))) {
            return (int) java.time.Duration.between(
                    expectedEntry.plusMinutes(tolerance), 
                    entryTime
            ).toMinutes();
        }
        
        return 0;
    }

    private static class DayCalculation {
        final int workedMinutes;
        final int extraMinutes;
        final int delayMinutes;
        final int absenceMinutes;

        DayCalculation(int workedMinutes, int extraMinutes, int delayMinutes, int absenceMinutes) {
            this.workedMinutes = workedMinutes;
            this.extraMinutes = extraMinutes;
            this.delayMinutes = delayMinutes;
            this.absenceMinutes = absenceMinutes;
        }
    }
}
