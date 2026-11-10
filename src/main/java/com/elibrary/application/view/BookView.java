package com.elibrary.application.view;

import com.elibrary.domain.model.Book;

import java.time.Instant;
import java.util.Map;

/**
 * Read model: a book as the API shows it, including the license state that is
 * derived rather than stored.
 */
public class BookView {

    private final Long id;
    private final String isbn;
    private final String title;
    private final String author;
    private final String publisher;
    private final String category;
    private final String description;
    private final String contentUrl;
    private final int totalLicenses;
    private final int availableCount;

    private BookView(Book book, int availableCount) {
        this.id = book.getId();
        this.isbn = book.getIsbn();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.publisher = book.getPublisher();
        this.category = book.getCategory();
        this.description = book.getDescription();
        this.contentUrl = book.getContentUrl();
        this.totalLicenses = book.getTotalLicenses();
        this.availableCount = availableCount;
    }

    public static BookView of(Book book, long activeLoans) {
        int available = (int) Math.max(0, book.getTotalLicenses() - activeLoans);
        return new BookView(book, available);
    }

    public static BookView of(Book book, Map<Long, Long> activeLoansByBookId) {
        return of(book, activeLoansByBookId.getOrDefault(book.getId(), 0L));
    }

    public boolean isAvailable() {
        return availableCount > 0;
    }

    public Long getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public int getTotalLicenses() {
        return totalLicenses;
    }

    public int getAvailableCount() {
        return availableCount;
    }
}