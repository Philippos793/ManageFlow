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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeAccountServiceTest {
    private EmployeeRepository employeeRepository;
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private EmployeeAccountService service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new EmployeeAccountService(employeeRepository, userRepository, passwordEncoder);
    }

    @Test
    void createsLinkedEmployeeAccountWithBcryptPasswordAndEmployeeRole() {
        Employee employee = employee(7L);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(20L);
            return user;
        });

        EmployeeAccountResponse response = service.createEmployeeAccount(
                new EmployeeAccountRequest(7L, " Employee.User ", "temporary-secret"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User savedUser = captor.getValue();
        assertEquals("employee.user", savedUser.getUsername());
        assertTrue(passwordEncoder.matches("temporary-secret", savedUser.getPassword()));
        assertEquals(Role.EMPLOYEE, savedUser.getRole());
        assertSame(employee, savedUser.getEmployee());
        assertEquals(20L, response.getUserId());
        assertEquals(7L, response.getEmployeeId());
        assertEquals(Role.EMPLOYEE, response.getRole());
    }

    @Test
    void duplicateUsernameReturnsConflict() {
        Employee employee = employee(7L);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByUsernameIgnoreCase("employee.user")).thenReturn(true);

        assertThrows(EmployeeAccountConflictException.class, () -> service.createEmployeeAccount(
                new EmployeeAccountRequest(7L, "employee.user", "password")));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void employeeWithExistingAccountReturnsConflict() {
        Employee employee = employee(7L);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmployee_Id(7L)).thenReturn(true);

        assertThrows(EmployeeAccountConflictException.class, () -> service.createEmployeeAccount(
                new EmployeeAccountRequest(7L, "employee.user", "password")));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void missingEmployeeReturnsNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> service.createEmployeeAccount(
                new EmployeeAccountRequest(99L, "employee.user", "password")));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void concurrentUniqueConstraintViolationReturnsConflict() {
        Employee employee = employee(7L);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThrows(EmployeeAccountConflictException.class, () -> service.createEmployeeAccount(
                new EmployeeAccountRequest(7L, "employee.user", "password")));
    }

    private Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        return employee;
    }
}
