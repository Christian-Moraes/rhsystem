package com.rhsystem.adapters.outbound.persistence;

import com.rhsystem.adapters.outbound.persistence.entity.TimeEventEntity;
import com.rhsystem.adapters.outbound.persistence.repository.JpaTimeEventRepository;
import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.repository.TimeEventRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TimeEventRepositoryAdapter implements TimeEventRepository {

    private final JpaTimeEventRepository jpaTimeEventRepository;

    public TimeEventRepositoryAdapter(JpaTimeEventRepository jpaTimeEventRepository) {
        this.jpaTimeEventRepository = jpaTimeEventRepository;
    }

    @Override
    public TimeEvent save(TimeEvent timeEvent) {
        TimeEventEntity entity = TimeEventEntity.fromDomain(timeEvent);
        TimeEventEntity saved = jpaTimeEventRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<TimeEvent> findById(UUID id) {
        return jpaTimeEventRepository.findById(id)
                .map(TimeEventEntity::toDomain);
    }

    @Override
    public List<TimeEvent> findByEmployeeId(UUID employeeId) {
        return jpaTimeEventRepository.findByEmployeeIdOrderByEventTimeAsc(employeeId)
                .stream()
                .map(TimeEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeEvent> findByEmployeeIdAndDateRange(UUID employeeId, LocalDateTime startDate, LocalDateTime endDate) {
        return jpaTimeEventRepository.findByEmployeeIdAndDateRange(employeeId, startDate, endDate)
                .stream()
                .map(TimeEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeEvent> findByEmployeeIdAndMonth(UUID employeeId, int month, int year) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);
        return jpaTimeEventRepository.findByEmployeeIdAndDateRange(employeeId, start, end)
                .stream()
                .map(TimeEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        jpaTimeEventRepository.deleteById(id);
    }
}
