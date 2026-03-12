package com.rhsystem.adapters.outbound.persistence.repository;

import com.rhsystem.adapters.outbound.persistence.entity.UserCompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserCompanyRepository extends JpaRepository<UserCompanyEntity, UUID> {
    Optional<UserCompanyEntity> findByUserIdAndCompanyId(UUID userId, UUID companyId);
    List<UserCompanyEntity> findByUserId(UUID userId);
    List<UserCompanyEntity> findByCompanyId(UUID companyId);
}
