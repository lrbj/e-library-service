package com.elibrary.domain.model;

import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ErrorCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A loan is the record of "user X held book Y from A to B".
 * <p>
 * There is deliberately no OVERDUE status: overdue is a function of the clock,
 * not of a background job. It is derived on read via {@link #isOverdue(Instant)}.
 */
@Entity
@Table(name = "loan", indexes = {
        @Index(name = "idx_loan_book", columnList = "book_id"),
        @Index(name = "idx_loan_user", columnList = "user_id")
})
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "borrowed_at", nullable = false)
    private Instant borrowedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    /** Persisted for query filtering; kept in sync with {@link #returnedAt} only. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LoanStatus status;

    protected Loan() {
        // for JPA
    }

    private Loan(Long bookId, String userId, Instant now, Duration period) {
        this.bookId = Objects.requireNonNull(bookId);
        this.userId = Objects.requireNonNull(userId);
        this.borrowedAt = Objects.requireNonNull(now);
        this.dueAt = now.plus(period);
        this.status = LoanStatus.ACTIVE;
    }

    public static Loan open(Long bookId, String userId, Instant now, Duration loanPeriod) {
        return new Loan(bookId, userId, now, loanPeriod);
    }

    public void markReturned(Instant now) {
        if (isReturned()) {
            throw new DomainException(ErrorCode.LOAN_ALREADY_RETURNED,
                    "Loan " + id + " has already been returned");
        }
        this.returnedAt = Objects.requireNonNull(now);
        this.status = LoanStatus.RETURNED;
    }

    public boolean isActive() {
        return returnedAt == null;
    }

    public boolean isReturned() {
        return returnedAt != null;
    }

    public boolean isOverdue(Instant now) {
        return isActive() && now.isAfter(dueAt);
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
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
}