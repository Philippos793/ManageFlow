CREATE UNIQUE INDEX uk_employees_email_lower
    ON employees (LOWER(email));