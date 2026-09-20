package com.philippos.employeemanagement.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskAttachmentMigrationTest {

    @Test
    void migrationCreatesSecureTaskAttachmentMetadataSchema() throws IOException {
        String migration = new String(getClass().getResourceAsStream(
                "/db/migration/V17__create_task_attachments.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertTrue(migration.contains("CREATE TABLE task_attachments"));
        assertTrue(migration.contains("REFERENCES tasks(id) ON DELETE CASCADE"));
        assertTrue(migration.contains("REFERENCES users(id) ON DELETE RESTRICT"));
        assertTrue(migration.contains("uploaded_at TIMESTAMPTZ NOT NULL"));
        assertTrue(migration.contains("ADMIN_RESOURCE"));
        assertTrue(migration.contains("EMPLOYEE_SUBMISSION"));
        assertTrue(migration.contains("stored_filename VARCHAR(255) NOT NULL UNIQUE"));
        assertTrue(migration.contains("idx_task_attachments_task_id"));
    }
}
