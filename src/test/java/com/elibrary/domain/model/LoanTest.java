package com.elibrary.domain.model;

import com.elibrary.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration PERIOD = Duration.ofDays(14);

    @Test
    void newLoanIsActiveAndNotOverdue() {
        Loan loan = Loan.open(1L, "alice", NOW, PERIOD);

        assertThat(loan.isActive()).isTrue();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getDueAt()).isEqualTo(NOW.plus(PERIOD));
        assertThat(loan.isOverdue(NOW.plus(Duration.ofDays(1)))).isFalse();
    }

    @Test
    void overdueIsDerivedFromTheClockNotStored() {
        Loan loan = Loan.open(1L, "alice", NOW, PERIOD);

        assertThat(loan.isOverdue(NOW.plus(Duration.ofDays(15)))).isTrue();
        // The persisted status never becomes "overdue"; the clock decides.
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    void returningClearsActiveStateAndStopsOverdue() {
        Loan loan = Loan.open(1L, "alice", NOW, PERIOD);
        loan.markReturned(NOW.plus(Duration.ofDays(30)));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(loan.isOverdue(NOW.plus(Duration.ofDays(60)))).isFalse();
    }

    @Test
    void returningTwiceIsRejected() {
        Loan loan = Loan.open(1L, "alice", NOW, PERIOD);
        loan.markReturned(NOW);

        assertThatThrownBy(() -> loan.markReturned(NOW))
                .isInstanceOf(DomainException.class);
    }
}