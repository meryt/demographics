package com.meryt.demographics.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { ResourceNotFoundException.class })
    protected ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException e) {
        return buildResponseEntity(new RestErrorMessage(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(value = { BadRequestException.class })
    protected ResponseEntity<Object> handleBadRequest(BadRequestException e) {
        return buildResponseEntity(new RestErrorMessage(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(value = { ConflictException.class })
    protected ResponseEntity<Object> handleConflict(ConflictException e) {
        return buildResponseEntity(new RestErrorMessage(HttpStatus.CONFLICT, e.getMessage()));
    }

    private ResponseEntity<Object> buildResponseEntity(RestErrorMessage error) {
        return new ResponseEntity<>(error, error.getStatus());
    }
}
