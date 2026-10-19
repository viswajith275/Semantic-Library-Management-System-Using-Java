package com.library.backend.service;

import com.library.backend.dto.BookCopyCountDto;
import com.library.backend.dto.DashboardDto.DashboardResponse;
import com.library.backend.dto.DashboardDto.LibraryStats;
import com.library.backend.dto.DashboardDto.Recommendation;
import com.library.backend.entity.Book;
import com.library.backend.entity.CopyStatus;
import com.library.backend.entity.LoanRecord;
import com.library.backend.entity.Role;
import com.library.backend.entity.User;
import com.library.backend.repository.BookCopyRepository;
import com.library.backend.repository.BookRepository;
import com.library.backend.repository.LoanRecordRepository;
import com.library.backend.repository.PinnedBookRepository;
import com.library.backend.repository.UserRepository;
import com.library.backend.service.embedding.VectorIndexCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int RECOMMENDATION_LIMIT = 5;

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRecordRepository loanRecordRepository;
    private final UserRepository userRepository;
    private final PinnedBookRepository pinnedBookRepository;
    private final MemberService memberService;
    private final VectorIndexCache vectorIndexCache;

    public DashboardService(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            LoanRecordRepository loanRecordRepository,
            UserRepository userRepository,
            PinnedBookRepository pinnedBookRepository,
            MemberService memberService,
            VectorIndexCache vectorIndexCache) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRecordRepository = loanRecordRepository;
        this.userRepository = userRepository;
        this.pinnedBookRepository = pinnedBookRepository;
        this.memberService = memberService;
        this.vectorIndexCache = vectorIndexCache;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(User user) {
        LocalDateTime now = LocalDateTime.now();

        LibraryStats stats = new LibraryStats(
            bookRepository.count(),
            bookCopyRepository.count(),
            bookCopyRepository.countByStatus(CopyStatus.AVAILABLE),
            bookCopyRepository.countByStatus(CopyStatus.BORROWED),
            bookCopyRepository.countByStatus(CopyStatus.MAINTENANCE),
            userRepository.countByRole(Role.MEMBER),
            loanRecordRepository.countByReturnedAtIsNull(),
            loanRecordRepository.countByReturnedAtIsNullAndDueDateBefore(now)
        );

        // Every book this member has ever borrowed, newest first.
        List<String> borrowedIsbns = loanRecordRepository
            .findAllByMemberIdOrderByBorrowedAtDesc(user.getId()).stream()
            .map(lr -> lr.getCopy().getBook().getIsbn())
            .distinct()
            .toList();

        return new DashboardResponse(
            stats,
            pinnedBookRepository.countByMemberId(user.getId()),
            memberService.getActiveLoans(user),
            buildRecommendations(borrowedIsbns)
        );
    }

    private List<Recommendation> buildRecommendations(List<String> borrowedIsbns) {
        Set<String> excluded = new HashSet<>(borrowedIsbns);
        Map<String, Double> scores = new HashMap<>();
        List<String> ranked = new ArrayList<>();

        float[] taste = averageVectors(borrowedIsbns);
        if (taste != null) {
            // Recommend unread books closest to the average of everything the member borrowed.
            for (VectorIndexCache.ScoredIsbn scored : vectorIndexCache.searchTopK(
                    taste, RECOMMENDATION_LIMIT + excluded.size(), excluded)) {
                ranked.add(scored.isbn());
                scores.put(scored.isbn(), scored.score());
            }
        } else {
            // No history (or no embeddings yet): newest additions first.
            ranked = bookRepository.findAllNewestFirst().stream()
                .map(Book::getIsbn)
                .filter(isbn -> !excluded.contains(isbn))
                .limit(RECOMMENDATION_LIMIT)
                .toList();
        }

        if (ranked.isEmpty()) {
            return List.of();
        }

        Map<String, Book> books = bookRepository.findAllByIsbnIn(ranked).stream()
            .collect(Collectors.toMap(Book::getIsbn, b -> b));
        Map<String, BookCopyCountDto> counts = bookRepository.findCopyCountsByIsbnIn(ranked).stream()
            .collect(Collectors.toMap(BookCopyCountDto::isbn, c -> c));

        List<Recommendation> recommendations = new ArrayList<>();
        for (String isbn : ranked) {
            Book book = books.get(isbn);
            if (book == null) {
                continue;
            }
            recommendations.add(new Recommendation(
                isbn,
                book.getTitle(),
                book.getAuthor(),
                book.getCoverColor(),
                book.getSummary(),
                book.getShelfLocation(),
                scores.get(isbn),
                counts.containsKey(isbn) ? counts.get(isbn).availableCopies() : 0L
            ));
        }
        return recommendations;
    }

    private float[] averageVectors(List<String> isbns) {
        float[] sum = null;
        int used = 0;
        for (String isbn : isbns) {
            float[] vector = vectorIndexCache.getVector(isbn);
            if (vector == null) {
                continue;
            }
            if (sum == null) {
                sum = new float[vector.length];
            } else if (sum.length != vector.length) {
                continue; // stale vector from a previous embedding model
            }
            for (int i = 0; i < vector.length; i++) {
                sum[i] += vector[i];
            }
            used++;
        }
        return used == 0 ? null : sum;
    }
}
