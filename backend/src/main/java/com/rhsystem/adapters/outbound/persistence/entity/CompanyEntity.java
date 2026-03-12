package com.rhsystem.adapters.outbound.persistence.entity;

import com.rhsystem.domain.model.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(name = "responsible_name", nullable = false)
    private String responsibleName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "enable_overtime", nullable = false)
    private Boolean enableOvertime;

    @Column(name = "enable_time_bank", nullable = false)
    private Boolean enableTimeBank;

    @Column(name = "overtime_percentage", nullable = false)
    private Integer overtimePercentage;

    @Column(name = "weekend_percentage", nullable = false)
    private Integer weekendPercentage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Company toDomain() {
        return new Company(id, name, cnpj, responsibleName, email, phone, 
                          enableOvertime, enableTimeBank, overtimePercentage, 
                          weekendPercentage, createdAt);
    }

    public static CompanyEntity fromDomain(Company company) {
        CompanyEntity entity = new CompanyEntity();
        entity.setId(company.getId());
        entity.setName(company.getName());
        entity.setCnpj(company.getCnpj());
        entity.setResponsibleName(company.getResponsibleName());
        entity.setEmail(company.getEmail());
        entity.setPhone(company.getPhone());
        entity.setEnableOvertime(company.getEnableOvertime());
        entity.setEnableTimeBank(company.getEnableTimeBank());
        entity.setOvertimePercentage(company.getOvertimePercentage());
        entity.setWeekendPercentage(company.getWeekendPercentage());
        entity.setCreatedAt(company.getCreatedAt());
        return entity;
    }
}
