package com.philippos.employeemanagement.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskChecklistMigrationTest {

    @Test
    void migrationCreatesChecklistTableWithTaskRelationshipAndIntegrityConstraints()
            throws IOException {
        String migration = new String(getClass().getResourceAsStream(
                "/db/migration/V16__create_task_checklist_items.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertTrue(migration.contains("CREATE TABLE task_checklist_items"));
        assertTrue(migration.contains("REFERENCES tasks(id) ON DELETE CASCADE"));
        assertTrue(migration.contains("created_at TIMESTAMPTZ NOT NULL"));
        assertTrue(migration.contains("completed_at TIMESTAMPTZ"));
        assertTrue(migration.contains("BTRIM(description) <> ''"));
        assertTrue(migration.contains("idx_task_checklist_items_task_id"));
    }
}
