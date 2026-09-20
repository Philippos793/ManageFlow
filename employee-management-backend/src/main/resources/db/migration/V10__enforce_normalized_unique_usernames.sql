DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        GROUP BY LOWER(TRIM(username))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize usernames: duplicate usernames exist after case-insensitive trimming';
    END IF;
END
$$;

ALTER TABLE users
    DROP CONSTRAINT uk_users_username;

UPDATE users
SET username = LOWER(TRIM(username));

CREATE UNIQUE INDEX uk_users_username_lower
    ON users (LOWER(username));