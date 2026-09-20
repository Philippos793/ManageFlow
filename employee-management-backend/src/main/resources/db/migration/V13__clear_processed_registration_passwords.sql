ALTER TABLE registration_requests
    ALTER COLUMN password DROP NOT NULL;

UPDATE registration_requests
SET password = NULL
WHERE status IN ('APPROVED', 'REJECTED');

ALTER TABLE registration_requests
    ADD CONSTRAINT chk_registration_requests_password_retention
        CHECK (
            (status = 'PENDING' AND password IS NOT NULL)
            OR
            (status IN ('APPROVED', 'REJECTED') AND password IS NULL)
        );
