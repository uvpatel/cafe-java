package com.cafepos.shared.error;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public record ProblemDetail(
    URI type,
    String title,
    int status,
    String detail,
    URI instance,
    String code,
    String traceId,
    Instant timestamp,
    List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message, Object rejectedValue) {}

    public static ProblemDetail of(String code, String title, int status, String detail, String instancePath) {
        return new ProblemDetail(
            URI.create("https://cafepos.example/problems/" + code.toLowerCase().replace('_', '-')),
            title,
            status,
            detail,
            URI.create(instancePath),
            code,
            java.util.UUID.randomUUID().toString(),
            Instant.now(),
            List.of()
        );
    }
}
