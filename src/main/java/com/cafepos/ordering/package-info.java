@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "catalog", "catalog::domain",
        "floor", "floor::domain",
        "session", "session::domain",
        "promotion", "promotion::api", "promotion::application"
    }
)
package com.cafepos.ordering;
