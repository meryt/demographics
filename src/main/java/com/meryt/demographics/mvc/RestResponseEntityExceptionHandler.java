package com.meryt.demographics.mvc;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { IllegalArgumentException.class })
    protected ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponseEntity(new RestErrorMessage(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    private ResponseEntity<Object> buildResponseEntity(RestErrorMessage error) {
        return new ResponseEntity<>(error, error.getStatus());
    }
}