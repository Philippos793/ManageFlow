package com.philippos.employeemanagement.migration;

import com.philippos.employeemanagement.bootstrap.AdminBootstrapService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applies every Flyway migration to a real PostgreSQL instance and lets
 * Hibernate validate the entity mappings against the resulting schema.
 * Skipped automatically when Docker is not available.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean
    private AdminBootstrapService adminBootstrapService;

    @Autowired
    private Flyway flyway;

    @Test
    void allMigrationsApplyAndSchemaMatchesEntities() {
        MigrationInfo[] applied = flyway.info().applied();
        MigrationInfo current = flyway.info().current();

        assertTrue(applied.length > 0, "Flyway should apply the versioned migrations");
        assertTrue(Arrays.stream(applied).allMatch(info -> info.getState().isApplied()),
                "Every migration should be applied successfully");
        assertNotNull(current);
        assertEquals(0, flyway.info().pending().length, "No migration should remain pending");
    }
}
