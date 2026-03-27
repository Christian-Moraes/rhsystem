package com.rhsystem.application.usecase;

import com.rhsystem.application.dto.WorkScheduleRequest;
import com.rhsystem.application.dto.WorkScheduleResponse;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.repository.UserCompanyRepository;
import com.rhsystem.domain.repository.WorkScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkScheduleUseCase {

    private final WorkScheduleRepository workScheduleRepository;
    private final UserCompanyRepository userCompanyRepository;

    public WorkScheduleUseCase(WorkScheduleRepository workScheduleRepository,
                                UserCompanyRepository userCompanyRepository) {
        this.workScheduleRepository = workScheduleRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    public List<WorkScheduleResponse> getByCompany(UUID userId, UUID companyId) {
        verifyUserAccess(userId, companyId);
        return workScheduleRepository.findByCompanyId(companyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkScheduleResponse create(UUID userId, UUID companyId, WorkScheduleRequest request) {
        verifyUserAccess(userId, companyId);

        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(null); // será gerado pelo JPA
        schedule.setCompanyId(companyId);
        // Domínio/banco: minutos (ex.: 8h → 480). API recebe horas inteiras.
        schedule.setDailyHours(request.getDailyHours() * 60);
        schedule.setEntryTime(parseTime(request.getEntryTime()));
        schedule.setExitTime(parseTime(request.getExitTime()));
        schedule.setBreakMinutes(request.getBreakMinutes());
        schedule.setToleranceLate(request.getToleranceLate());
        schedule.setToleranceLeave(request.getToleranceLeave());

        WorkSchedule saved = workScheduleRepository.save(schedule);
        return toResponse(saved);
    }

    private void verifyUserAccess(UUID userId, UUID companyId) {
        userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("Usuário não tem acesso a esta empresa"));
    }

    private WorkScheduleResponse toResponse(WorkSchedule schedule) {
        WorkScheduleResponse response = new WorkScheduleResponse();
        response.setId(schedule.getId());
        Integer minutes = schedule.getDailyHours();
        response.setDailyHours(minutes == null ? null : minutes / 60);
        response.setEntryTime(schedule.getEntryTime() != null ? schedule.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
        response.setExitTime(schedule.getExitTime() != null ? schedule.getExitTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
        response.setBreakMinutes(schedule.getBreakMinutes());
        response.setToleranceLate(schedule.getToleranceLate());
        response.setToleranceLeave(schedule.getToleranceLeave());
        return response;
    }

    private static LocalTime parseTime(String time) {
        if (time == null) return null;
        // Aceita "HH:mm" e também "H:mm" (por segurança)
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm");
        return LocalTime.parse(time, fmt);
    }
}

