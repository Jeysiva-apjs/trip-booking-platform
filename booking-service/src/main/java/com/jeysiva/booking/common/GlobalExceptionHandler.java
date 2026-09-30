package com.jeysiva.booking.common;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// Maps every exception to an HTTP status with a uniform body, so controllers contain no try/catch.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ResourceUnavailableException.class)
    public ResponseEntity<ApiError> handleUnavailable(ResourceUnavailableException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Lost the @Version race: another booking updated the seat/room first.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "That seat or room was just taken by another booking. Please retry.");
    }

    // DB picked this transaction as a deadlock victim under contention.
    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ApiError> handleLockContention(CannotAcquireLockException ex) {
        return build(HttpStatus.CONFLICT, "Booking contention on this seat or room. Please retry.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, details);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }
}
