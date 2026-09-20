ALTER TABLE notifications
    DROP CONSTRAINT chk_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type
    CHECK (type IN (
        'TASK_ASSIGNED',
        'TASK_ACCEPTED',
        'TASK_DECLINED',
        'TASK_UPDATED',
        'TASK_COMPLETED'
    ));
