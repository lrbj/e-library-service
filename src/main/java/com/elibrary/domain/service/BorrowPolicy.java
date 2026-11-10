package com.elibrary.domain.service;

import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Loan;
import com.elibrary.domain.repository.LoanRepository;

/**
 * Every borrowing rule lives here, in one place, and is expressed as a chain of
 * small checks. Adding a rule means adding a check, not touching LoanService.
 */
public class BorrowPolicy {

    /** One individual rule. Returning normally means "passed". */
    public interface Rule {
        void check(Book book, String userId, long activeLoansOfUser);
    }

    private final int maxLoansPerUser;
    private final LoanRepository loanRepository;

    public BorrowPolicy(int maxLoansPerUser, LoanRepository loanRepository) {
        this.maxLoansPerUser = maxLoansPerUser;
        this.loanRepository = loanRepository;
    }

    public void ensureCanBorrow(Book book, String userId) {
        boolean duplicated = loanRepository.existsActiveByBookIdAndUserId(book.getId(), userId);
        long activeLoansOfUser = loanRepository.findActiveByUserId(userId).size();
        long activeLoansOfBook = loanRepository.countActiveByBookId(book.getId());

        // Rule 1: no duplicate active loan of the same book by the same user.
        if (duplicated) {
            throw new DomainException(ErrorCode.DUPLICATE_ACTIVE_LOAN,
                    "You already have an active loan of this book");
        }
        // Rule 2: concurrent license cap.
        if (activeLoansOfBook >= book.getTotalLicenses()) {
            throw new DomainException(ErrorCode.BOOK_UNAVAILABLE,
                    "All " + book.getTotalLicenses() + " license(s) are currently on loan");
        }
        // Rule 3: per-user cap.
        if (activeLoansOfUser >= maxLoansPerUser) {
            throw new DomainException(ErrorCode.LOAN_LIMIT_EXCEEDED,
                    "You have reached the limit of " + maxLoansPerUser + " concurrent loans");
        }
    }

    public void ensureCanReturn(Loan loan, String userId) {
        if (!loan.getUserId().equals(userId)) {
            // 403, not 404: the loan exists, the caller simply may not touch it.
            throw new DomainException(ErrorCode.LOAN_NOT_OWNED, "This loan belongs to another user");
        }
        if (loan.isReturned()) {
            throw new DomainException(ErrorCode.LOAN_ALREADY_RETURNED, "This loan has already been returned");
        }
    }
}