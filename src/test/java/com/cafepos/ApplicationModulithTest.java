package com.cafepos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulithTest {

    @Test
    void verifyModuleStructure() {
        ApplicationModules modules = ApplicationModules.of("com.cafepos");
        modules.verify();
    }
}

