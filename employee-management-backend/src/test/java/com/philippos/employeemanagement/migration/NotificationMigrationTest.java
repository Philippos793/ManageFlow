package com.philippos.employeemanagement.migration;

import com.philippos.employeemanagement.entity.NotificationType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationMigrationTest {

    @Test
    void v15DefinesNotificationTableAndOwnershipConstraints() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V15__create_notifications.sql")) {
            assertTrue(input != null, "V15 notification migration must exist");
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("CREATE TABLE notifications"));
        assertTrue(migration.contains("recipient_user_id"));
        assertTrue(migration.contains("REFERENCES users(id)"));
        assertTrue(migration.contains("REFERENCES tasks(id)"));
        assertTrue(migration.contains("TIMESTAMPTZ"));
        assertTrue(migration.contains("TASK_ASSIGNED"));
        assertTrue(migration.contains("TASK_ACCEPTED"));
        assertTrue(migration.contains("TASK_DECLINED"));
        assertTrue(migration.contains("idx_notifications_recipient_unread"));
    }

    @Test
    void v18AddsTheTaskUpdatedNotificationType() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V18__add_task_updated_notification_type.sql")) {
            assertTrue(input != null, "V18 notification type migration must exist");
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("DROP CONSTRAINT chk_notifications_type"));
        assertTrue(migration.contains("TASK_UPDATED"));
    }

    @Test
    void v21AddsTheTaskCompletedNotificationType() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V21__add_task_completed_notification_type.sql")) {
            assertTrue(input != null, "V21 notification type migration must exist");
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("DROP CONSTRAINT chk_notifications_type"));
        assertTrue(migration.contains("TASK_COMPLETED"));
    }

    @Test
    void v22AddsTheTaskChangesRequestedTypes() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V22__add_task_changes_requested_types.sql")) {
            assertTrue(input != null, "V22 notification and activity migration must exist");
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("notifications"));
        assertTrue(migration.contains("task_activities"));
        assertTrue(migration.contains("TASK_CHANGES_REQUESTED"));
    }

    @Test
    void v23ExpandsNotificationTypeColumnForAllNotificationTypes() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V23__expand_notification_type_column.sql")) {
            assertTrue(input != null, "V23 notification type migration must exist");
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(migration.contains("ALTER COLUMN type TYPE VARCHAR(50)"));
        for (NotificationType type : NotificationType.values()) {
            assertTrue(type.name().length() <= 50,
                    () -> type + " must fit in notifications.type");
        }
    }
}
