package com.library.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class MemberDto {

    public record CreateMemberRequest(
        @NotBlank(message = "Member name is required")
        String name,

        @Email(message = "Invalid email format")
        String email
    ) {}

    public record UpdateMemberRequest(
        String name,

        @Email(message = "Invalid email format")
        String email,

        Boolean active,

        Boolean regenerateCode
    ) {}

    public record MemberResponse(
        Long id,
        String name,
        String email,
        String customCode,
        Boolean active
    ) {}
}
