package com.rhsystem.adapters.outbound.persistence;

import com.rhsystem.adapters.outbound.persistence.entity.WorkScheduleEntity;
import com.rhsystem.adapters.outbound.persistence.repository.JpaWorkScheduleRepository;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.repository.WorkScheduleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class WorkScheduleRepositoryAdapter implements WorkScheduleRepository {

    private final JpaWorkScheduleRepository jpaWorkScheduleRepository;

    public WorkScheduleRepositoryAdapter(JpaWorkScheduleRepository jpaWorkScheduleRepository) {
        this.jpaWorkScheduleRepository = jpaWorkScheduleRepository;
    }

    @Override
    public WorkSchedule save(WorkSchedule workSchedule) {
        WorkScheduleEntity entity = WorkScheduleEntity.fromDomain(workSchedule);
        WorkScheduleEntity saved = jpaWorkScheduleRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<WorkSchedule> findById(UUID id) {
        return jpaWorkScheduleRepository.findById(id)
                .map(WorkScheduleEntity::toDomain);
    }

    @Override
    public List<WorkSchedule> findByCompanyId(UUID companyId) {
        return jpaWorkScheduleRepository.findByCompanyId(companyId)
                .stream()
                .map(WorkScheduleEntity::toDomain)
                .collect(Collectors.toList());
    }
}
