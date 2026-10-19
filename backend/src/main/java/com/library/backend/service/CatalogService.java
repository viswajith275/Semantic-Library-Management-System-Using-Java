package com.library.backend.service;

import com.library.backend.dto.BookDto.*;
import com.library.backend.dto.ShelfBookSummaryDto;
import com.library.backend.dto.ShelfDetailDto;
import com.library.backend.dto.ShelfDetailDto.ShelfBookItemDto;
import com.library.backend.entity.Book;
import com.library.backend.entity.BookCopy;
import com.library.backend.entity.CopyStatus;
import com.library.backend.exception.BadRequestException;
import com.library.backend.exception.ConflictException;
import com.library.backend.exception.ResourceNotFoundException;
import com.library.backend.repository.BookCopyRepository;
import com.library.backend.repository.BookRepository;
import com.library.backend.repository.LoanRecordRepository;
import com.library.backend.repository.PinnedBookRepository;
import com.library.backend.service.embedding.EmbeddingService;
import com.library.backend.service.embedding.VectorIndexCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRecordRepository loanRecordRepository;
    private final PinnedBookRepository pinnedBookRepository;
    private final EmbeddingService embeddingService;
    private final VectorIndexCache vectorIndexCache;
    private final LibraryService libraryService;
    private final TransactionTemplate transactionTemplate;

    public CatalogService(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            LoanRecordRepository loanRecordRepository,
            PinnedBookRepository pinnedBookRepository,
            EmbeddingService embeddingService,
            VectorIndexCache vectorIndexCache,
            LibraryService libraryService,
            PlatformTransactionManager transactionManager) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRecordRepository = loanRecordRepository;
        this.pinnedBookRepository = pinnedBookRepository;
        this.embeddingService = embeddingService;
        this.vectorIndexCache = vectorIndexCache;
        this.libraryService = libraryService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public BookDetailResponse createBook(CreateBookRequest request) {
        String isbn = LibraryService.normalizeIsbn(request.isbn());
        if (isbn == null || isbn.isBlank()) {
            throw new BadRequestException("ISBN is required");
        }

        if (bookRepository.existsByIsbn(isbn)) {
            throw new ConflictException("Book with ISBN " + isbn + " already exists");
        }

        // Validate initial barcodes upfront
        if (request.initialBarcodes() != null) {
            Set<String> seen = new HashSet<>();
            for (String barcode : request.initialBarcodes()) {
                String cleanBarcode = barcode.trim();
                if (cleanBarcode.isBlank()) {
                    throw new BadRequestException("Barcode cannot be empty");
                }
                if (!seen.add(cleanBarcode)) {
                    throw new ConflictException("Duplicate barcode in request: " + cleanBarcode);
                }
                if (bookCopyRepository.existsByBarcode(cleanBarcode)) {
                    throw new ConflictException("Barcode already exists in inventory: " + cleanBarcode);
                }
            }
        }

        // STEP 1: Compute embedding OUTSIDE transaction (safe for pool-size = 1)
        String embeddingText = embeddingService.buildEmbeddingText(
            request.title(),
            request.author(),
            request.coverColor(),
            request.summary(),
            request.characters()
        );
        float[] embedding = embeddingService.generateEmbedding(embeddingText);
        String embeddingJson = vectorIndexCache.serializeVector(embedding);

        // STEP 2: Database transaction
        Book savedBook = transactionTemplate.execute(status -> {
            Book book = new Book(
                isbn,
                request.title().trim(),
                request.author().trim(),
                request.coverColor() != null ? request.coverColor().trim() : null,
                request.summary(),
                request.characters(),
                request.shelfLocation().trim(),
                embeddingJson
            );
            bookRepository.save(book);

            if (request.initialBarcodes() != null) {
                for (String barcode : request.initialBarcodes()) {
                    BookCopy copy = new BookCopy(barcode.trim(), book, CopyStatus.AVAILABLE);
                    bookCopyRepository.save(copy);
                }
            }
            return book;
        });

        // STEP 3: Update in-memory vector cache after DB commit
        vectorIndexCache.put(isbn, embedding);

        return libraryService.getBookByIsbn(isbn);
    }

    public BookDetailResponse updateBook(String rawIsbn, UpdateBookRequest request) {
        String isbn = LibraryService.normalizeIsbn(rawIsbn);
        bookRepository.findById(isbn)
            .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN " + isbn + " not found"));

        // STEP 1: Compute embedding OUTSIDE transaction
        String embeddingText = embeddingService.buildEmbeddingText(
            request.title(),
            request.author(),
            request.coverColor(),
            request.summary(),
            request.characters()
        );
        float[] embedding = embeddingService.generateEmbedding(embeddingText);
        String embeddingJson = vectorIndexCache.serializeVector(embedding);

        // STEP 2: Database transaction (update metadata without touching copies collection)
        transactionTemplate.executeWithoutResult(status -> {
            Book book = bookRepository.findById(isbn)
                .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN " + isbn + " not found"));
            book.setTitle(request.title().trim());
            book.setAuthor(request.author().trim());
            book.setCoverColor(request.coverColor() != null ? request.coverColor().trim() : null);
            book.setSummary(request.summary());
            book.setCharacters(request.characters());
            book.setShelfLocation(request.shelfLocation().trim());
            book.setEmbeddingJson(embeddingJson);
            bookRepository.save(book);
        });

        // STEP 3: Update in-memory vector cache
        vectorIndexCache.put(isbn, embedding);

        return libraryService.getBookByIsbn(isbn);
    }

    public void deleteBook(String rawIsbn) {
        String isbn = LibraryService.normalizeIsbn(rawIsbn);
        if (!bookRepository.existsByIsbn(isbn)) {
            throw new ResourceNotFoundException("Book with ISBN " + isbn + " not found");
        }

        // Check if any loan record (active or historical) exists for any copy of this book
        if (loanRecordRepository.existsByCopyBookIsbn(isbn)) {
            throw new ConflictException("Cannot delete book with existing active or historical loan records");
        }

        transactionTemplate.executeWithoutResult(status -> {
            pinnedBookRepository.deleteByBookIsbn(isbn);
            bookCopyRepository.deleteAllByBookIsbn(isbn);
            bookRepository.deleteById(isbn);
        });

        vectorIndexCache.remove(isbn);
    }

    @Transactional
    public void addCopies(String rawIsbn, AddCopiesRequest request) {
        String isbn = LibraryService.normalizeIsbn(rawIsbn);
        Book book = bookRepository.findById(isbn)
            .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN " + isbn + " not found"));

        Set<String> seen = new HashSet<>();
        for (String barcode : request.barcodes()) {
            String cleanBarcode = barcode.trim();
            if (cleanBarcode.isBlank()) {
                throw new BadRequestException("Barcode cannot be blank");
            }
            if (!seen.add(cleanBarcode)) {
                throw new ConflictException("Duplicate barcode in request: " + cleanBarcode);
            }
            if (bookCopyRepository.existsByBarcode(cleanBarcode)) {
                throw new ConflictException("Barcode already exists in inventory: " + cleanBarcode);
            }
        }

        for (String cleanBarcode : seen) {
            BookCopy copy = new BookCopy(cleanBarcode, book, CopyStatus.AVAILABLE);
            bookCopyRepository.save(copy);
        }
    }

    @Transactional
    public void updateCopyStatus(String barcode, UpdateCopyStatusRequest request) {
        BookCopy copy = bookCopyRepository.findByBarcode(barcode.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Copy with barcode " + barcode + " not found"));

        if (copy.getStatus() == CopyStatus.BORROWED) {
            throw new ConflictException("Cannot manually change status of a currently borrowed copy");
        }

        if (request.status() == CopyStatus.BORROWED) {
            throw new BadRequestException("Cannot manually set status to BORROWED; checkout through circulation");
        }

        copy.setStatus(request.status());
        bookCopyRepository.save(copy);
    }

    @Transactional
    public void deleteCopy(String barcode) {
        BookCopy copy = bookCopyRepository.findByBarcode(barcode.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Copy with barcode " + barcode + " not found"));

        if (copy.getStatus() == CopyStatus.BORROWED) {
            throw new ConflictException("Cannot delete copy that is currently borrowed");
        }

        if (loanRecordRepository.existsByCopyBarcode(copy.getBarcode())) {
            throw new ConflictException("Cannot delete copy that has active or historical loan records");
        }

        bookCopyRepository.delete(copy);
    }

    @Transactional(readOnly = true)
    public List<ShelfDetailDto> getShelfAnalytics() {
        List<ShelfBookSummaryDto> summaries = bookRepository.findShelfAnalytics();

        Map<String, List<ShelfBookSummaryDto>> grouped = summaries.stream()
            .collect(Collectors.groupingBy(
                ShelfBookSummaryDto::shelfLocation,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<ShelfDetailDto> result = new ArrayList<>();
        for (Map.Entry<String, List<ShelfBookSummaryDto>> entry : grouped.entrySet()) {
            String shelf = entry.getKey();
            List<ShelfBookSummaryDto> shelfBooks = entry.getValue();

            long totalCopies = 0;
            long availableCopies = 0;
            long borrowedCopies = 0;
            long maintenanceCopies = 0;

            List<ShelfBookItemDto> bookItems = new ArrayList<>();
            for (ShelfBookSummaryDto b : shelfBooks) {
                totalCopies += b.totalCopies();
                availableCopies += b.availableCopies();
                borrowedCopies += b.borrowedCopies();
                maintenanceCopies += b.maintenanceCopies();

                bookItems.add(new ShelfBookItemDto(
                    b.isbn(),
                    b.title(),
                    b.author(),
                    b.totalCopies(),
                    b.availableCopies(),
                    b.borrowedCopies(),
                    b.maintenanceCopies()
                ));
            }

            result.add(new ShelfDetailDto(
                shelf,
                totalCopies,
                availableCopies,
                borrowedCopies,
                maintenanceCopies,
                bookItems
            ));
        }

        return result;
    }
}
