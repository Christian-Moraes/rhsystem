package com.rhsystem.adapters.inbound.controller;

import com.rhsystem.application.dto.ClosureBreakdownResponse;
import com.rhsystem.application.dto.TimesheetResponse;
import com.rhsystem.application.usecase.TimesheetUseCase;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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

    /** Detalhamento para exportação (planilha); mesmo cálculo do fechamento. */
    @GetMapping("/employee/{employeeId}/month/{month}/year/{year}/closure-breakdown")
    public ResponseEntity<ClosureBreakdownResponse> getClosureBreakdown(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @PathVariable int month,
            @PathVariable int year,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        ClosureBreakdownResponse response = timesheetUseCase.getClosureBreakdown(userId, companyId, employeeId, month, year);
        return ResponseEntity.ok(response);
    }

    /**
     * Arquivo CSV ou Excel (.xls XML) gerado no servidor: batidas E/S, totais e texto das regras.
     *
     * @param format {@code xls} (padrão) ou {@code csv}
     */
    @GetMapping("/employee/{employeeId}/month/{month}/year/{year}/export")
    public ResponseEntity<byte[]> exportClosure(
            @PathVariable UUID companyId,
            @PathVariable UUID employeeId,
            @PathVariable int month,
            @PathVariable int year,
            @RequestParam(defaultValue = "xls") String format,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        byte[] body = timesheetUseCase.exportClosureReport(userId, companyId, employeeId, month, year, format);
        boolean csv = "csv".equalsIgnoreCase(format != null ? format.trim() : "");
        String ext = csv ? "csv" : "xls";
        ContentDisposition cd = ContentDisposition.attachment()
                .filename("fechamento_" + year + "-" + String.format("%02d", month) + "." + ext, StandardCharsets.UTF_8)
                .build();
        MediaType mediaType = csv
                ? MediaType.parseMediaType("text/csv;charset=UTF-8")
                : MediaType.parseMediaType("application/vnd.ms-excel;charset=UTF-8");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(mediaType)
                .body(body);
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
