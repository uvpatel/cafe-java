package com.cafepos.shared.seed;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/seed")
public class ApiSeedController {

    private final DatabaseSeeder databaseSeeder;

    public ApiSeedController(DatabaseSeeder databaseSeeder) {
        this.databaseSeeder = Objects.requireNonNull(databaseSeeder, "databaseSeeder must not be null");
    }

    public record ApiSeedResponse(
            boolean success,
            String message,
            String timestamp
    ) {}

    @PostMapping("/execute")
    public ResponseEntity<ApiSeedResponse> executeApiSeeding(
            @RequestParam(required = false, defaultValue = "false") boolean force
    ) {
        databaseSeeder.seedAll();
        return ResponseEntity.ok(new ApiSeedResponse(
                true,
                "API Data Seeding executed successfully! Realistic demo data populated in Neon PostgreSQL database.",
                Instant.now().toString()
        ));
    }
}
