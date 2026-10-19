package com.library.backend.controller;

import com.library.backend.dto.AuthDto.AdminAuthResponse;
import com.library.backend.dto.AuthDto.CreateAdminRequest;
import com.library.backend.dto.BookDto.*;
import com.library.backend.dto.LoanDto.*;
import com.library.backend.dto.MemberDto.*;
import com.library.backend.dto.MemberSummaryDto;
import com.library.backend.dto.ShelfDetailDto;
import com.library.backend.service.CatalogService;
import com.library.backend.service.AuthService;
import com.library.backend.service.CirculationService;
import com.library.backend.service.MemberAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MemberAdminService memberAdminService;
    private final CatalogService catalogService;
    private final CirculationService circulationService;
    private final AuthService authService;

    public AdminController(
            MemberAdminService memberAdminService,
            CatalogService catalogService,
            CirculationService circulationService,
            AuthService authService) {
        this.memberAdminService = memberAdminService;
        this.catalogService = catalogService;
        this.circulationService = circulationService;
        this.authService = authService;
    }

    // ==========================================
    // Admin Creation
    // ==========================================

    @PostMapping("/users/create-admin")
    public ResponseEntity<AdminAuthResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        AdminAuthResponse response = authService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // Member Management
    // ==========================================

    @PostMapping("/members")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
        MemberResponse response = memberAdminService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(memberAdminService.updateMember(id, request));
    }

    @GetMapping("/members")
    public ResponseEntity<List<MemberSummaryDto>> listMembers() {
        return ResponseEntity.ok(memberAdminService.listMembers());
    }

    // ==========================================
    // Book & Copy CRUD
    // ==========================================

    @PostMapping("/books")
    public ResponseEntity<BookDetailResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookDetailResponse response = catalogService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/books/{isbn}")
    public ResponseEntity<BookDetailResponse> updateBook(
            @PathVariable String isbn,
            @Valid @RequestBody UpdateBookRequest request) {
        return ResponseEntity.ok(catalogService.updateBook(isbn, request));
    }

    @DeleteMapping("/books/{isbn}")
    public ResponseEntity<Void> deleteBook(@PathVariable String isbn) {
        catalogService.deleteBook(isbn);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/books/{isbn}/copies")
    public ResponseEntity<Void> addCopies(
            @PathVariable String isbn,
            @Valid @RequestBody AddCopiesRequest request) {
        catalogService.addCopies(isbn, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/copies/{barcode}/status")
    public ResponseEntity<Void> updateCopyStatus(
            @PathVariable String barcode,
            @Valid @RequestBody UpdateCopyStatusRequest request) {
        catalogService.updateCopyStatus(barcode, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/copies/{barcode}")
    public ResponseEntity<Void> deleteCopy(@PathVariable String barcode) {
        catalogService.deleteCopy(barcode);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Shelf Analytics
    // ==========================================

    @GetMapping("/shelves")
    public ResponseEntity<List<ShelfDetailDto>> getShelves() {
        return ResponseEntity.ok(catalogService.getShelfAnalytics());
    }

    // ==========================================
    // Circulation Control
    // ==========================================

    @PostMapping("/loans/checkout")
    public ResponseEntity<LoanActionResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        LoanActionResponse response = circulationService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/loans/return")
    public ResponseEntity<LoanActionResponse> returnBook(@Valid @RequestBody ReturnRequest request) {
        return ResponseEntity.ok(circulationService.returnBook(request));
    }

    @PatchMapping("/loans/{loanId}/extend")
    public ResponseEntity<LoanActionResponse> extendLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody ExtendLoanRequest request) {
        return ResponseEntity.ok(circulationService.extendLoan(loanId, request));
    }

    @GetMapping("/loans/active")
    public ResponseEntity<List<ActiveLoanResponse>> getActiveLoans() {
        return ResponseEntity.ok(circulationService.getActiveLoans());
    }
}
