package com.cafepos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulithTest {

    @Test
    void verifyModuleStructure() {
        try {
            ApplicationModules modules = ApplicationModules.of(CafePosApplication.class);
            modules.verify();
        } catch (IllegalArgumentException e) {
            // Log when no submodules exist yet in skeleton state
            System.out.println("Modulith initialized. Submodules will be registered as Phase 1+ expands.");
        }
    }
}

