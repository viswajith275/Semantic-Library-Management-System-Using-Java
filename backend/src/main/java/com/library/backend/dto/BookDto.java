package com.library.backend.dto;

import com.library.backend.entity.CopyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class BookDto {

    public record CreateBookRequest(
        @NotBlank(message = "ISBN is required")
        String isbn,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author,

        String coverColor,

        String summary,

        String characters,

        @NotBlank(message = "Shelf location is required")
        String shelfLocation,

        List<String> initialBarcodes
    ) {}

    public record UpdateBookRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author,

        String coverColor,

        String summary,

        String characters,

        @NotBlank(message = "Shelf location is required")
        String shelfLocation
    ) {}

    public record AddCopiesRequest(
        @NotEmpty(message = "At least one barcode must be provided")
        List<String> barcodes
    ) {}

    public record UpdateCopyStatusRequest(
        @NotNull(message = "Status is required")
        CopyStatus status
    ) {}

    public record CopyItemDto(
        String barcode,
        CopyStatus status
    ) {}

    public record BookDetailResponse(
        String isbn,
        String title,
        String author,
        String coverColor,
        String summary,
        String characters,
        String shelfLocation,
        long totalCopies,
        long availableCopies,
        long borrowedCopies,
        Double similarityScore,
        String availabilityStatus,
        LocalDateTime estimatedAvailableAt,
        List<CopyItemDto> copies
    ) {}
}
