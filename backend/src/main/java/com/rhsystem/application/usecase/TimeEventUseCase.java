package com.rhsystem.application.usecase;

import com.rhsystem.application.dto.TimeEventRequest;
import com.rhsystem.application.dto.TimeEventResponse;
import com.rhsystem.domain.model.Employee;
import com.rhsystem.domain.model.TimeEvent;
import com.rhsystem.domain.model.TimeEventStatus;
import com.rhsystem.domain.model.TimeEventType;
import com.rhsystem.domain.repository.EmployeeRepository;
import com.rhsystem.domain.repository.TimeEventRepository;
import com.rhsystem.domain.repository.UserCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TimeEventUseCase {

    private final TimeEventRepository timeEventRepository;
    private final EmployeeRepository employeeRepository;
    private final UserCompanyRepository userCompanyRepository;

    public TimeEventUseCase(TimeEventRepository timeEventRepository,
                           EmployeeRepository employeeRepository,
                           UserCompanyRepository userCompanyRepository) {
        this.timeEventRepository = timeEventRepository;
        this.employeeRepository = employeeRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    @Transactional
    public TimeEventResponse createTimeEvent(UUID userId, UUID companyId, TimeEventRequest request) {
        verifyEmployeeAccess(userId, companyId, request.getEmployeeId());

        TimeEvent timeEvent = new TimeEvent();
        timeEvent.setId(UUID.randomUUID());
        timeEvent.setEmployeeId(request.getEmployeeId());
        timeEvent.setEventType(TimeEventType.valueOf(request.getEventType()));
        timeEvent.setEventTime(request.getEventTime());
        timeEvent.setSource("MANUAL");
        timeEvent.setStatus(TimeEventStatus.CONFIRMED);
        timeEvent.setCreatedAt(LocalDateTime.now());
        timeEvent = timeEventRepository.save(timeEvent);

        return toResponse(timeEvent);
    }

    public List<TimeEventResponse> getTimeEventsByEmployee(UUID userId, UUID companyId, UUID employeeId) {
        verifyEmployeeAccess(userId, companyId, employeeId);
        return timeEventRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TimeEventResponse> getTimeEventsByEmployeeAndMonth(
            UUID userId, UUID companyId, UUID employeeId, int month, int year) {
        verifyEmployeeAccess(userId, companyId, employeeId);
        return timeEventRepository.findByEmployeeIdAndMonth(employeeId, month, year)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TimeEventResponse getTimeEventById(UUID userId, UUID companyId, UUID timeEventId) {
        TimeEvent timeEvent = timeEventRepository.findById(timeEventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        verifyEmployeeAccess(userId, companyId, timeEvent.getEmployeeId());

        return toResponse(timeEvent);
    }

    @Transactional
    public TimeEventResponse updateTimeEvent(
            UUID userId, UUID companyId, UUID timeEventId, TimeEventRequest request) {
        TimeEvent timeEvent = timeEventRepository.findById(timeEventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        verifyEmployeeAccess(userId, companyId, timeEvent.getEmployeeId());

        timeEvent.setEventType(TimeEventType.valueOf(request.getEventType()));
        timeEvent.setEventTime(request.getEventTime());
        timeEvent.setStatus(TimeEventStatus.CONFIRMED);
        timeEvent = timeEventRepository.save(timeEvent);

        return toResponse(timeEvent);
    }

    @Transactional
    public void deleteTimeEvent(UUID userId, UUID companyId, UUID timeEventId) {
        TimeEvent timeEvent = timeEventRepository.findById(timeEventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        verifyEmployeeAccess(userId, companyId, timeEvent.getEmployeeId());

        timeEventRepository.delete(timeEventId);
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

    private TimeEventResponse toResponse(TimeEvent timeEvent) {
        TimeEventResponse response = new TimeEventResponse();
        response.setId(timeEvent.getId());
        response.setEmployeeId(timeEvent.getEmployeeId());
        response.setEventType(timeEvent.getEventType().name());
        response.setEventTime(timeEvent.getEventTime());
        response.setSource(timeEvent.getSource());
        response.setStatus(timeEvent.getStatus().name());
        response.setCreatedAt(timeEvent.getCreatedAt());
        return response;
    }
}
