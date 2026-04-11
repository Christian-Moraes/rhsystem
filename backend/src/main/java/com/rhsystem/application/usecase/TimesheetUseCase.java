package com.rhsystem.application.usecase;

import com.rhsystem.application.dto.ClosureBreakdownResponse;
import com.rhsystem.application.dto.TimesheetResponse;
import com.rhsystem.application.service.ClosureExportService;
import com.rhsystem.domain.model.Company;
import com.rhsystem.domain.model.Employee;
import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.TimeEventType;
import com.rhsystem.domain.model.Timesheet;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.repository.*;
import com.rhsystem.domain.service.TimeCalculationEngine;
import com.rhsystem.domain.valueobject.DailyBreakdownLine;
import com.rhsystem.domain.valueobject.TimeCalculationDetailResult;
import com.rhsystem.domain.valueobject.TimeCalculationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TimesheetUseCase {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final TimesheetRepository timesheetRepository;
    private final TimeEventRepository timeEventRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final CompanyRepository companyRepository;
    private final TimeCalculationEngine timeCalculationEngine;
    private final ClosureExportService closureExportService;

    public TimesheetUseCase(TimesheetRepository timesheetRepository,
                            TimeEventRepository timeEventRepository,
                            EmployeeRepository employeeRepository,
                            WorkScheduleRepository workScheduleRepository,
                            UserCompanyRepository userCompanyRepository,
                            CompanyRepository companyRepository,
                            TimeCalculationEngine timeCalculationEngine,
                            ClosureExportService closureExportService) {
        this.timesheetRepository = timesheetRepository;
        this.timeEventRepository = timeEventRepository;
        this.employeeRepository = employeeRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.companyRepository = companyRepository;
        this.timeCalculationEngine = timeCalculationEngine;
        this.closureExportService = closureExportService;
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

    /**
     * Detalhamento para exportação — mesma escala, eventos e motor que {@link #closeTimesheet}.
     * Não exige mês fechado (reflete cálculo atual).
     */
    public ClosureBreakdownResponse getClosureBreakdown(UUID userId, UUID companyId, UUID employeeId, int month, int year) {
        return closureBreakdownModel(userId, companyId, employeeId, month, year);
    }

    /**
     * CSV ou planilha Excel (.xls XML) gerados no servidor.
     *
     * @param format {@code xls} (padrão) ou {@code csv}
     */
    public byte[] exportClosureReport(UUID userId, UUID companyId, UUID employeeId, int month, int year, String format) {
        ClosureBreakdownResponse model = closureBreakdownModel(userId, companyId, employeeId, month, year);
        String f = format != null ? format.trim().toLowerCase(Locale.ROOT) : "xls";
        if ("csv".equals(f)) {
            return closureExportService.toCsv(model);
        }
        return closureExportService.toSpreadsheetXml(model);
    }

    private ClosureBreakdownResponse closureBreakdownModel(UUID userId, UUID companyId, UUID employeeId, int month, int year) {
        verifyEmployeeAccess(userId, companyId, employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        List<TimeEvent> events = timeEventRepository.findByEmployeeIdAndMonth(employeeId, month, year);

        WorkSchedule workSchedule = employee.getWorkScheduleId() != null
                ? workScheduleRepository.findById(employee.getWorkScheduleId())
                        .orElseThrow(() -> new RuntimeException("Escala de trabalho não encontrada"))
                : workScheduleRepository.findByCompanyId(companyId).stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Escala de trabalho não encontrada"));

        TimeCalculationDetailResult detail = timeCalculationEngine.calculateDetailed(events, workSchedule);
        TimeCalculationResult totals = detail.getTotals();

        Map<String, List<TimeEvent>> eventsByDate = events.stream()
                .filter(e -> e.getEventTime() != null)
                .collect(Collectors.groupingBy(e -> e.getEventTime().toLocalDate().toString()));

        ClosureBreakdownResponse response = new ClosureBreakdownResponse();
        response.setCompanyName(company.getName());
        response.setCompanyEnableOvertime(company.getEnableOvertime());
        response.setCompanyEnableTimeBank(company.getEnableTimeBank());
        response.setCompanyOvertimePercentage(company.getOvertimePercentage());
        response.setCompanyWeekendPercentage(company.getWeekendPercentage());

        response.setEmployeeName(employee.getName());
        response.setMonth(month);
        response.setYear(year);
        response.setScheduleDailyNormMinutes(detail.getDailyNormMinutes());
        if (workSchedule.getEntryTime() != null) {
            response.setScheduleEntryTime(workSchedule.getEntryTime().format(HM));
        }
        if (workSchedule.getExitTime() != null) {
            response.setScheduleExitTime(workSchedule.getExitTime().format(HM));
        }
        response.setScheduleBreakMinutes(workSchedule.getBreakMinutes());
        response.setScheduleToleranceLate(workSchedule.getToleranceLate());
        response.setScheduleToleranceLeave(workSchedule.getToleranceLeave());

        List<ClosureBreakdownResponse.DailyLine> lines = detail.getDailyLines().stream()
                .map(line -> toDailyLineDto(line, eventsByDate.getOrDefault(line.date().toString(), List.of())))
                .collect(Collectors.toList());
        response.setDailyLines(lines);

        response.setCalculatedWorkedMinutes(totals.getWorkedHours());
        response.setCalculatedExtraMinutes(totals.getExtraHours());
        response.setCalculatedDelayMinutes(totals.getDelayMinutes());
        response.setCalculatedAbsenceMinutes(totals.getAbsenceMinutes());

        timesheetRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year).ifPresent(ts -> {
            if (ts.getClosedAt() != null) {
                response.setClosedAt(ts.getClosedAt().toString());
            }
            response.setStoredWorkedMinutes(ts.getWorkedHours());
            response.setStoredExtraMinutes(ts.getExtraHours());
            response.setStoredDelayMinutes(ts.getDelayMinutes());
            response.setStoredAbsenceMinutes(ts.getAbsenceMinutes());
        });

        response.setFormulaDocumentation(formulaDocumentationLines(company));
        return response;
    }

    private ClosureBreakdownResponse.DailyLine toDailyLineDto(DailyBreakdownLine line, List<TimeEvent> dayEvents) {
        ClosureBreakdownResponse.DailyLine d = new ClosureBreakdownResponse.DailyLine();
        d.setDate(line.date().toString());
        d.setPunchTimeline(punchTimelineForDay(dayEvents));
        d.setWorkedRawMinutes(line.workedRawMinutes());
        d.setWorkedCountedMinutes(line.workedCountedMinutes());
        d.setExtraMinutes(line.extraMinutes());
        d.setDelayMinutes(line.delayMinutes());
        d.setAbsenceMinutes(line.absenceMinutes());
        d.setFirstEntryTime(line.firstEntryTime());
        d.setCalculationNote(line.calculationNote());
        return d;
    }

    private static String punchTimelineForDay(List<TimeEvent> dayEvents) {
        if (dayEvents == null || dayEvents.isEmpty()) {
            return "—";
        }
        List<TimeEvent> sorted = dayEvents.stream()
                .filter(e -> e.getEventType() == TimeEventType.ENTRY || e.getEventType() == TimeEventType.EXIT)
                .sorted(Comparator.comparing(TimeEvent::getEventTime))
                .collect(Collectors.toList());
        if (sorted.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            int pair = (i / 2) + 1;
            String label = (i % 2 == 0) ? "E" : "S";
            String time = sorted.get(i).getEventTime().toLocalTime().format(HM);
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(label).append(pair).append(" ").append(time);
        }
        return sb.toString();
    }

    private static List<String> formulaDocumentationLines(Company company) {
        List<String> lines = new ArrayList<>();
        lines.add("Norma diária (minutos): campo daily_hours da escala (ex.: 480 = 8 horas). Teto de «trabalhadas contábeis» no dia e base de «ausência» quando não há trechos válidos.");
        lines.add("Pareamento entrada/saída: em cada dia civil, ordem cronológica FIFO; cada saída fecha a entrada mais antiga ainda aberta naquele dia (não há pareamento entre dias). "
                + "Trechos ou soma diária acima de tetos de segurança são limitados quando os dados são inconsistentes.");
        lines.add("Coluna «Batidas (E/S)»: horários do dia na ordem cronológica, rotulados como na grade (E1/S1/E2/S2…), não pelo tipo gravado no banco.");
        lines.add("Dias no detalhe: união dos dias com batidas e dos dias com minutos por trechos.");
        lines.add("Se soma dos trechos do dia = 0: ausência do dia = norma (minutos); trabalhadas/extras/atraso do dia = 0.");
        lines.add("Se soma > 0: extras_dia = max(0, soma_trechos − norma); trabalhadas_contábeis_dia = min(soma_trechos, norma).");
        lines.add("Atraso: primeira entrada do dia; se após (entrada_escala + tolerância atraso em min), atraso = diferença em minutos. Entrada da escala antes de 05:00 não gera atraso neste motor.");
        lines.add("Totais do mês: somatório de trabalhadas contábeis, extras, atrasos e ausências.");
        lines.add("Valores «armazenados» (se existirem) são os do último fechamento salvo; «calculados» usam sempre eventos e escala atuais.");
        if (company != null) {
            lines.add("Empresa «" + company.getName() + "»: hora extra habilitada = "
                    + (Boolean.TRUE.equals(company.getEnableOvertime()) ? "sim" : "não")
                    + "; banco de horas habilitado = "
                    + (Boolean.TRUE.equals(company.getEnableTimeBank()) ? "sim" : "não") + ".");
            lines.add("Motor atual: não acumula nem liquida «banco de horas»; o excedente da norma diária entra em «extras» (minutos). Compensação em banco, se adotada pelo RH, fica fora deste cálculo até haver suporte explícito no sistema.");
            if (Boolean.TRUE.equals(company.getEnableOvertime()) && company.getOvertimePercentage() != null) {
                lines.add("Cadastro: percentual de hora extra = " + company.getOvertimePercentage()
                        + "% (referência; os minutos «extras» aqui são o volume de tempo acima da norma).");
            }
        }
        return lines;
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
