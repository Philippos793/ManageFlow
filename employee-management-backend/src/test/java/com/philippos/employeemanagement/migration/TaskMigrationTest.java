package com.philippos.employeemanagement.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskMigrationTest {

    @Test
    void v14DefinesTaskTableConstraintsAndIndexes() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V14__create_tasks.sql")) {
            assertTrue(input != null, "V14 task migration must exist");
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("CREATE TABLE tasks"));
        assertTrue(migration.contains("TIMESTAMPTZ"));
        assertTrue(migration.contains("REFERENCES employees(id)"));
        assertTrue(migration.contains("REFERENCES users(id)"));
        assertTrue(migration.contains("CHECK (time_allowed_minutes > 0)"));
        assertTrue(migration.contains("CHECK (progress BETWEEN 0 AND 100)"));
        assertTrue(migration.contains("idx_tasks_assigned_employee_status"));
        assertTrue(migration.contains("idx_tasks_created_by_user_status"));
    }

    @Test
    void v19AddsTaskArchiveWithoutRemovingHistoricalData() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V19__add_task_archive.sql")) {
            assertTrue(input != null, "V19 task archive migration must exist");
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE"));
        assertTrue(migration.contains("idx_tasks_active_assigned_employee"));
        assertTrue(migration.contains("idx_tasks_active_created_by_user"));
    }

    @Test
    void v20DefinesTaskActivitiesWithChronologicalIndex() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V20__create_task_activities.sql")) {
            assertTrue(input != null, "V20 task activity migration must exist");
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("CREATE TABLE task_activities"));
        assertTrue(migration.contains("REFERENCES tasks(id)"));
        assertTrue(migration.contains("REFERENCES users(id)"));
        assertTrue(migration.contains("TIMESTAMPTZ"));
        assertTrue(migration.contains("TASK_ARCHIVED"));
        assertTrue(migration.contains("idx_task_activities_task_created_at"));
    }
}
