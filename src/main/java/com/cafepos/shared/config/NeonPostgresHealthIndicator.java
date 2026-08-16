package com.cafepos.shared.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class NeonPostgresHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public NeonPostgresHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            boolean valid = conn.isValid(3) && rs.next();
            if (valid) {
                return Health.up()
                        .withDetail("database", "Neon PostgreSQL")
                        .withDetail("status", "Reachable & Healthy")
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", "Neon PostgreSQL")
                        .withDetail("status", "Connection Invalid")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("database", "Neon PostgreSQL")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
