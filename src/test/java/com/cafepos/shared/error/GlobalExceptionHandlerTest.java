package com.cafepos.shared.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ServletWebRequest createWebRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        return new ServletWebRequest(request);
    }

    @Test
    @DisplayName("handleBusinessException should return custom status and code")
    void shouldHandleBusinessException() {
        BusinessException ex = new BusinessException("ORDER_ALREADY_PAID", "Order has already been paid", 400);
        ResponseEntity<ProblemDetail> response = handler.handleBusinessException(ex, createWebRequest("/api/v1/orders/123/pay"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ORDER_ALREADY_PAID");
        assertThat(response.getBody().title()).isEqualTo("Order has already been paid");
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    @DisplayName("handleAccessDeniedException should return 403 Forbidden with ACCESS_DENIED code")
    void shouldHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden action");
        ResponseEntity<ProblemDetail> response = handler.handleAccessDeniedException(ex, createWebRequest("/api/v1/admin/config"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().status()).isEqualTo(403);
    }

    @Test
    @DisplayName("handleAuthenticationException should return 401 Unauthorized with UNAUTHORIZED code")
    void shouldHandleAuthenticationException() {
        BadCredentialsException ex = new BadCredentialsException("Invalid username or password");
        ResponseEntity<ProblemDetail> response = handler.handleAuthenticationException(ex, createWebRequest("/api/v1/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().status()).isEqualTo(401);
    }

    @Test
    @DisplayName("handleOptimisticLockingFailureException should return 409 Conflict with CONCURRENCY_CONFLICT code")
    void shouldHandleOptimisticLockingConflict() {
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update detected");
        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLockingFailureException(ex, createWebRequest("/api/v1/orders/123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONCURRENCY_CONFLICT");
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    @DisplayName("handleNoSuchElementException should return 404 Not Found")
    void shouldHandleNoSuchElementException() {
        NoSuchElementException ex = new NoSuchElementException("Product not found");
        ResponseEntity<ProblemDetail> response = handler.handleNoSuchElementException(ex, createWebRequest("/api/v1/products/456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    @DisplayName("handleValidationException should extract field errors properly")
    void shouldHandleValidationException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "orderRequest");
        bindingResult.addError(new FieldError("orderRequest", "guestCount", -1, false, null, null, "Guest count must be positive"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex, createWebRequest("/api/v1/orders"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().fieldErrors()).hasSize(1);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("guestCount");
    }

    @Test
    @DisplayName("handleGenericException should return 500 Internal Server Error without leaking internal stack traces")
    void shouldHandleGenericException() {
        RuntimeException ex = new RuntimeException("Null pointer in private calculation");
        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex, createWebRequest("/api/v1/checkout"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().detail()).isEqualTo("An unexpected internal server error occurred");
    }
}
