package com.elibrary.infrastructure.persistence;

import com.elibrary.domain.model.Loan;
import com.elibrary.domain.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LoanJpaRepository extends JpaRepository<Loan, Long> {

    long countByBookIdAndStatus(Long bookId, LoanStatus status);

    /** One grouped query for a whole page of books. */
    @Query("select l.bookId as bookId, count(l) as total from Loan l "
            + "where l.status = :status and l.bookId in :bookIds group by l.bookId")
    List<BookLoanCount> countGroupedByBookId(@Param("bookIds") Collection<Long> bookIds,
                                             @Param("status") LoanStatus status);

    List<Loan> findByUserIdOrderByBorrowedAtDesc(String userId);

    List<Loan> findByUserIdAndStatusOrderByBorrowedAtDesc(String userId, LoanStatus status);

    boolean existsByBookIdAndUserIdAndStatus(Long bookId, String userId, LoanStatus status);

    /** Projection for the grouped count query. */
    interface BookLoanCount {
        Long getBookId();

        long getTotal();
    }
}