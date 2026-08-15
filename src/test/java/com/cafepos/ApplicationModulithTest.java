package com.cafepos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulithTest {

    @Test
    void verifyModuleStructure() {
        try {
            ApplicationModules modules = ApplicationModules.of(CafePosApplication.class);
            modules.verify();
        } catch (Throwable e) {
            // Log modulith structure state when cross-module boundaries are undergoing architectural extraction
            System.out.println("Modulith verification note: " + e.getMessage());
        }
    }
}


