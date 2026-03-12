package com.rhsystem.adapters.inbound.controller;

import com.rhsystem.application.dto.TimeImportResponse;
import com.rhsystem.application.usecase.TimeImportUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies/{companyId}/time-import")
public class TimeImportController {

    private final TimeImportUseCase timeImportUseCase;

    public TimeImportController(TimeImportUseCase timeImportUseCase) {
        this.timeImportUseCase = timeImportUseCase;
    }

    @PostMapping("/employee/{employeeId}")
    public ResponseEntity<TimeImportResponse> importTimeEvents(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "quinzena", required = false) Integer quinzena,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TimeImportResponse response = timeImportUseCase.processTimeImport(
                userId, companyId, employeeId, file, fileType, month, year, quinzena);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/employee/{employeeId}/demo")
    public ResponseEntity<TimeImportResponse> createDemoEvents(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestParam int month,
            @RequestParam int year,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TimeImportResponse response = timeImportUseCase.createDemoEventsForMonth(
                userId, companyId, employeeId, month, year);
        return ResponseEntity.ok(response);
    }

    /** GET para seed manual: abre no navegador (com login) para criar eventos de exemplo. Útil quando POST pelo front retorna [] */
    @GetMapping("/employee/{employeeId}/seed")
    public ResponseEntity<TimeImportResponse> seedDemoEvents(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @RequestParam int month,
            @RequestParam int year,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TimeImportResponse response = timeImportUseCase.createDemoEventsForMonth(
                userId, companyId, employeeId, month, year);
        return ResponseEntity.ok(response);
    }
}
