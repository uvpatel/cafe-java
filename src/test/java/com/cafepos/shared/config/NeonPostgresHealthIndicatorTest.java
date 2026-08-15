package com.cafepos.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NeonPostgresHealthIndicatorTest {

    @Test
    @DisplayName("Health check should return UP when database connection is valid")
    void healthCheckShouldReturnUpWhenConnectionIsValid() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.isValid(anyInt())).thenReturn(true);

        NeonPostgresHealthIndicator indicator = new NeonPostgresHealthIndicator(dataSource);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("database", "Neon PostgreSQL");
    }

    @Test
    @DisplayName("Health check should return DOWN when database throws exception")
    void healthCheckShouldReturnDownOnException() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        NeonPostgresHealthIndicator indicator = new NeonPostgresHealthIndicator(dataSource);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Connection failed");
    }
}
