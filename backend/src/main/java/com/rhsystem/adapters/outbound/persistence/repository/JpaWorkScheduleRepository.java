package com.rhsystem.adapters.outbound.persistence.repository;

import com.rhsystem.adapters.outbound.persistence.entity.WorkScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaWorkScheduleRepository extends JpaRepository<WorkScheduleEntity, UUID> {
    List<WorkScheduleEntity> findByCompanyId(UUID companyId);
}
