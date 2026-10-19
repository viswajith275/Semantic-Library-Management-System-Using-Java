package com.library.backend.controller;

import com.library.backend.dto.BookDto.BookDetailResponse;
import com.library.backend.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookDetailResponse>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "10") int topK) {
        return ResponseEntity.ok(libraryService.search(query, topK));
    }

    @GetMapping("/books/{isbn}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable String isbn) {
        return ResponseEntity.ok(libraryService.getBookByIsbn(isbn));
    }
}
