package com.kanban.backend.common;

import org.springframework.http.HttpStatus;

/**
 * A business-logic error that should be reported to the client with a
 * specific HTTP status and message (e.g. "이미 가입된 이메일입니다").
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
