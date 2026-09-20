ALTER TABLE work_shifts
    ALTER COLUMN start_time TYPE TIMESTAMPTZ
        USING start_time AT TIME ZONE 'Europe/Athens',
    ALTER COLUMN end_time TYPE TIMESTAMPTZ
        USING end_time AT TIME ZONE 'Europe/Athens';
