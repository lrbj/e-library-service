package com.elibrary.infrastructure.persistence;

import com.elibrary.domain.model.Loan;
import com.elibrary.domain.model.LoanStatus;
import com.elibrary.domain.repository.LoanRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class JpaLoanRepository implements LoanRepository {

    private final LoanJpaRepository delegate;

    public JpaLoanRepository(LoanJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public Loan save(Loan loan) {
        return delegate.save(loan);
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public long countActiveByBookId(Long bookId) {
        return delegate.countByBookIdAndStatus(bookId, LoanStatus.ACTIVE);
    }

    @Override
    public Map<Long, Long> countActiveByBookIds(Collection<Long> bookIds) {
        Map<Long, Long> result = new HashMap<>();
        if (bookIds == null || bookIds.isEmpty()) {
            return result;
        }
        for (LoanJpaRepository.BookLoanCount row : delegate.countGroupedByBookId(bookIds, LoanStatus.ACTIVE)) {
            result.put(row.getBookId(), row.getTotal());
        }
        return result;
    }

    @Override
    public List<Loan> findActiveByUserId(String userId) {
        return delegate.findByUserIdAndStatusOrderByBorrowedAtDesc(userId, LoanStatus.ACTIVE);
    }

    @Override
    public List<Loan> findByUserId(String userId) {
        return delegate.findByUserIdOrderByBorrowedAtDesc(userId);
    }

    @Override
    public boolean existsActiveByBookIdAndUserId(Long bookId, String userId) {
        return delegate.existsByBookIdAndUserIdAndStatus(bookId, userId, LoanStatus.ACTIVE);
    }
}