package com.aditya.jobtracker.controller;

import com.aditya.jobtracker.dto.ErrorResponse;
import com.aditya.jobtracker.exception.ApiException;
import com.aditya.jobtracker.exception.DatabaseException;
import com.aditya.jobtracker.exception.DuplicateJobException;
import com.aditya.jobtracker.exception.DuplicateUserException;
import com.aditya.jobtracker.exception.InvalidStatusChangeException;
import com.aditya.jobtracker.exception.JobNotFoundException;
import com.aditya.jobtracker.exception.WatcherException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// One place that turns our exceptions into proper HTTP answers.
// @RestControllerAdvice = "watch ALL controllers; when one throws an exception, come here".
// Without this, every exception would become an ugly 500 error.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(JobNotFoundException e) {
        return reply(HttpStatus.NOT_FOUND, e.getMessage());                 // 404
    }

    @ExceptionHandler(DuplicateJobException.class)
    public ResponseEntity<ErrorResponse> duplicate(DuplicateJobException e) {
        return reply(HttpStatus.CONFLICT, e.getMessage());                  // 409
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponse> duplicateUser(DuplicateUserException e) {
        return reply(HttpStatus.CONFLICT, e.getMessage());                  // 409
    }

    @ExceptionHandler({InvalidStatusChangeException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception e) {
        return reply(HttpStatus.BAD_REQUEST, e.getMessage());               // 400
    }

    // The JSON body is missing or broken
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> badJson(HttpMessageNotReadableException e) {
        return reply(HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON.");
    }

    // Example: /api/jobs/abc where a number was expected
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> badId(MethodArgumentTypeMismatchException e) {
        return reply(HttpStatus.BAD_REQUEST, "The id in the URL must be a number.");
    }

    // Adzuna is down, the key is wrong, or the limit is reached.
    // 502 Bad Gateway = "the problem is in another server we depend on, not in our own code".
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> adzuna(ApiException e) {
        return reply(HttpStatus.BAD_GATEWAY, e.getMessage());               // 502
    }

    @ExceptionHandler(WatcherException.class)
    public ResponseEntity<ErrorResponse> watcher(WatcherException e) {
        return reply(HttpStatus.CONFLICT, e.getMessage());                  // 409
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> database(DatabaseException e) {
        return reply(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());     // 500
    }

    private ResponseEntity<ErrorResponse> reply(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
