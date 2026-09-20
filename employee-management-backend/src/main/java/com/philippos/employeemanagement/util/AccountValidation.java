package com.philippos.employeemanagement.util;

import java.util.Locale;

public final class AccountValidation {
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 72;
    public static final String USERNAME_PATTERN = "^[A-Za-z0-9._-]+$";

    private AccountValidation() {
    }

    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }
}