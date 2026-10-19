package com.library.backend.service;

import com.library.backend.dto.BookCopyCountDto;
import com.library.backend.dto.BookDto.BookDetailResponse;
import com.library.backend.dto.EarliestDueDateDto;
import com.library.backend.dto.LoanDto.MemberLoanResponse;
import com.library.backend.entity.Book;
import com.library.backend.entity.LoanRecord;
import com.library.backend.entity.PinnedBook;
import com.library.backend.entity.User;
import com.library.backend.exception.ResourceNotFoundException;
import com.library.backend.repository.BookRepository;
import com.library.backend.repository.LoanRecordRepository;
import com.library.backend.repository.PinnedBookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final PinnedBookRepository pinnedBookRepository;
    private final BookRepository bookRepository;
    private final LoanRecordRepository loanRecordRepository;

    public MemberService(
            PinnedBookRepository pinnedBookRepository,
            BookRepository bookRepository,
            LoanRecordRepository loanRecordRepository) {
        this.pinnedBookRepository = pinnedBookRepository;
        this.bookRepository = bookRepository;
        this.loanRecordRepository = loanRecordRepository;
    }

    @Transactional
    public void pinBook(User member, String rawIsbn) {
        String isbn = LibraryService.normalizeIsbn(rawIsbn);
        Book book = bookRepository.findById(isbn)
            .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN " + isbn + " not found"));

        if (!pinnedBookRepository.existsByMemberIdAndBookIsbn(member.getId(), isbn)) {
            PinnedBook pinnedBook = new PinnedBook(member, book, LocalDateTime.now());
            pinnedBookRepository.save(pinnedBook);
        }
    }

    @Transactional
    public void unpinBook(User member, String rawIsbn) {
        String isbn = LibraryService.normalizeIsbn(rawIsbn);
        pinnedBookRepository.deleteByMemberIdAndBookIsbn(member.getId(), isbn);
    }

    @Transactional(readOnly = true)
    public List<BookDetailResponse> getPinnedBooks(User member) {
        List<PinnedBook> pinned = pinnedBookRepository.findAllByMemberIdOrderByPinnedAtDesc(member.getId());
        if (pinned.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> isbns = pinned.stream().map(p -> p.getBook().getIsbn()).toList();

        Map<String, BookCopyCountDto> countMap = bookRepository.findCopyCountsByIsbnIn(isbns).stream()
            .collect(Collectors.toMap(BookCopyCountDto::isbn, c -> c));

        Map<String, LocalDateTime> dueMap = loanRecordRepository.findEarliestDueDatesByIsbnIn(isbns).stream()
            .collect(Collectors.toMap(EarliestDueDateDto::isbn, EarliestDueDateDto::minDueDate));

        LocalDateTime now = LocalDateTime.now();
        List<BookDetailResponse> results = new ArrayList<>();

        for (PinnedBook pb : pinned) {
            Book book = pb.getBook();
            String isbn = book.getIsbn();
            BookCopyCountDto counts = countMap.getOrDefault(isbn, new BookCopyCountDto(isbn, 0L, 0L, 0L, 0L));
            LocalDateTime minDueDate = dueMap.get(isbn);

            LibraryService.AvailabilityResult availability = LibraryService.computeAvailability(
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
                null,
                availability.status(),
                availability.estimatedAvailableAt(),
                Collections.emptyList()
            ));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<MemberLoanResponse> getActiveLoans(User member) {
        List<LoanRecord> loans = loanRecordRepository.findAllByMemberIdAndReturnedAtIsNullOrderByDueDateAsc(member.getId());
        LocalDateTime now = LocalDateTime.now();

        return loans.stream().map(lr -> {
            long remainingHours = Duration.between(now, lr.getDueDate()).toHours();
            boolean isOverdue = lr.getDueDate().isBefore(now);
            return new MemberLoanResponse(
                lr.getId(),
                lr.getCopy().getBook().getTitle(),
                lr.getCopy().getBook().getIsbn(),
                lr.getCopy().getBarcode(),
                lr.getBorrowedAt(),
                lr.getDueDate(),
                remainingHours,
                isOverdue
            );
        }).toList();
    }
}
