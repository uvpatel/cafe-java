@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "floor", "floor::domain",
        "ordering", "ordering::api", "ordering::domain", "ordering::application",
        "session", "session::domain"
    }
)
package com.cafepos.booking;
