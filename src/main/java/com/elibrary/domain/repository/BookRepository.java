package com.elibrary.domain.repository;

import com.elibrary.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Domain-facing port. The application layer depends on this interface only,
 * so persistence technology stays replaceable.
 */
public interface BookRepository {

    Optional<Book> findById(Long id);

    boolean existsByIsbn(String isbn);

    /**
     * Reads a book and takes a write lock on its row until the current
     * transaction ends. Borrowing goes through this method so that two concurrent
     * requests against the last available license are serialised by the database
     * instead of both passing the "is it available?" check.
     */
    Optional<Book> findByIdForUpdate(Long id);

    Page<Book> search(String keyword, String category, Pageable pageable);

    Book save(Book book);
}