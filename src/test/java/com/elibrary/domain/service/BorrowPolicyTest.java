package com.elibrary.domain.service;

import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Loan;
import com.elibrary.domain.repository.LoanRepository;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: no Spring context, no database.
 * Business rules are the thing most likely to regress, so they are tested in isolation.
 */
class BorrowPolicyTest {

    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final BorrowPolicy policy = new BorrowPolicy(5, loanRepository);

    private Book book(int licenses) {
        Book b = new Book("isbn-" + licenses, "Title", "Author", "Pub", "cat", null, null, licenses);
        // ISBN must be unique in the DB, not in a unit test; id is null-safe here.
        return b;
    }

    @Test
    void rejectsDuplicateActiveLoan() {
        when(loanRepository.existsActiveByBookIdAndUserId(any(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> policy.ensureCanBorrow(book(2), "alice"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ACTIVE_LOAN);
    }

    @Test
    void rejectsWhenAllLicensesAreOnLoan() {
        when(loanRepository.countActiveByBookId(any())).thenReturn(1L);
        when(loanRepository.findActiveByUserId(anyString())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> policy.ensureCanBorrow(book(1), "alice"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_UNAVAILABLE);
    }

    @Test
    void rejectsWhenUserReachedLoanLimit() {
        when(loanRepository.countActiveByBookId(any())).thenReturn(0L);
        when(loanRepository.findActiveByUserId(anyString()))
                .thenReturn(Collections.nCopies(5, mock(Loan.class)));

        assertThatThrownBy(() -> policy.ensureCanBorrow(book(3), "alice"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOAN_LIMIT_EXCEEDED);
    }

    @Test
    void allowsBorrowWhenEveryRulePasses() {
        when(loanRepository.countActiveByBookId(any())).thenReturn(0L);
        when(loanRepository.findActiveByUserId(anyString())).thenReturn(Collections.emptyList());

        policy.ensureCanBorrow(book(1), "alice");
        assertThat(true).isTrue(); // reaching here means no exception was thrown
    }

    @Test
    void refusesToReturnSomeoneElsesLoan() {
        Loan loan = mock(Loan.class);
        when(loan.getUserId()).thenReturn("bob");

        assertThatThrownBy(() -> policy.ensureCanReturn(loan, "alice"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOAN_NOT_OWNED);
    }
}