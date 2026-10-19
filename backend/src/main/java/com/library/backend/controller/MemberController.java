package com.library.backend.controller;

import com.library.backend.dto.BookDto.BookDetailResponse;
import com.library.backend.dto.LoanDto.MemberLoanResponse;
import com.library.backend.entity.User;
import com.library.backend.security.SecurityUtils;
import com.library.backend.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/pins/{isbn}")
    public ResponseEntity<Void> pinBook(@PathVariable String isbn) {
        User currentUser = SecurityUtils.getCurrentUser();
        memberService.pinBook(currentUser, isbn);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/pins/{isbn}")
    public ResponseEntity<Void> unpinBook(@PathVariable String isbn) {
        User currentUser = SecurityUtils.getCurrentUser();
        memberService.unpinBook(currentUser, isbn);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pins")
    public ResponseEntity<List<BookDetailResponse>> getPinnedBooks() {
        User currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(memberService.getPinnedBooks(currentUser));
    }

    @GetMapping("/loans")
    public ResponseEntity<List<MemberLoanResponse>> getActiveLoans() {
        User currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(memberService.getActiveLoans(currentUser));
    }
}
