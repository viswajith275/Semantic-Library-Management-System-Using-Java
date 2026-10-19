package com.library.backend;

import com.library.backend.service.LibraryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers every branch of the availability state machine. */
class AvailabilityLogicTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void noCopiesAtAll() {
        var result = LibraryService.computeAvailability(0, 0, 0, null, NOW);
        assertThat(result.status()).isEqualTo("NO_COPIES");
        assertThat(result.estimatedAvailableAt()).isNull();
    }

    @Test
    void atLeastOneCopyAvailable() {
        var result = LibraryService.computeAvailability(3, 1, 2, NOW.plusDays(5), NOW);
        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.estimatedAvailableAt()).isNull();
    }

    @Test
    void allCopiesInMaintenance() {
        var result = LibraryService.computeAvailability(2, 0, 0, null, NOW);
        assertThat(result.status()).isEqualTo("UNAVAILABLE_MAINTENANCE");
        assertThat(result.estimatedAvailableAt()).isNull();
    }

    @Test
    void allBorrowedNotYetOverdue() {
        LocalDateTime due = NOW.plusDays(3);
        var result = LibraryService.computeAvailability(2, 0, 2, due, NOW);
        assertThat(result.status()).isEqualTo("ALL_BORROWED");
        assertThat(result.estimatedAvailableAt()).isEqualTo(due);
    }

    @Test
    void allBorrowedAndOverdue() {
        var result = LibraryService.computeAvailability(2, 0, 2, NOW.minusDays(1), NOW);
        assertThat(result.status()).isEqualTo("OVERDUE_PENDING_RETURN");
        assertThat(result.estimatedAvailableAt()).isNull();
    }
}
