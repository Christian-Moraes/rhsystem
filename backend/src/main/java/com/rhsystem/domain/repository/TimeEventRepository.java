package com.rhsystem.domain.repository;

import com.rhsystem.domain.model.TimeEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeEventRepository {
    TimeEvent save(TimeEvent timeEvent);
    Optional<TimeEvent> findById(UUID id);
    List<TimeEvent> findByEmployeeId(UUID employeeId);
    List<TimeEvent> findByEmployeeIdAndDateRange(UUID employeeId, LocalDateTime startDate, LocalDateTime endDate);
    List<TimeEvent> findByEmployeeIdAndMonth(UUID employeeId, int month, int year);
    void delete(UUID id);
}
