package com.rhsystem.domain.repository;

import com.rhsystem.domain.model.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findById(UUID id);
    List<Company> findByUserId(UUID userId);
    boolean existsByCnpj(String cnpj);
}
