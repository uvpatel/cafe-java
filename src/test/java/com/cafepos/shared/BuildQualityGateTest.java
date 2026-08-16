package com.cafepos.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BuildQualityGateTest {

    @Test
    @DisplayName("Verify Java 21 environment baseline and build metadata")
    void verifyJavaVersionAndEnvironment() {
        String javaVersion = System.getProperty("java.version");
        assertThat(javaVersion).isNotNull();

        // Ensure Java version starts with 21 or higher
        int majorVersion = Integer.parseInt(javaVersion.split("\\.")[0]);
        assertThat(majorVersion).isGreaterThanOrEqualTo(21);
    }
}
