package com.rhsystem.adapters.outbound.persistence;

import com.rhsystem.adapters.outbound.persistence.entity.UserCompanyEntity;
import com.rhsystem.adapters.outbound.persistence.repository.JpaUserCompanyRepository;
import com.rhsystem.domain.model.UserCompany;
import com.rhsystem.domain.repository.UserCompanyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserCompanyRepositoryAdapter implements UserCompanyRepository {

    private final JpaUserCompanyRepository jpaUserCompanyRepository;

    public UserCompanyRepositoryAdapter(JpaUserCompanyRepository jpaUserCompanyRepository) {
        this.jpaUserCompanyRepository = jpaUserCompanyRepository;
    }

    @Override
    public UserCompany save(UserCompany userCompany) {
        UserCompanyEntity entity = UserCompanyEntity.fromDomain(userCompany);
        UserCompanyEntity saved = jpaUserCompanyRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<UserCompany> findByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return jpaUserCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
                .map(UserCompanyEntity::toDomain);
    }

    @Override
    public List<UserCompany> findByUserId(UUID userId) {
        return jpaUserCompanyRepository.findByUserId(userId)
                .stream()
                .map(UserCompanyEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserCompany> findByCompanyId(UUID companyId) {
        return jpaUserCompanyRepository.findByCompanyId(companyId)
                .stream()
                .map(UserCompanyEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        jpaUserCompanyRepository.deleteById(id);
    }
}
