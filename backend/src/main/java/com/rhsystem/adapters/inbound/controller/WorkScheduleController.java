package com.rhsystem.adapters.inbound.controller;

import com.rhsystem.application.dto.WorkScheduleRequest;
import com.rhsystem.application.dto.WorkScheduleResponse;
import com.rhsystem.application.usecase.WorkScheduleUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies/{companyId}/work-schedules")
public class WorkScheduleController {

    private final WorkScheduleUseCase workScheduleUseCase;

    public WorkScheduleController(WorkScheduleUseCase workScheduleUseCase) {
        this.workScheduleUseCase = workScheduleUseCase;
    }

    @GetMapping
    public ResponseEntity<List<WorkScheduleResponse>> getByCompany(
            @PathVariable UUID companyId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(workScheduleUseCase.getByCompany(userId, companyId));
    }

    @PostMapping
    public ResponseEntity<WorkScheduleResponse> create(
            @PathVariable UUID companyId,
            @Valid @RequestBody WorkScheduleRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(workScheduleUseCase.create(userId, companyId, request));
    }
}

