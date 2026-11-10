package com.elibrary.application;

import com.elibrary.application.view.BookView;
import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ErrorCode;
import com.elibrary.domain.model.Book;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.LoanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookQueryService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookQueryService(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    public Page<BookView> browse(String keyword, String category, Pageable pageable) {
        Page<Book> page = bookRepository.search(keyword, category, pageable);
        List<Long> ids = page.getContent().stream().map(Book::getId).collect(Collectors.toList());
        // One aggregate query for the whole page instead of one per book.
        Map<Long, Long> activeLoans = ids.isEmpty()
                ? java.util.Collections.emptyMap()
                : loanRepository.countActiveByBookIds(ids);
        return page.map(book -> BookView.of(book, activeLoans));
    }

    public BookView get(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new DomainException(ErrorCode.BOOK_NOT_FOUND, "Book " + bookId + " not found"));
        return BookView.of(book, loanRepository.countActiveByBookId(bookId));
    }
}