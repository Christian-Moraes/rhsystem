package com.rhsystem.application.usecase;

import com.rhsystem.application.dto.CompanyRequest;
import com.rhsystem.application.dto.CompanyResponse;
import com.rhsystem.domain.model.Company;
import com.rhsystem.domain.model.UserCompany;
import com.rhsystem.domain.model.UserRole;
import com.rhsystem.domain.model.WorkSchedule;
import com.rhsystem.domain.repository.CompanyRepository;
import com.rhsystem.domain.repository.UserCompanyRepository;
import com.rhsystem.domain.repository.WorkScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CompanyUseCase {

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final WorkScheduleRepository workScheduleRepository;

    public CompanyUseCase(CompanyRepository companyRepository,
                          UserCompanyRepository userCompanyRepository,
                          WorkScheduleRepository workScheduleRepository) {
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.workScheduleRepository = workScheduleRepository;
    }

    @Transactional
    public CompanyResponse createCompany(UUID userId, CompanyRequest request) {
        if (companyRepository.existsByCnpj(request.getCnpj())) {
            throw new RuntimeException("CNPJ já cadastrado");
        }

        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(request.getName());
        company.setCnpj(request.getCnpj());
        company.setResponsibleName(request.getResponsibleName());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone() != null ? request.getPhone() : "");
        company.setEnableOvertime(request.getEnableOvertime());
        company.setEnableTimeBank(request.getEnableTimeBank());
        company.setOvertimePercentage(request.getOvertimePercentage());
        company.setWeekendPercentage(request.getWeekendPercentage());
        company.setCreatedAt(LocalDateTime.now());
        company = companyRepository.save(company);

        UserCompany userCompany = new UserCompany();
        userCompany.setId(UUID.randomUUID());
        userCompany.setUserId(userId);
        userCompany.setCompanyId(company.getId());
        userCompany.setRole(UserRole.ADMIN);
        userCompany.setCreatedAt(LocalDateTime.now());
        userCompanyRepository.save(userCompany);

        WorkSchedule workSchedule = new WorkSchedule();
        workSchedule.setId(UUID.randomUUID());
        workSchedule.setCompanyId(company.getId());
        workSchedule.setDailyHours(480);
        workSchedule.setEntryTime(LocalTime.of(8, 0));
        workSchedule.setExitTime(LocalTime.of(17, 0));
        workSchedule.setBreakMinutes(60);
        workSchedule.setToleranceLate(10);
        workSchedule.setToleranceLeave(10);
        workScheduleRepository.save(workSchedule);

        return toResponse(company, UserRole.ADMIN);
    }

    public List<CompanyResponse> getUserCompanies(UUID userId) {
        List<UserCompany> userCompanies = userCompanyRepository.findByUserId(userId);
        return userCompanies.stream()
                .map(uc -> {
                    Company company = companyRepository.findById(uc.getCompanyId())
                            .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
                    return toResponse(company, uc.getRole());
                })
                .collect(Collectors.toList());
    }

    public CompanyResponse getCompanyById(UUID companyId, UUID userId) {
        UserCompany userCompany = userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("Usuário não tem acesso a esta empresa"));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        return toResponse(company, userCompany.getRole());
    }

    @Transactional
    public CompanyResponse updateCompany(UUID companyId, UUID userId, CompanyRequest request) {
        UserCompany userCompany = userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new RuntimeException("Usuário não tem acesso a esta empresa"));

        if (userCompany.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Apenas administradores podem editar empresas");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        if (!company.getCnpj().equals(request.getCnpj()) && companyRepository.existsByCnpj(request.getCnpj())) {
            throw new RuntimeException("CNPJ já cadastrado");
        }

        company.setName(request.getName());
        company.setCnpj(request.getCnpj());
        company.setResponsibleName(request.getResponsibleName());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone() != null ? request.getPhone() : "");
        company.setEnableOvertime(request.getEnableOvertime());
        company.setEnableTimeBank(request.getEnableTimeBank());
        company.setOvertimePercentage(request.getOvertimePercentage());
        company.setWeekendPercentage(request.getWeekendPercentage());
        company = companyRepository.save(company);

        return toResponse(company, userCompany.getRole());
    }

    private CompanyResponse toResponse(Company company, UserRole role) {
        CompanyResponse response = new CompanyResponse();
        response.setId(company.getId());
        response.setName(company.getName());
        response.setCnpj(company.getCnpj());
        response.setResponsibleName(company.getResponsibleName());
        response.setEmail(company.getEmail());
        response.setPhone(company.getPhone());
        response.setEnableOvertime(company.getEnableOvertime());
        response.setEnableTimeBank(company.getEnableTimeBank());
        response.setOvertimePercentage(company.getOvertimePercentage());
        response.setWeekendPercentage(company.getWeekendPercentage());
        response.setCreatedAt(company.getCreatedAt());
        response.setRole(role.name());
        return response;
    }
}
