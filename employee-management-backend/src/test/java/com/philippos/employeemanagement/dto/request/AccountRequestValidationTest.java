package com.philippos.employeemanagement.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normalizesUsernamesAcrossAllAccountRequests() {
        assertEquals("john.doe", new RegistrationSubmissionRequest(
                "John", "Doe", "john@example.com", " John.Doe ", "password123")
                .getUsername());
        assertEquals("john_doe", new EmployeeAccountRequest(
                1L, " John_Doe ", "password123").getUsername());
        assertEquals("john-doe", new LoginRequest(
                " John-Doe ", "password123").getUsername());
    }

    @Test
    void rejectsUsernameOutsideLengthLimitsOrWithInvalidCharacters() {
        RegistrationSubmissionRequest tooShort = new RegistrationSubmissionRequest(
                "John", "Doe", "john@example.com", "ab", "password123");
        EmployeeAccountRequest invalidCharacters = new EmployeeAccountRequest(
                1L, "john doe", "password123");

        assertTrue(messagesFor(tooShort, "username")
                .contains("Username must be between 3 and 50 characters"));
        assertTrue(messagesFor(invalidCharacters, "username")
                .contains("Username may contain only letters, numbers, periods, underscores, and hyphens"));
    }

    @Test
    void enforcesPasswordLengthForRegistrationAndAccountCreation() {
        RegistrationSubmissionRequest registration = new RegistrationSubmissionRequest(
                "John", "Doe", "john@example.com", "john", "short");
        EmployeeAccountRequest account = new EmployeeAccountRequest(
                1L, "john", "x".repeat(73));

        assertTrue(messagesFor(registration, "password")
                .contains("Password must be between 8 and 72 characters"));
        assertTrue(messagesFor(account, "temporaryPassword")
                .contains("Temporary password must be between 8 and 72 characters"));
    }

    @Test
    void loginLimitsPasswordTo72CharactersWithoutTrimmingIt() {
        String passwordWithSpaces = " password123 ";
        LoginRequest valid = new LoginRequest("john", passwordWithSpaces);
        LoginRequest tooLong = new LoginRequest("john", "x".repeat(73));

        assertEquals(passwordWithSpaces, valid.getPassword());
        assertFalse(validator.validate(valid).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")));
        assertTrue(messagesFor(tooLong, "password")
                .contains("Password must not exceed 72 characters"));
    }

    private Set<String> messagesFor(Object request, String field) {
        return validator.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals(field))
                .map(violation -> violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());
    }
}