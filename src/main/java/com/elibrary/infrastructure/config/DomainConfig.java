package com.elibrary.infrastructure.config;

import com.elibrary.domain.repository.LoanRepository;
import com.elibrary.domain.service.BorrowPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DomainConfig {

    /**
     * The domain never calls Instant.now() directly; a Clock is injected so tests
     * can move time and assert on overdue behaviour deterministically.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public BorrowPolicy borrowPolicy(LoanRepository loanRepository,
                                     @Value("${library.loan.max-per-user:5}") int maxPerUser) {
        return new BorrowPolicy(maxPerUser, loanRepository);
    }
}