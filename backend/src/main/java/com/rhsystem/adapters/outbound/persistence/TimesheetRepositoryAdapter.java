package com.rhsystem.adapters.outbound.persistence;

import com.rhsystem.adapters.outbound.persistence.entity.TimesheetEntity;
import com.rhsystem.adapters.outbound.persistence.repository.JpaTimesheetRepository;
import com.rhsystem.domain.model.Timesheet;
import com.rhsystem.domain.repository.TimesheetRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TimesheetRepositoryAdapter implements TimesheetRepository {

    private final JpaTimesheetRepository jpaTimesheetRepository;

    public TimesheetRepositoryAdapter(JpaTimesheetRepository jpaTimesheetRepository) {
        this.jpaTimesheetRepository = jpaTimesheetRepository;
    }

    @Override
    public Timesheet save(Timesheet timesheet) {
        TimesheetEntity entity = TimesheetEntity.fromDomain(timesheet);
        TimesheetEntity saved = jpaTimesheetRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Timesheet> findByEmployeeIdAndMonthAndYear(UUID employeeId, int month, int year) {
        return jpaTimesheetRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .map(TimesheetEntity::toDomain);
    }

    @Override
    public List<Timesheet> findByEmployeeId(UUID employeeId) {
        return jpaTimesheetRepository.findByEmployeeId(employeeId)
                .stream()
                .map(TimesheetEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Timesheet> findByCompanyId(UUID companyId, int month, int year) {
        return jpaTimesheetRepository.findByCompanyIdAndMonthAndYear(companyId, month, year)
                .stream()
                .map(TimesheetEntity::toDomain)
                .collect(Collectors.toList());
    }
}
