package com.rhsystem.adapters.inbound.controller;

import com.rhsystem.application.dto.LoginRequest;
import com.rhsystem.application.dto.LoginResponse;
import com.rhsystem.application.dto.RegisterRequest;
import com.rhsystem.application.usecase.AuthUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authUseCase.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authUseCase.register(request);
        return ResponseEntity.ok().build();
    }
}
