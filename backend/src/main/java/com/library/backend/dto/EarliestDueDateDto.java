package com.library.backend.dto;

import java.time.LocalDateTime;

public record EarliestDueDateDto(
    String isbn,
    LocalDateTime minDueDate
) {}
