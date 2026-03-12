package com.rhsystem.application.usecase;

import com.rhsystem.application.dto.TimesheetResponse;
import com.rhsystem.domain.model.Employee;
import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.Timesheet;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.repository.*;
import com.rhsystem.domain.service.TimeCalculationEngine;
import com.rhsystem.domain.valueobject.TimeCalculationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TimesheetUseCase {

    private final TimesheetRepository timesheetRepository;
    private final TimeEventRepository timeEventRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final TimeCalculationEngine timeCalculationEngine;

    public TimesheetUseCase(TimesheetRepository timesheetRepository,
                            TimeEventRepository timeEventRepository,
                            EmployeeRepository employeeRepository,
                            WorkScheduleRepository workScheduleRepository,
                            UserCompanyRepository userCompanyRepository,
                            TimeCalculationEngine timeCalculationEngine) {
        this.timesheetRepository = timesheetRepository;
        this.timeEventRepository = timeEventRepository;
        this.employeeRepository = employeeRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.timeCalculationEngine = timeCalculationEngine;
    }

    @Transactional
    public TimesheetResponse closeTimesheet(UUID userId, UUID companyId, UUID employeeId, int month, int year) {
        verifyEmployeeAccess(userId, companyId, employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        List<TimeEvent> events = timeEventRepository.findByEmployeeIdAndMonth(employeeId, month, year);

        WorkSchedule workSchedule = employee.getWorkScheduleId() != null
                ? workScheduleRepository.findById(employee.getWorkScheduleId())
                        .orElseThrow(() -> new RuntimeException("Escala de trabalho não encontrada"))
                : workScheduleRepository.findByCompanyId(companyId).stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Escala de trabalho não encontrada"));

        TimeCalculationResult result = timeCalculationEngine.calculate(events, workSchedule);

        Timesheet timesheet = timesheetRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElse(new Timesheet());

        timesheet.setId(timesheet.getId() != null ? timesheet.getId() : UUID.randomUUID());
        timesheet.setEmployeeId(employeeId);
        timesheet.setMonth(month);
        timesheet.setYear(year);
        timesheet.setWorkedHours(result.getWorkedHours());
        timesheet.setExtraHours(result.getExtraHours());
        timesheet.setDelayMinutes(result.getDelayMinutes());
        timesheet.setAbsenceMinutes(result.getAbsenceMinutes());
        timesheet.setClosedAt(LocalDateTime.now());
        timesheet = timesheetRepository.save(timesheet);

        return toResponse(timesheet, employee.getName());
    }

    public TimesheetResponse getTimesheet(UUID userId, UUID companyId, UUID employeeId, int month, int year) {
        verifyEmployeeAccess(userId, companyId, employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        Timesheet timesheet = timesheetRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElse(null);

        if (timesheet == null) {
            throw new RuntimeException("Timesheet não encontrado. Feche o mês primeiro.");
        }

        return toResponse(timesheet, employee.getName());
    }

    public List<TimesheetResponse> getTimesheetsByCompany(UUID userId, UUID companyId, int month, int year) {
        userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("Usuário não tem acesso a esta empresa"));

        List<Timesheet> timesheets = timesheetRepository.findByCompanyId(companyId, month, year);

        return timesheets.stream()
                .map(ts -> {
                    Employee employee = employeeRepository.findById(ts.getEmployeeId())
                            .orElse(null);
                    return toResponse(ts, employee != null ? employee.getName() : "Desconhecido");
                })
                .collect(Collectors.toList());
    }

    private void verifyEmployeeAccess(UUID userId, UUID companyId, UUID employeeId) {
        userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("Usuário não tem acesso a esta empresa"));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        if (!employee.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Funcionário não pertence a esta empresa");
        }
    }

    private TimesheetResponse toResponse(Timesheet timesheet, String employeeName) {
        TimesheetResponse response = new TimesheetResponse();
        response.setId(timesheet.getId());
        response.setEmployeeId(timesheet.getEmployeeId());
        response.setEmployeeName(employeeName);
        response.setMonth(timesheet.getMonth());
        response.setYear(timesheet.getYear());
        response.setWorkedHours(timesheet.getWorkedHours());
        response.setExtraHours(timesheet.getExtraHours());
        response.setDelayMinutes(timesheet.getDelayMinutes());
        response.setAbsenceMinutes(timesheet.getAbsenceMinutes());
        response.setClosedAt(timesheet.getClosedAt());
        return response;
    }
}
