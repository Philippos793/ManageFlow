ALTER TABLE employees
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE employees
    ADD CONSTRAINT chk_employees_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'));