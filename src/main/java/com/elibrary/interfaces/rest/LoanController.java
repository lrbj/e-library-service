package com.elibrary.interfaces.rest;

import com.elibrary.application.LoanService;
import com.elibrary.application.view.LoanView;
import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Loans", description = "Borrow and return titles")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/books/{bookId}/loans")
    @Operation(summary = "Borrow a book",
            description = "Creates an active loan. Fails with 409 when the license cap is reached, "
                    + "the user already holds this book, or the per-user cap is exceeded.")
    public ResponseEntity<LoanView> borrow(@PathVariable Long bookId) {
        String userId = currentUser();
        LoanView loan = loanService.borrow(bookId, userId);
        return ResponseEntity.created(URI.create("/api/v1/loans/" + loan.getId())).body(loan);
    }

    @PostMapping("/loans/{loanId}/return")
    @Operation(summary = "Return a book",
            description = "Explicit action rather than DELETE: the loan record survives as history.")
    public ResponseEntity<LoanView> giveBack(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.giveBack(loanId, currentUser()));
    }

    @GetMapping("/loans")
    @Operation(summary = "My loans", description = "Loans of the caller identified by X-User-Id.")
    public ResponseEntity<List<LoanView>> myLoans(
            @Parameter(description = "true = only loans still held; false = full history")
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(loanService.myLoans(currentUser(), activeOnly));
    }

    private String currentUser() {
        String userId = UserContext.require();
        if (userId == null) {
            throw new DomainException(ErrorCode.UNAUTHORIZED,
                    "Missing required header " + UserContextInterceptor.USER_HEADER);
        }
        return userId;
    }
}