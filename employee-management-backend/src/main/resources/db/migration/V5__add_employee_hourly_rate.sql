ALTER TABLE employees
    ADD COLUMN hourly_rate NUMERIC(10, 2);

ALTER TABLE employees
    ADD CONSTRAINT chk_employees_hourly_rate_non_negative
        CHECK (hourly_rate IS NULL OR hourly_rate >= 0);