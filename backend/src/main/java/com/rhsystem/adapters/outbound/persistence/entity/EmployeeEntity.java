package com.rhsystem.adapters.outbound.persistence.entity;

import com.rhsystem.domain.model.Employee;
import com.rhsystem.domain.model.EmployeeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"company_id", "cpf"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 14)
    private String cpf;

    private String role;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "work_schedule_id")
    private UUID workScheduleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status;

    public Employee toDomain() {
        return new Employee(id, companyId, name, cpf, role, hireDate, workScheduleId, status);
    }

    public static EmployeeEntity fromDomain(Employee employee) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setId(employee.getId());
        entity.setCompanyId(employee.getCompanyId());
        entity.setName(employee.getName());
        entity.setCpf(employee.getCpf());
        entity.setRole(employee.getRole());
        entity.setHireDate(employee.getHireDate());
        entity.setWorkScheduleId(employee.getWorkScheduleId());
        entity.setStatus(employee.getStatus());
        return entity;
    }
}
