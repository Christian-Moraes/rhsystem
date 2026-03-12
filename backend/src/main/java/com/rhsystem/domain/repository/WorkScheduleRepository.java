package com.rhsystem.domain.repository;

import com.rhsystem.domain.model.WorkSchedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkScheduleRepository {
    WorkSchedule save(WorkSchedule workSchedule);
    Optional<WorkSchedule> findById(UUID id);
    List<WorkSchedule> findByCompanyId(UUID companyId);
}
