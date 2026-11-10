package com.elibrary.infrastructure.persistence;

import com.elibrary.domain.model.Book;
import com.elibrary.domain.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class JpaBookRepository implements BookRepository {

    private final BookJpaRepository delegate;

    public JpaBookRepository(BookJpaRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Book> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    @Transactional
    public Optional<Book> findByIdForUpdate(Long id) {
        return delegate.findByIdForUpdate(id);
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        return delegate.existsByIsbn(isbn);
    }

    @Override
    public Page<Book> search(String keyword, String category, Pageable pageable) {
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like),
                        cb.like(cb.lower(root.get("isbn")), like)));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return delegate.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public Book save(Book book) {
        return delegate.save(book);
    }
}