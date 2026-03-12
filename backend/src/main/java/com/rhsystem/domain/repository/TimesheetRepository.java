package com.rhsystem.domain.repository;

import com.rhsystem.domain.model.Timesheet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetRepository {
    Timesheet save(Timesheet timesheet);
    Optional<Timesheet> findByEmployeeIdAndMonthAndYear(UUID employeeId, int month, int year);
    List<Timesheet> findByEmployeeId(UUID employeeId);
    List<Timesheet> findByCompanyId(UUID companyId, int month, int year);
}
