package com.rhsystem.application.usecase;

import com.rhsystem.application.dto.LoginRequest;
import com.rhsystem.application.dto.LoginResponse;
import com.rhsystem.application.dto.RegisterRequest;
import com.rhsystem.config.security.JwtTokenProvider;
import com.rhsystem.domain.model.*;
import com.rhsystem.domain.repository.CompanyRepository;
import com.rhsystem.domain.repository.UserCompanyRepository;
import com.rhsystem.domain.repository.UserRepository;
import com.rhsystem.domain.repository.WorkScheduleRepository;
import com.rhsystem.shared.UserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthUseCase {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthUseCase(UserRepository userRepository, CompanyRepository companyRepository,
                      UserCompanyRepository userCompanyRepository, WorkScheduleRepository workScheduleRepository,
                      PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Usuário inativo");
        }

        List<UserCompany> userCompanies = userCompanyRepository.findByUserId(user.getId());
        
        UUID defaultCompanyId = null;
        String defaultRole = null;
        
        if (!userCompanies.isEmpty()) {
            UserCompany defaultUserCompany = userCompanies.get(0);
            defaultCompanyId = defaultUserCompany.getCompanyId();
            defaultRole = defaultUserCompany.getRole().name();
        }

        String accessToken = jwtTokenProvider.generateToken(
                user.getId(),
                defaultCompanyId,
                defaultRole != null ? defaultRole : "ADMIN"
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        List<LoginResponse.CompanyDto> companies = userCompanies.stream()
                .map(uc -> {
                    Company c = companyRepository.findById(uc.getCompanyId()).orElse(null);
                    return new LoginResponse.CompanyDto(
                            c != null ? c.getId() : uc.getCompanyId(),
                            c != null ? c.getName() : "Unknown",
                            uc.getRole().name()
                    );
                })
                .collect(Collectors.toList());

        LoginResponse.UserDto userDto = new LoginResponse.UserDto(
                user.getId(),
                user.getName(),
                user.getEmail()
        );

        return new LoginResponse(accessToken, refreshToken, userDto, companies);
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
