package com.rhsystem.adapters.inbound.controller;

import com.rhsystem.application.dto.TimeEventRequest;
import com.rhsystem.application.dto.TimeEventResponse;
import com.rhsystem.application.usecase.TimeEventUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies/{companyId}/time-events")
public class TimeEventController {

    private final TimeEventUseCase timeEventUseCase;

    public TimeEventController(TimeEventUseCase timeEventUseCase) {
        this.timeEventUseCase = timeEventUseCase;
    }

    @PostMapping
    public ResponseEntity<TimeEventResponse> createTimeEvent(
            @PathVariable UUID companyId,
            @Valid @RequestBody TimeEventRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TimeEventResponse response = timeEventUseCase.createTimeEvent(userId, companyId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<TimeEventResponse>> getTimeEventsByEmployee(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<TimeEventResponse> events = timeEventUseCase.getTimeEventsByEmployee(userId, companyId, employeeId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/employee/{employeeId}/month/{month}/year/{year}")
    public ResponseEntity<List<TimeEventResponse>> getTimeEventsByEmployeeAndMonth(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @PathVariable int month,
            @PathVariable int year,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<TimeEventResponse> events = timeEventUseCase.getTimeEventsByEmployeeAndMonth(
                userId, companyId, employeeId, month, year);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeEventResponse> getTimeEvent(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TimeEventResponse event = timeEventUseCase.getTimeEventById(userId, companyId, id);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimeEventResponse> updateTimeEvent(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            @Valid @RequestBody TimeEventRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        TimeEventResponse response = timeEventUseCase.updateTimeEvent(userId, companyId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeEvent(
            @PathVariable UUID companyId,
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        timeEventUseCase.deleteTimeEvent(userId, companyId, id);
        return ResponseEntity.noContent().build();
    }
}
