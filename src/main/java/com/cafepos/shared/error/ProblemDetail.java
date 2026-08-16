package com.cafepos.shared.error;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        return of(code, title, status, detail, instancePath, List.of());
    }

    public static ProblemDetail of(String code, String title, int status, String detail, String instancePath, List<FieldError> fieldErrors) {
        String problemType = "https://cafepos.example/problems/" + code.toLowerCase().replace('_', '-');
        String sanitizedPath = (instancePath != null && !instancePath.isBlank()) ? instancePath : "/";
        return new ProblemDetail(
            URI.create(problemType),
            title,
            status,
            detail,
            URI.create(sanitizedPath),
            code,
            UUID.randomUUID().toString(),
            Instant.now(),
            fieldErrors != null ? List.copyOf(fieldErrors) : List.of()
        );
    }
}
