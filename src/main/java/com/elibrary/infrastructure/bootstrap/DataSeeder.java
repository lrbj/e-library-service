package com.elibrary.infrastructure.bootstrap;

import com.elibrary.domain.model.Book;
import com.elibrary.domain.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Small demo catalogue so the API is explorable straight after start-up.
 * Disable with library.seed.enabled=false.
 */
@Configuration
@ConditionalOnProperty(name = "library.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public ApplicationRunner seedBooks(BookRepository bookRepository) {
        return args -> {
            seed(bookRepository, "978-0132350884", "Clean Code", "Robert C. Martin",
                    "Prentice Hall", "software", 2);
            seed(bookRepository, "978-0201633610", "Design Patterns", "Erich Gamma et al.",
                    "Addison-Wesley", "software", 1);
            seed(bookRepository, "978-0321125217", "Domain-Driven Design", "Eric Evans",
                    "Addison-Wesley", "software", 1);
            seed(bookRepository, "978-0306406157", "The Art of Computer Programming", "Donald E. Knuth",
                    "Addison-Wesley", "computer-science", 1);
            seed(bookRepository, "978-0262033848", "Introduction to Algorithms", "Cormen et al.",
                    "MIT Press", "computer-science", 3);
            seed(bookRepository, "978-0131103627", "The C Programming Language", "Kernighan & Ritchie",
                    "Prentice Hall", "software", 1);
            seed(bookRepository, "978-0553380163", "A Brief History of Time", "Stephen Hawking",
                    "Bantam", "science", 1);
            log.info("Demo catalogue seeded");
        };
    }

    private void seed(BookRepository repo, String isbn, String title, String author,
                      String publisher, String category, int licenses) {
        // Idempotent: several Spring test contexts may boot against the same
        // in-memory database in one JVM.
        if (repo.existsByIsbn(isbn)) {
            return;
        }
        repo.save(new Book(isbn, title, author, publisher, category,
                title + " — demo entry", "/content/" + isbn + ".pdf", licenses));
    }
}