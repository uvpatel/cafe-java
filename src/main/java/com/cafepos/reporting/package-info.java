@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "ordering", "ordering::domain",
        "payment", "payment::domain",
        "session", "session::domain",
        "organization", "organization::domain"
    }
)
package com.cafepos.reporting;
