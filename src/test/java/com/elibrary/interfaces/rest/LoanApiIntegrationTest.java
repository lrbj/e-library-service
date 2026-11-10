package com.elibrary.interfaces.rest;

import com.elibrary.domain.model.Book;
import com.elibrary.domain.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests. They verify the contract that clients actually call:
 * status codes, the error envelope, and the X-User-Id requirement.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoanApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    private Long singleLicenseBookId;

    @BeforeEach
    void setUp() {
        // Unique ISBN per test: the whole suite shares one in-memory database,
        // and the unique constraint on isbn is part of the schema under test.
        // Kept short because the column is VARCHAR(20) like a real ISBN.
        singleLicenseBookId = bookRepository.save(new Book(
                "T" + System.nanoTime() % 1_000_000_000L,
                "Concurrency Under Test", "Author", "Pub", "test", null, null, 1)).getId();
    }

    @Test
    void fullBorrowAndReturnFlow() throws Exception {
        String body = mockMvc.perform(post("/api/v1/books/" + singleLicenseBookId + "/loans")
                        .header(UserContextInterceptor.USER_HEADER, "alice"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.overdue").value(false))
                .andReturn().getResponse().getContentAsString();

        long loanId = Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/v1/loans").header(UserContextInterceptor.USER_HEADER, "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(singleLicenseBookId.intValue()));

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return")
                        .header(UserContextInterceptor.USER_HEADER, "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        mockMvc.perform(get("/api/v1/loans").header(UserContextInterceptor.USER_HEADER, "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void secondBorrowOfSingleLicenseBookIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/books/" + singleLicenseBookId + "/loans")
                        .header(UserContextInterceptor.USER_HEADER, "borrower-a"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/books/" + singleLicenseBookId + "/loans")
                        .header(UserContextInterceptor.USER_HEADER, "borrower-b"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_UNAVAILABLE"));
    }

    @Test
    void sameUserCannotBorrowTheSameBookTwice() throws Exception {
        mockMvc.perform(post("/api/v1/books/" + singleLicenseBookId + "/loans")
                        .header(UserContextInterceptor.USER_HEADER, "carol"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/books/" + singleLicenseBookId + "/loans")
                        .header(UserContextInterceptor.USER_HEADER, "carol"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACTIVE_LOAN"));
    }

    @Test
    void missingUserHeaderIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/loans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unknownBookReturns404WithEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/books/999999")
                        .header(UserContextInterceptor.USER_HEADER, "alice")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/books/999999"));
    }
}