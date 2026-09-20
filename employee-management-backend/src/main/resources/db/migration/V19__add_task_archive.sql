ALTER TABLE tasks
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_tasks_active_assigned_employee
    ON tasks (assigned_employee_id)
    WHERE archived = FALSE;

CREATE INDEX idx_tasks_active_created_by_user
    ON tasks (created_by_user_id)
    WHERE archived = FALSE;
