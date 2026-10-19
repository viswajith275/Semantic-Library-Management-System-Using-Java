package com.library.backend.repository;

import com.library.backend.dto.EarliestDueDateDto;
import com.library.backend.entity.LoanRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRecordRepository extends JpaRepository<LoanRecord, Long> {

    boolean existsByCopyBookIsbn(String isbn);

    boolean existsByCopyBarcode(String barcode);

    Optional<LoanRecord> findByCopyBarcodeAndReturnedAtIsNull(String barcode);

    // Fetch copy/book/member up front: the loan lists read those fields for every row.
    // shortcut: returns every active loan; add paging when the count runs into thousands.
    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<LoanRecord> findAllByReturnedAtIsNullOrderByDueDateAsc();

    @EntityGraph(attributePaths = {"copy", "copy.book"})
    List<LoanRecord> findAllByMemberIdAndReturnedAtIsNullOrderByDueDateAsc(Long memberId);

    // Used by the dashboard to build a member's reading profile.
    @EntityGraph(attributePaths = {"copy", "copy.book"})
    List<LoanRecord> findAllByMemberIdOrderByBorrowedAtDesc(Long memberId);

    long countByReturnedAtIsNull();

    long countByReturnedAtIsNullAndDueDateBefore(LocalDateTime now);

    @Query("""
        SELECT new com.library.backend.dto.EarliestDueDateDto(
            lr.copy.book.isbn,
            MIN(lr.dueDate)
        )
        FROM LoanRecord lr
        WHERE lr.copy.book.isbn IN :isbns AND lr.returnedAt IS NULL
        GROUP BY lr.copy.book.isbn
    """)
    List<EarliestDueDateDto> findEarliestDueDatesByIsbnIn(@Param("isbns") Collection<String> isbns);

    @Query("""
        SELECT MIN(lr.dueDate)
        FROM LoanRecord lr
        WHERE lr.copy.book.isbn = :isbn AND lr.returnedAt IS NULL
    """)
    Optional<LocalDateTime> findEarliestDueDateByIsbn(@Param("isbn") String isbn);
}
