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

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User login(String username, String password) {
        String normalizedUsername = AccountValidation.normalizeUsername(username);
        User user = userRepository
                .findByUsername(normalizedUsername)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid username or password")
                );

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






