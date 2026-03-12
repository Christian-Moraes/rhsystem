package com.rhsystem.adapters.inbound.controller;

import com.rhsystem.application.dto.CompanyRequest;
import com.rhsystem.application.dto.CompanyResponse;
import com.rhsystem.application.usecase.CompanyUseCase;
import com.rhsystem.shared.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyUseCase companyUseCase;

    public CompanyController(CompanyUseCase companyUseCase) {
        this.companyUseCase = companyUseCase;
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(
            @Valid @RequestBody CompanyRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        CompanyResponse response = companyUseCase.createCompany(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getUserCompanies(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<CompanyResponse> companies = companyUseCase.getUserCompanies(userId);
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompany(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        CompanyResponse company = companyUseCase.getCompanyById(id, userId);
        return ResponseEntity.ok(company);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody CompanyRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        CompanyResponse response = companyUseCase.updateCompany(id, userId, request);
        return ResponseEntity.ok(response);
    }
}
