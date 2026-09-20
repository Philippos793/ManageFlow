package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.EmployeeAccountRequest;
import com.philippos.employeemanagement.dto.response.EmployeeAccountResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.EmployeeAccountConflictException;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.util.AccountValidation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeAccountService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeAccountService(EmployeeRepository employeeRepository, UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EmployeeAccountResponse createEmployeeAccount(EmployeeAccountRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        String username = AccountValidation.normalizeUsername(request.getUsername());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new EmployeeAccountConflictException("Username already exists");
        }
        if (userRepository.existsByEmployee_Id(employee.getId())) {
            throw new EmployeeAccountConflictException("Employee already has a user account");
        }
        User user = new User(username, passwordEncoder.encode(request.getTemporaryPassword()), Role.EMPLOYEE);
        user.setEmployee(employee);
        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new EmployeeAccountResponse(savedUser.getId(), employee.getId(),
                    savedUser.getUsername(), savedUser.getRole());
        } catch (DataIntegrityViolationException exception) {
            throw new EmployeeAccountConflictException("Username or employee account already exists");
        }
    }
}
