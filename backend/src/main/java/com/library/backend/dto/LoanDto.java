package com.library.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class LoanDto {

    public record CheckoutRequest(
        @NotBlank(message = "Copy barcode is required")
        String barcode,

        @NotBlank(message = "Member custom code is required")
        String memberCustomCode,

        @NotNull(message = "Days to borrow is required")
        @Min(value = 1, message = "Days to borrow must be at least 1")
        @Max(value = 365, message = "Days to borrow cannot exceed 365")
        Integer daysToBorrow
    ) {}

    public record ReturnRequest(
        @NotBlank(message = "Copy barcode is required")
        String barcode
    ) {}

    public record ExtendLoanRequest(
        @NotNull(message = "Additional days is required")
        @Min(value = 1, message = "Additional days must be at least 1")
        Integer additionalDays
    ) {}

    public record ActiveLoanResponse(
        Long id,
        String bookTitle,
        String isbn,
        String barcode,
        String memberName,
        String memberCustomCode,
        LocalDateTime borrowedAt,
        LocalDateTime dueDate,
        boolean isOverdue
    ) {}

    public record MemberLoanResponse(
        Long loanId,
        String bookTitle,
        String isbn,
        String barcode,
        LocalDateTime borrowedAt,
        LocalDateTime dueDate,
        long remainingHours,
        boolean isOverdue
    ) {}

    public record LoanActionResponse(
        String message,
        Long loanId,
        String barcode,
        LocalDateTime dueDate,
        LocalDateTime returnedAt
    ) {}
}
