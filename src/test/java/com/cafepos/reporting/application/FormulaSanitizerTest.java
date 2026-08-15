package com.cafepos.reporting.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormulaSanitizerTest {

    @Test
    @DisplayName("Should sanitize formula prefix characters in CSV export fields")
    void sanitizeFormulaPrefixes() {
        assertThat(FormulaSanitizer.sanitize("=1+2")).isEqualTo("'=1+2");
        assertThat(FormulaSanitizer.sanitize("+cmd")).isEqualTo("'+cmd");
        assertThat(FormulaSanitizer.sanitize("-100")).isEqualTo("'-100");
        assertThat(FormulaSanitizer.sanitize("@SUM")).isEqualTo("'@SUM");
        assertThat(FormulaSanitizer.sanitize("Normal Text")).isEqualTo("Normal Text");
        assertThat(FormulaSanitizer.sanitize(null)).isEqualTo("");
    }
}
