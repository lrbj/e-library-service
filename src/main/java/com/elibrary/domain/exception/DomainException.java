package com.elibrary.domain.exception;

/**
 * Base type for every rule violation raised by the domain.
 * Web layer translates it into an HTTP response; nothing else needs to catch it.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}