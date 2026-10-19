package com.library.backend.dto;

public record ShelfBookSummaryDto(
    String shelfLocation,
    String isbn,
    String title,
    String author,
    long totalCopies,
    long availableCopies,
    long borrowedCopies,
    long maintenanceCopies
) {
    public ShelfBookSummaryDto(String shelfLocation, String isbn, String title, String author, Long totalCopies, Long availableCopies, Long borrowedCopies, Long maintenanceCopies) {
        this(
            shelfLocation,
            isbn,
            title,
            author,
            totalCopies != null ? totalCopies : 0L,
            availableCopies != null ? availableCopies : 0L,
            borrowedCopies != null ? borrowedCopies : 0L,
            maintenanceCopies != null ? maintenanceCopies : 0L
        );
    }
}
