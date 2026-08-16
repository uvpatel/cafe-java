package com.cafepos.shared.error;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String extractPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "/";
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Business rule violation [code={}, status={}]: {}", ex.getCode(), ex.getStatus(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.of(ex.getCode(), ex.getMessage(), ex.getStatus(), ex.getMessage(), path);
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String path = extractPath(request);
        List<ProblemDetail.FieldError> fieldErrors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ProblemDetail.FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()));
        }
        String firstMessage = fieldErrors.isEmpty() ? "Validation failed" : fieldErrors.get(0).message();
        log.warn("Validation error on {}: {}", path, firstMessage);
        ProblemDetail problem = ProblemDetail.of("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST.value(), firstMessage, path, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        String path = extractPath(request);
        List<ProblemDetail.FieldError> fieldErrors = new ArrayList<>();
        ex.getConstraintViolations().forEach(cv -> {
            String property = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : "parameter";
            fieldErrors.add(new ProblemDetail.FieldError(property, cv.getMessage(), cv.getInvalidValue()));
        });
        String firstMessage = fieldErrors.isEmpty() ? "Constraint violation" : fieldErrors.get(0).message();
        log.warn("Constraint violation on {}: {}", path, firstMessage);
        ProblemDetail problem = ProblemDetail.of("CONSTRAINT_VIOLATION", "Constraint violation", HttpStatus.BAD_REQUEST.value(), firstMessage, path, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Access denied on {}: {}", path, ex.getMessage());
        ProblemDetail problem = ProblemDetail.of("ACCESS_DENIED", "Access Denied", HttpStatus.FORBIDDEN.value(), "You do not have permission to perform this action", path);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Authentication failed on {}: {}", path, ex.getMessage());
        ProblemDetail problem = ProblemDetail.of("UNAUTHORIZED", "Authentication Required", HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), path);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Entity not found on {}: {}", path, ex.getMessage());
        ProblemDetail problem = ProblemDetail.of("RESOURCE_NOT_FOUND", "Resource Not Found", HttpStatus.NOT_FOUND.value(), ex.getMessage(), path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Invalid argument on {}: {}", path, ex.getMessage());
        ProblemDetail problem = ProblemDetail.of("INVALID_ARGUMENT", "Invalid Argument", HttpStatus.BAD_REQUEST.value(), ex.getMessage(), path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Illegal state on {}: {}", path, ex.getMessage());
        ProblemDetail problem = ProblemDetail.of("ILLEGAL_STATE", "Illegal State", HttpStatus.CONFLICT.value(), ex.getMessage(), path);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex, WebRequest request) {
        String path = extractPath(request);
        log.warn("Optimistic locking conflict on {}: {}", path, ex.getMessage());
        ProblemDetail problem = ProblemDetail.of("CONCURRENCY_CONFLICT", "Resource Conflict", HttpStatus.CONFLICT.value(), "The resource was modified concurrently by another transaction. Please retry.", path);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String path = extractPath(request);
        ProblemDetail problem = ProblemDetail.of("METHOD_NOT_ALLOWED", "Method Not Allowed", HttpStatus.METHOD_NOT_ALLOWED.value(), ex.getMessage(), path);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        String path = extractPath(request);
        ProblemDetail problem = ProblemDetail.of("UNSUPPORTED_MEDIA_TYPE", "Unsupported Media Type", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ex.getMessage(), path);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        String path = extractPath(request);
        ProblemDetail problem = ProblemDetail.of("RESOURCE_NOT_FOUND", "Endpoint Not Found", HttpStatus.NOT_FOUND.value(), ex.getMessage(), path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        String path = extractPath(request);
        int statusCode = ex.getStatusCode().value();
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ProblemDetail problem = ProblemDetail.of("HTTP_ERROR_" + statusCode, "HTTP Error", statusCode, reason, path);
        return ResponseEntity.status(ex.getStatusCode()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, WebRequest request) {
        String path = extractPath(request);
        log.error("Unhandled exception caught while processing request on {}: ", path, ex);
        ProblemDetail problem = ProblemDetail.of("INTERNAL_SERVER_ERROR", "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected internal server error occurred", path);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
