package com.elibrary.domain.repository;

import com.elibrary.domain.model.Loan;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoanRepository {

    Loan save(Loan loan);

    Optional<Loan> findById(Long id);

    /** Number of loans currently holding a license for the given book. */
    long countActiveByBookId(Long bookId);

    /**
     * Bulk variant used by list endpoints so that rendering a page of books
     * costs one extra query instead of one per row (avoids an N+1).
     */
    Map<Long, Long> countActiveByBookIds(Collection<Long> bookIds);

    /** Active loans of one user, newest first. */
    List<Loan> findActiveByUserId(String userId);

    /** All loans of one user, newest first. */
    List<Loan> findByUserId(String userId);

    /** True when this user already holds an active loan of this book. */
    boolean existsActiveByBookIdAndUserId(Long bookId, String userId);
}