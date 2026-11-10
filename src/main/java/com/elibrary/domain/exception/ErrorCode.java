package com.elibrary.domain.exception;

/**
 * Stable, machine-readable error codes. The API contract exposes these;
 * HTTP status mapping lives in the web layer, not here.
 */
public enum ErrorCode {
    BOOK_NOT_FOUND(404),
    BOOK_UNAVAILABLE(409),
    DUPLICATE_ACTIVE_LOAN(409),
    LOAN_LIMIT_EXCEEDED(409),
    LOAN_NOT_FOUND(404),
    LOAN_NOT_OWNED(403),
    LOAN_ALREADY_RETURNED(409),
    VALIDATION_ERROR(400),
    UNAUTHORIZED(401);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}