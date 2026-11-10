package com.elibrary.interfaces.rest;

import com.elibrary.application.BookQueryService;
import com.elibrary.application.view.BookView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Browse and inspect the catalogue")
public class BookController {

    private final BookQueryService bookQueryService;

    public BookController(BookQueryService bookQueryService) {
        this.bookQueryService = bookQueryService;
    }

    @GetMapping
    @Operation(summary = "Browse books",
            description = "Paged catalogue search. keyword matches title, author or ISBN (case-insensitive).")
    public ResponseEntity<Page<BookView>> browse(
            @Parameter(description = "Free-text query") @RequestParam(required = false) String keyword,
            @Parameter(description = "Exact category filter") @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "title"));
        return ResponseEntity.ok(bookQueryService.browse(keyword, category, pageable));
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Book details", description = "Includes derived license availability.")
    public ResponseEntity<BookView> get(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookQueryService.get(bookId));
    }
}