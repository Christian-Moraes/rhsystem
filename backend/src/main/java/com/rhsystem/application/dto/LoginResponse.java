package com.rhsystem.application.dto;

import java.util.List;
import java.util.UUID;

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserDto user;
    private List<CompanyDto> companies;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, UserDto user, List<CompanyDto> companies) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.companies = companies;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public List<CompanyDto> getCompanies() {
        return companies;
    }

    public void setCompanies(List<CompanyDto> companies) {
        this.companies = companies;
    }

    public static class UserDto {
        private UUID id;
        private String name;
        private String email;

        public UserDto() {
        }

        public UserDto(UUID id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class CompanyDto {
        private UUID id;
        private String name;
        private String role;

        public CompanyDto() {
        }

        public CompanyDto(UUID id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
