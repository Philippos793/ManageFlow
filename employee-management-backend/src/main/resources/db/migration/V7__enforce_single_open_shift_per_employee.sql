CREATE UNIQUE INDEX uk_work_shifts_one_open_per_employee
    ON work_shifts (employee_id)
    WHERE end_time IS NULL;