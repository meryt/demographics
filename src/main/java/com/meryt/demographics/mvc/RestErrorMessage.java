package com.meryt.demographics.mvc;

import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RestErrorMessage {

    private final HttpStatus status;
    private final String message;
    private final LocalDateTime timestamp;
    private final String debugMessage;

    public RestErrorMessage(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.debugMessage = null;
    }

    public RestErrorMessage(HttpStatus status, String message, Throwable ex) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.debugMessage = ex.getLocalizedMessage();
    }
}
