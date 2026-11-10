package com.elibrary.domain.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

/**
 * A catalogued digital title.
 * <p>
 * {@code totalLicenses} is the number of simultaneous loans the library owns.
 * The number of <em>currently available</em> licenses is deliberately NOT stored:
 * it is derived from the active loans, so there is only one source of truth.
 */
@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(length = 120)
    private String publisher;

    @Column(length = 60)
    private String category;

    @Column(length = 2000)
    private String description;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "total_licenses", nullable = false)
    private int totalLicenses;

    protected Book() {
        // for JPA
    }

    public Book(String isbn, String title, String author, String publisher,
                String category, String description, String contentUrl, int totalLicenses) {
        if (totalLicenses < 1) {
            throw new IllegalArgumentException("totalLicenses must be >= 1");
        }
        this.isbn = Objects.requireNonNull(isbn);
        this.title = Objects.requireNonNull(title);
        this.author = Objects.requireNonNull(author);
        this.publisher = publisher;
        this.category = category;
        this.description = description;
        this.contentUrl = contentUrl;
        this.totalLicenses = totalLicenses;
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
}