package com.library.backend.dto;

import com.library.backend.dto.LoanDto.MemberLoanResponse;

import java.util.List;

public class DashboardDto {

    public record LibraryStats(
        long totalBooks,
        long totalCopies,
        long availableCopies,
        long borrowedCopies,
        long maintenanceCopies,
        long totalMembers,
        long activeLoans,
        long overdueLoans
    ) {}

    public record Recommendation(
        String isbn,
        String title,
        String author,
        String coverColor,
        String summary,
        String shelfLocation,
        Double similarityScore,
        long availableCopies
    ) {}

    public record DashboardResponse(
        LibraryStats stats,
        long pinnedBooks,
        List<MemberLoanResponse> activeLoans,
        List<Recommendation> recommendations
    ) {}
}
