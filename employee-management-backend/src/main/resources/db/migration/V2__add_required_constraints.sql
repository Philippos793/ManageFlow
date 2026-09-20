ALTER TABLE users
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN password SET NOT NULL,
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_username UNIQUE (username);

ALTER TABLE employees
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL,
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN department SET NOT NULL;
