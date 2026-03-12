package com.rhsystem.domain.repository;

import com.rhsystem.domain.model.UserCompany;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCompanyRepository {
    UserCompany save(UserCompany userCompany);
    Optional<UserCompany> findByUserIdAndCompanyId(UUID userId, UUID companyId);
    List<UserCompany> findByUserId(UUID userId);
    List<UserCompany> findByCompanyId(UUID companyId);
    void delete(UUID id);
}
