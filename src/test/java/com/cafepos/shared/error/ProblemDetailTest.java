package com.cafepos.shared.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailTest {

    @Test
    @DisplayName("Should create well-formed ProblemDetail instance with RFC 9457 URI")
    void testProblemDetailCreation() {
        ProblemDetail problem = ProblemDetail.of("SESSION_ALREADY_OPEN", "Terminal active session exists", 409, "Detailed message", "/api/v1/pos-sessions");

        assertThat(problem.code()).isEqualTo("SESSION_ALREADY_OPEN");
        assertThat(problem.status()).isEqualTo(409);
        assertThat(problem.type().toString()).isEqualTo("https://cafepos.example/problems/session-already-open");
        assertThat(problem.traceId()).isNotNull();
    }
}
