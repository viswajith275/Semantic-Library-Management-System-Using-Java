package com.library.backend.dto;

import java.time.LocalDateTime;

public record MemberSummaryDto(
    Long id,
    String name,
    String email,
    String customCode,
    Boolean active,
    long activeBorrowedCount
) {}
