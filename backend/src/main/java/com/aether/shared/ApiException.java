package com.aether.shared;

import org.springframework.http.HttpStatus;

/**
 * Domain error carrying the HTTP status and a stable machine-readable code.
 * The code — not the message — is what the frontend translates (PT/EN).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
