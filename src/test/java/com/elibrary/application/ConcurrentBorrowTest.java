package com.elibrary.application;

import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single most valuable test in this project: N threads race for the last
 * available license, and exactly one must win.
 * <p>
 * Note: MockMvc is single-threaded by design, so the concurrency test calls the
 * service directly. That is also the honest place to test it — the guarantee is a
 * transactional one, not an HTTP one.
 */
@SpringBootTest
class ConcurrentBorrowTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void onlyOneOfTenConcurrentBorrowsSucceeds() throws Exception {
        Book book = bookRepository.save(new Book(
                "CONC-001", "Last Licence", "Author", "Pub", "test", null, null, 1));

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final String userId = "user-" + i;
            tasks.add(() -> {
                startGate.await();
                try {
                    loanService.borrow(book.getId(), userId);
                    successes.incrementAndGet();
                } catch (DomainException e) {
                    assertThat(e.getErrorCode()).isIn(ErrorCode.BOOK_UNAVAILABLE,
                            ErrorCode.DUPLICATE_ACTIVE_LOAN);
                    conflicts.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }
        startGate.countDown();
        for (Future<Void> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threads - 1);
    }
}