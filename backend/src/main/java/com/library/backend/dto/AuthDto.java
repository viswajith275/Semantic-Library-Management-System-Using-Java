package com.library.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class AuthDto {

    public record AdminLoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}

    public record AdminAuthResponse(
        Long id,
        String name,
        String email,
        String role,
        String token,
        LocalDateTime expiresAt
    ) {}

    public record MemberLoginRequest(
        @NotBlank(message = "Custom code is required")
        String customCode
    ) {}

    public record MemberAuthResponse(
        Long id,
        String name,
        String role,
        String customCode
    ) {}

    public record CreateAdminRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}
}
