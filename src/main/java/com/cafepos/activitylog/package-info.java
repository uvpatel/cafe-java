@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "kitchen", "kitchen::domain"
    }
)
package com.cafepos.activitylog;
