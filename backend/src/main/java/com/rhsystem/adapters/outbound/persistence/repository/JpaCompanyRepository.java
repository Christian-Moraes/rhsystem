package com.rhsystem.adapters.outbound.persistence.repository;

import com.rhsystem.adapters.outbound.persistence.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCompanyRepository extends JpaRepository<CompanyEntity, UUID> {
    boolean existsByCnpj(String cnpj);
    
    @Query("SELECT c FROM CompanyEntity c JOIN UserCompanyEntity uc ON c.id = uc.companyId WHERE uc.userId = :userId")
    List<CompanyEntity> findByUserId(@Param("userId") UUID userId);
}
