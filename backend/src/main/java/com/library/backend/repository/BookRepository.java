package com.library.backend.repository;

import com.library.backend.dto.BookCopyCountDto;
import com.library.backend.dto.ShelfBookSummaryDto;
import com.library.backend.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {

    boolean existsByIsbn(String isbn);

    List<Book> findAllByIsbnIn(Collection<String> isbns);

    // SQLite rowid is the insertion order; avoids adding a created_at column whose
    // driver-side second precision cannot break ties between books added together.
    @Query(value = "SELECT * FROM books ORDER BY rowid DESC", nativeQuery = true)
    List<Book> findAllNewestFirst();

    @Query("""
        SELECT new com.library.backend.dto.ShelfBookSummaryDto(
            b.shelfLocation,
            b.isbn,
            b.title,
            b.author,
            COUNT(c.barcode),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.AVAILABLE THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.BORROWED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.MAINTENANCE THEN 1L ELSE 0L END)
        )
        FROM Book b
        LEFT JOIN BookCopy c ON c.book = b
        GROUP BY b.shelfLocation, b.isbn, b.title, b.author
        ORDER BY b.shelfLocation ASC, b.title ASC
    """)
    List<ShelfBookSummaryDto> findShelfAnalytics();

    @Query("""
        SELECT new com.library.backend.dto.BookCopyCountDto(
            b.isbn,
            COUNT(c.barcode),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.AVAILABLE THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.BORROWED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.MAINTENANCE THEN 1L ELSE 0L END)
        )
        FROM Book b
        LEFT JOIN BookCopy c ON c.book = b
        WHERE b.isbn IN :isbns
        GROUP BY b.isbn
    """)
    List<BookCopyCountDto> findCopyCountsByIsbnIn(@Param("isbns") Collection<String> isbns);

    @Query("""
        SELECT new com.library.backend.dto.BookCopyCountDto(
            b.isbn,
            COUNT(c.barcode),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.AVAILABLE THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.BORROWED THEN 1L ELSE 0L END),
            SUM(CASE WHEN c.status = com.library.backend.entity.CopyStatus.MAINTENANCE THEN 1L ELSE 0L END)
        )
        FROM Book b
        LEFT JOIN BookCopy c ON c.book = b
        WHERE b.isbn = :isbn
        GROUP BY b.isbn
    """)
    Optional<BookCopyCountDto> findCopyCountsByIsbn(@Param("isbn") String isbn);
}
