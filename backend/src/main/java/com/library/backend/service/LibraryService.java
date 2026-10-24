package com.library.backend.service;

import com.library.backend.dto.BookCopyCountDto;
import com.library.backend.dto.BookDto.*;
import com.library.backend.dto.EarliestDueDateDto;
import com.library.backend.entity.AvailabilityStatus;
import com.library.backend.entity.Book;
import com.library.backend.exception.ResourceNotFoundException;
import com.library.backend.repository.BookCopyRepository;
import com.library.backend.repository.BookRepository;
import com.library.backend.repository.LoanRecordRepository;
import com.library.backend.service.embedding.EmbeddingService;
import com.library.backend.service.embedding.VectorIndexCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LibraryService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRecordRepository loanRecordRepository;
    private final EmbeddingService embeddingService;
    private final VectorIndexCache vectorIndexCache;

    public LibraryService(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            LoanRecordRepository loanRecordRepository,
            EmbeddingService embeddingService,
            VectorIndexCache vectorIndexCache) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRecordRepository = loanRecordRepository;
        this.embeddingService = embeddingService;
        this.vectorIndexCache = vectorIndexCache;
    }

    public record AvailabilityResult(String status, LocalDateTime estimatedAvailableAt) {}

    public static AvailabilityResult computeAvailability(
            long totalCopies,
            long availableCopies,
            long borrowedCopies,
            LocalDateTime minDueDate,
            LocalDateTime now) {
        if (totalCopies == 0) {
            return new AvailabilityResult(AvailabilityStatus.NO_COPIES.name(), null);
        } else if (availableCopies > 0) {
            return new AvailabilityResult(AvailabilityStatus.AVAILABLE.name(), null);
        } else if (borrowedCopies == 0) {
            return new AvailabilityResult(AvailabilityStatus.UNAVAILABLE_MAINTENANCE.name(), null);
        } else {
            if (minDueDate != null && minDueDate.isBefore(now)) {
                return new AvailabilityResult(AvailabilityStatus.OVERDUE_PENDING_RETURN.name(), null);
            } else {
                return new AvailabilityResult(AvailabilityStatus.ALL_BORROWED.name(), minDueDate);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<BookDetailResponse> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return Collections.emptyList();
        }

        // 1. Vector generation outside database lock
        float[] queryVector = embeddingService.generateEmbedding(query);

        // 2. Cosine similarity in-memory
        List<VectorIndexCache.ScoredIsbn> scoredList = vectorIndexCache.searchTopK(queryVector, topK);
        if (scoredList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> isbns = scoredList.stream().map(VectorIndexCache.ScoredIsbn::isbn).toList();
        Map<String, Double> scoreMap = scoredList.stream()
            .collect(Collectors.toMap(VectorIndexCache.ScoredIsbn::isbn, VectorIndexCache.ScoredIsbn::score));

        // 3. Bulk fetch books, copy counts, and earliest due dates (Zero N+1 queries)
        Map<String, Book> bookMap = bookRepository.findAllByIsbnIn(isbns).stream()
            .collect(Collectors.toMap(Book::getIsbn, b -> b));

        Map<String, BookCopyCountDto> countMap = bookRepository.findCopyCountsByIsbnIn(isbns).stream()
            .collect(Collectors.toMap(BookCopyCountDto::isbn, c -> c));

        Map<String, LocalDateTime> dueMap = loanRecordRepository.findEarliestDueDatesByIsbnIn(isbns).stream()
            .collect(Collectors.toMap(EarliestDueDateDto::isbn, EarliestDueDateDto::minDueDate));

        Map<String, List<CopyItemDto>> copyMap = bookCopyRepository.findAllByBookIsbnIn(isbns).stream()
            .collect(Collectors.groupingBy(
                copy -> copy.getBook().getIsbn(),
                Collectors.mapping(
                    copy -> new CopyItemDto(copy.getBarcode(), copy.getStatus()),
                    Collectors.toList()
                )
            ));

        LocalDateTime now = LocalDateTime.now();

        // 4. Assemble in exact ranked order
        List<BookDetailResponse> results = new ArrayList<>();
        for (String isbn : isbns) {
            Book book = bookMap.get(isbn);
            if (book == null) {
                continue;
            }

            BookCopyCountDto counts = countMap.getOrDefault(isbn, new BookCopyCountDto(isbn, 0L, 0L, 0L, 0L));
            LocalDateTime minDueDate = dueMap.get(isbn);
            AvailabilityResult availability = computeAvailability(
                counts.totalCopies(),
                counts.availableCopies(),
                counts.borrowedCopies(),
                minDueDate,
                now
            );

            results.add(new BookDetailResponse(
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getCoverColor(),
                book.getSummary(),
                book.getCharacters(),
                book.getShelfLocation(),
                counts.totalCopies(),
                counts.availableCopies(),
                counts.borrowedCopies(),
                scoreMap.get(isbn),
                availability.status(),
                availability.estimatedAvailableAt(),
                copyMap.getOrDefault(isbn, Collections.emptyList())
                    .stream()
                    .sorted(Comparator.comparing(CopyItemDto::barcode))
                    .toList()
            ));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public BookDetailResponse getBookByIsbn(String rawIsbn) {
        String isbn = normalizeIsbn(rawIsbn);
        Book book = bookRepository.findById(isbn)
            .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN " + isbn + " not found"));

        BookCopyCountDto counts = bookRepository.findCopyCountsByIsbn(isbn)
            .orElse(new BookCopyCountDto(isbn, 0L, 0L, 0L, 0L));

        LocalDateTime minDueDate = loanRecordRepository.findEarliestDueDateByIsbn(isbn).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        AvailabilityResult availability = computeAvailability(
            counts.totalCopies(),
            counts.availableCopies(),
            counts.borrowedCopies(),
            minDueDate,
            now
        );

        List<CopyItemDto> copies = bookCopyRepository.findAllByBookIsbn(isbn).stream()
            .map(c -> new CopyItemDto(c.getBarcode(), c.getStatus()))
            .toList();

        return new BookDetailResponse(
            book.getIsbn(),
            book.getTitle(),
            book.getAuthor(),
            book.getCoverColor(),
            book.getSummary(),
            book.getCharacters(),
            book.getShelfLocation(),
            counts.totalCopies(),
            counts.availableCopies(),
            counts.borrowedCopies(),
            null,
            availability.status(),
            availability.estimatedAvailableAt(),
            copies
        );
    }

    public static String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.replaceAll("[\\s-]+", "").trim();
    }
}
