package com.elibrary.interfaces.rest;

import com.elibrary.domain.exception.ErrorCode;

import java.time.Instant;

/**
 * Single error envelope for every failure the API can produce.
 */
public class ApiError {

    private final String code;
    private final String message;
    private final Instant timestamp;
    private final String path;

    public ApiError(ErrorCode code, String message, String path) {
        this.code = code.name();
        this.message = message;
        this.timestamp = Instant.now();
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }
}