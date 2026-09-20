ALTER TABLE notifications
    DROP CONSTRAINT chk_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type
    CHECK (type IN (
        'TASK_ASSIGNED',
        'TASK_ACCEPTED',
        'TASK_DECLINED',
        'TASK_UPDATED',
        'TASK_COMPLETED',
        'TASK_CHANGES_REQUESTED'
    ));

ALTER TABLE task_activities
    DROP CONSTRAINT chk_task_activities_type;

ALTER TABLE task_activities
    ADD CONSTRAINT chk_task_activities_type
    CHECK (activity_type IN (
        'TASK_CREATED',
        'TASK_ACCEPTED',
        'TASK_DECLINED',
        'TASK_UPDATED',
        'CHECKLIST_UPDATED',
        'PROGRESS_UPDATED',
        'ATTACHMENT_UPLOADED',
        'TASK_COMPLETED',
        'TASK_ARCHIVED',
        'TASK_CHANGES_REQUESTED'
    ));
