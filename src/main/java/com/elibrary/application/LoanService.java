package com.elibrary.application;

import com.elibrary.application.view.LoanView;
import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Loan;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.LoanRepository;
import com.elibrary.domain.service.BorrowPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoanService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final BorrowPolicy borrowPolicy;
    private final Clock clock;
    private final Duration loanPeriod;

    public LoanService(BookRepository bookRepository,
                       LoanRepository loanRepository,
                       BorrowPolicy borrowPolicy,
                       Clock clock,
                       @Value("${library.loan.period-days:14}") long periodDays) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.borrowPolicy = borrowPolicy;
        this.clock = clock;
        this.loanPeriod = Duration.ofDays(periodDays);
    }

    public LoanView borrow(Long bookId, String userId) {
        // Pessimistic lock, not optimistic: with a single hot book the natural
        // contention is "everyone wants the last license", so one short row lock
        // is cheaper than a retry loop, and it makes the cap check unconditionally
        // correct inside the transaction.
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new DomainException(ErrorCode.BOOK_NOT_FOUND, "Book " + bookId + " not found"));

        borrowPolicy.ensureCanBorrow(book, userId);

        Loan loan = Loan.open(bookId, userId, Instant.now(clock), loanPeriod);
        try {
            Loan saved = loanRepository.save(loan);
            return LoanView.of(saved, book.getTitle(), Instant.now(clock));
        } catch (DataIntegrityViolationException e) {
            // Unique index on (book_id, user_id) for active loans fired: a concurrent
            // request from the same user won the race. Report it as the business error.
            throw new DomainException(ErrorCode.DUPLICATE_ACTIVE_LOAN,
                    "You already have an active loan of this book");
        }
    }

    public LoanView giveBack(Long loanId, String userId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new DomainException(ErrorCode.LOAN_NOT_FOUND, "Loan " + loanId + " not found"));

        borrowPolicy.ensureCanReturn(loan, userId);
        loan.markReturned(Instant.now(clock));
        loanRepository.save(loan);

        String title = bookRepository.findById(loan.getBookId())
                .map(Book::getTitle)
                .orElse(null);
        return LoanView.of(loan, title, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public List<LoanView> myLoans(String userId, boolean activeOnly) {
        List<Loan> loans = activeOnly
                ? loanRepository.findActiveByUserId(userId)
                : loanRepository.findByUserId(userId);
        Instant now = Instant.now(clock);
        return loans.stream()
                .map(loan -> LoanView.of(loan, titleOf(loan.getBookId()), now))
                .collect(Collectors.toList());
    }

    private String titleOf(Long bookId) {
        return bookRepository.findById(bookId).map(Book::getTitle).orElse(null);
    }
}