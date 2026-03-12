package com.rhsystem.adapters.outbound.persistence;

import com.rhsystem.adapters.outbound.persistence.entity.EmployeeEntity;
import com.rhsystem.adapters.outbound.persistence.repository.JpaEmployeeRepository;
import com.rhsystem.domain.model.Employee;
import com.rhsystem.domain.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final JpaEmployeeRepository jpaEmployeeRepository;

    public EmployeeRepositoryAdapter(JpaEmployeeRepository jpaEmployeeRepository) {
        this.jpaEmployeeRepository = jpaEmployeeRepository;
    }

    @Override
    public Employee save(Employee employee) {
        EmployeeEntity entity = EmployeeEntity.fromDomain(employee);
        EmployeeEntity saved = jpaEmployeeRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Employee> findById(UUID id) {
        return jpaEmployeeRepository.findById(id)
                .map(EmployeeEntity::toDomain);
    }

    @Override
    public List<Employee> findByCompanyId(UUID companyId) {
        return jpaEmployeeRepository.findByCompanyId(companyId)
                .stream()
                .map(EmployeeEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCpfAndCompanyId(String cpf, UUID companyId) {
        return jpaEmployeeRepository.existsByCpfAndCompanyId(cpf, companyId);
    }
}
