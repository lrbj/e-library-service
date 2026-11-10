package com.elibrary.application.view;

import com.elibrary.domain.model.Loan;
import com.elibrary.domain.model.LoanStatus;

import java.time.Instant;

/**
 * Read model for a loan. {@code overdue} is computed from the clock on every read,
 * which is why no scheduled job is needed anywhere in this service.
 */
public class LoanView {

    private final Long id;
    private final Long bookId;
    private final String bookTitle;
    private final String userId;
    private final Instant borrowedAt;
    private final Instant dueAt;
    private final Instant returnedAt;
    private final LoanStatus status;
    private final boolean overdue;

    private LoanView(Loan loan, String bookTitle, boolean overdue) {
        this.id = loan.getId();
        this.bookId = loan.getBookId();
        this.bookTitle = bookTitle;
        this.userId = loan.getUserId();
        this.borrowedAt = loan.getBorrowedAt();
        this.dueAt = loan.getDueAt();
        this.returnedAt = loan.getReturnedAt();
        this.status = loan.getStatus();
        this.overdue = overdue;
    }

    public static LoanView of(Loan loan, String bookTitle, Instant now) {
        return new LoanView(loan, bookTitle, loan.isOverdue(now));
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public boolean isOverdue() {
        return overdue;
    }
}