package com.rhsystem.adapters.inbound.controller;

import com.rhsystem.application.dto.TimesheetResponse;
import com.rhsystem.application.usecase.TimesheetUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies/{companyId}/timesheets")
public class TimesheetController {

    private final TimesheetUseCase timesheetUseCase;

    public TimesheetController(TimesheetUseCase timesheetUseCase) {
        this.timesheetUseCase = timesheetUseCase;
    }

    @PostMapping("/employee/{employeeId}/month/{month}/year/{year}")
    public ResponseEntity<TimesheetResponse> closeTimesheet(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @PathVariable int month,
            @PathVariable int year,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TimesheetResponse response = timesheetUseCase.closeTimesheet(userId, companyId, employeeId, month, year);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/{employeeId}/month/{month}/year/{year}")
    public ResponseEntity<TimesheetResponse> getTimesheet(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @PathVariable int month,
            @PathVariable int year,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TimesheetResponse response = timesheetUseCase.getTimesheet(userId, companyId, employeeId, month, year);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/month/{month}/year/{year}")
    public ResponseEntity<List<TimesheetResponse>> getTimesheetsByCompany(
            @PathVariable UUID companyId,
            @PathVariable int month,
            @PathVariable int year,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<TimesheetResponse> timesheets = timesheetUseCase.getTimesheetsByCompany(userId, companyId, month, year);
        return ResponseEntity.ok(timesheets);
    }
}
