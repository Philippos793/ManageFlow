package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.util.AccountValidation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Compared against when the username does not exist, so unknown and
    // known usernames take the same time and cannot be told apart.
    private final String dummyPasswordHash;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("unused-dummy-password");
    }

    @Transactional(readOnly = true)
    public User login(String username, String password) {
        String normalizedUsername = AccountValidation.normalizeUsername(username);
        Optional<User> existingUser = userRepository.findByUsername(normalizedUsername);

        if (existingUser.isEmpty()) {
            passwordEncoder.matches(password, dummyPasswordHash);
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = existingUser.get();
        boolean passwordMatches =
                passwordEncoder.matches(password, user.getPassword());

        if (!passwordMatches) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (user.getRole() == Role.EMPLOYEE) {
            if (user.getEmployee() == null) {
                throw new BadCredentialsException("Employee account is unavailable");
            }
            if (user.getEmployee().getStatus() == EmployeeStatus.INACTIVE) {
                throw new BadCredentialsException("Employee account is inactive");
            }
        }

        return user;
    }
}
