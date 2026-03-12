package com.rhsystem.adapters.outbound.persistence;

import com.rhsystem.adapters.outbound.persistence.entity.CompanyEntity;
import com.rhsystem.adapters.outbound.persistence.repository.JpaCompanyRepository;
import com.rhsystem.domain.model.Company;
import com.rhsystem.domain.repository.CompanyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CompanyRepositoryAdapter implements CompanyRepository {

    private final JpaCompanyRepository jpaCompanyRepository;

    public CompanyRepositoryAdapter(JpaCompanyRepository jpaCompanyRepository) {
        this.jpaCompanyRepository = jpaCompanyRepository;
    }

    @Override
    public Company save(Company company) {
        CompanyEntity entity = CompanyEntity.fromDomain(company);
        CompanyEntity saved = jpaCompanyRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return jpaCompanyRepository.findById(id)
                .map(CompanyEntity::toDomain);
    }

    @Override
    public List<Company> findByUserId(UUID userId) {
        return jpaCompanyRepository.findByUserId(userId)
                .stream()
                .map(CompanyEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCnpj(String cnpj) {
        return jpaCompanyRepository.existsByCnpj(cnpj);
    }
}
