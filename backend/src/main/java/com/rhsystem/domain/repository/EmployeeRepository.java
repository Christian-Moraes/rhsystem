package com.rhsystem.domain.repository;

import com.rhsystem.domain.model.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(UUID id);
    List<Employee> findByCompanyId(UUID companyId);
    boolean existsByCpfAndCompanyId(String cpf, UUID companyId);
}
