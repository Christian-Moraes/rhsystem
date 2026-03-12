package com.rhsystem.adapters.inbound.controller;

import com.rhsystem.application.dto.EmployeeRequest;
import com.rhsystem.application.dto.EmployeeResponse;
import com.rhsystem.application.usecase.EmployeeUseCase;
import com.rhsystem.shared.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies/{companyId}/employees")
public class EmployeeController {

    private final EmployeeUseCase employeeUseCase;

    public EmployeeController(EmployeeUseCase employeeUseCase) {
        this.employeeUseCase = employeeUseCase;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @PathVariable UUID companyId,
            @Valid @RequestBody EmployeeRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        EmployeeResponse response = employeeUseCase.createEmployee(userId, companyId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees(
            @PathVariable UUID companyId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<EmployeeResponse> employees = employeeUseCase.getEmployeesByCompany(userId, companyId);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        EmployeeResponse employee = employeeUseCase.getEmployeeById(userId, companyId, id);
        return ResponseEntity.ok(employee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        EmployeeResponse response = employeeUseCase.updateEmployee(userId, companyId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        employeeUseCase.deleteEmployee(userId, companyId, id);
        return ResponseEntity.noContent().build();
    }
}
