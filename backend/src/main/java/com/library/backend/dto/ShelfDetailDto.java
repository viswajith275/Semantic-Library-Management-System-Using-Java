package com.library.backend.dto;

import java.util.List;

public record ShelfDetailDto(
    String shelfLocation,
    long totalCopies,
    long availableCopies,
    long borrowedCopies,
    long maintenanceCopies,
    List<ShelfBookItemDto> books
) {
    public record ShelfBookItemDto(
        String isbn,
        String title,
        String author,
        long totalCopies,
        long availableCopies,
        long borrowedCopies,
        long maintenanceCopies
    ) {}
}
