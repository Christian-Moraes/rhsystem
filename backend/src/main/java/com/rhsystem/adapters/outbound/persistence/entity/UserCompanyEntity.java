package com.rhsystem.adapters.outbound.persistence.entity;

import com.rhsystem.domain.model.UserCompany;
import com.rhsystem.domain.model.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_companies", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "company_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserCompany toDomain() {
        return new UserCompany(id, userId, companyId, role, createdAt);
    }

    public static UserCompanyEntity fromDomain(UserCompany userCompany) {
        UserCompanyEntity entity = new UserCompanyEntity();
        entity.setId(userCompany.getId());
        entity.setUserId(userCompany.getUserId());
        entity.setCompanyId(userCompany.getCompanyId());
        entity.setRole(userCompany.getRole());
        entity.setCreatedAt(userCompany.getCreatedAt());
        return entity;
    }
}
