package com.philippos.employeemanagement.bootstrap;

import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.util.AccountValidation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createInitialAdminIfRequired(String configuredUsername, String configuredPassword) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        String username = AccountValidation.normalizeUsername(configuredUsername);
        validateCredentials(username, configuredPassword);

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException(
                    "Initial administrator username is already in use");
        }

        User admin = new User(
                username,
                passwordEncoder.encode(configuredPassword),
                Role.ADMIN);
        userRepository.saveAndFlush(admin);
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "No administrator exists. ADMIN_USERNAME and ADMIN_PASSWORD must be configured");
        }
        if (username.length() < AccountValidation.USERNAME_MIN_LENGTH
                || username.length() > AccountValidation.USERNAME_MAX_LENGTH
                || !username.matches(AccountValidation.USERNAME_PATTERN)) {
            throw new IllegalStateException(
                    "ADMIN_USERNAME must satisfy the application username requirements");
        }
        if (password.length() < AccountValidation.PASSWORD_MIN_LENGTH
                || password.length() > AccountValidation.PASSWORD_MAX_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must satisfy the application password requirements");
        }
    }
}
