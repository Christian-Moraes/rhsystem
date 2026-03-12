package com.rhsystem.application.usecase;

import com.rhsystem.application.dto.EmployeeRequest;
import com.rhsystem.application.dto.EmployeeResponse;
import com.rhsystem.domain.model.Employee;
import com.rhsystem.domain.model.EmployeeStatus;
import com.rhsystem.domain.repository.CompanyRepository;
import com.rhsystem.domain.repository.EmployeeRepository;
import com.rhsystem.domain.repository.UserCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;

    public EmployeeUseCase(EmployeeRepository employeeRepository,
                          CompanyRepository companyRepository,
                          UserCompanyRepository userCompanyRepository) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    @Transactional
    public EmployeeResponse createEmployee(UUID userId, UUID companyId, EmployeeRequest request) {
        verifyUserAccess(userId, companyId);

        if (employeeRepository.existsByCpfAndCompanyId(request.getCpf(), companyId)) {
            throw new RuntimeException("CPF já cadastrado para esta empresa");
        }

        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setCompanyId(companyId);
        employee.setName(request.getName());
        employee.setCpf(request.getCpf());
        employee.setRole(request.getRole());
        employee.setHireDate(request.getHireDate());
        employee.setWorkScheduleId(request.getWorkScheduleId());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee = employeeRepository.save(employee);

        return toResponse(employee);
    }

    public List<EmployeeResponse> getEmployeesByCompany(UUID userId, UUID companyId) {
        verifyUserAccess(userId, companyId);
        return employeeRepository.findByCompanyId(companyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployeeById(UUID userId, UUID companyId, UUID employeeId) {
        verifyUserAccess(userId, companyId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (!employee.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Funcionário não pertence a esta empresa");
        }

        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID userId, UUID companyId, UUID employeeId, EmployeeRequest request) {
        verifyUserAccess(userId, companyId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (!employee.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Funcionário não pertence a esta empresa");
        }

        if (!employee.getCpf().equals(request.getCpf()) && 
            employeeRepository.existsByCpfAndCompanyId(request.getCpf(), companyId)) {
            throw new RuntimeException("CPF já cadastrado para esta empresa");
        }

        employee.setName(request.getName());
        employee.setCpf(request.getCpf());
        employee.setRole(request.getRole());
        employee.setHireDate(request.getHireDate());
        employee.setWorkScheduleId(request.getWorkScheduleId());
        employee = employeeRepository.save(employee);

        return toResponse(employee);
    }

    @Transactional
    public void deleteEmployee(UUID userId, UUID companyId, UUID employeeId) {
        verifyUserAccess(userId, companyId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (!employee.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Funcionário não pertence a esta empresa");
        }

        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);
    }

    private void verifyUserAccess(UUID userId, UUID companyId) {
        userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("Usuário não tem acesso a esta empresa"));
    }

    private EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setCompanyId(employee.getCompanyId());
        response.setName(employee.getName());
        response.setCpf(employee.getCpf());
        response.setRole(employee.getRole());
        response.setHireDate(employee.getHireDate());
        response.setWorkScheduleId(employee.getWorkScheduleId());
        response.setStatus(employee.getStatus().name());
        return response;
    }
}
