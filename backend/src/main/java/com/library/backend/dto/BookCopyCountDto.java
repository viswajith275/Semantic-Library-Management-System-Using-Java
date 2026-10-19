package com.library.backend.dto;

public record BookCopyCountDto(
    String isbn,
    long totalCopies,
    long availableCopies,
    long borrowedCopies,
    long maintenanceCopies
) {
    public BookCopyCountDto(String isbn, Long totalCopies, Long availableCopies, Long borrowedCopies, Long maintenanceCopies) {
        this(
            isbn,
            totalCopies != null ? totalCopies : 0L,
            availableCopies != null ? availableCopies : 0L,
            borrowedCopies != null ? borrowedCopies : 0L,
            maintenanceCopies != null ? maintenanceCopies : 0L
        );
    }
}
