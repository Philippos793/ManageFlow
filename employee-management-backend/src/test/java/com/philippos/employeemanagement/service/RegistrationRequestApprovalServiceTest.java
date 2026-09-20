package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.RegistrationApprovalRequest;
import com.philippos.employeemanagement.entity.Department;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.RegistrationRequest;
import com.philippos.employeemanagement.entity.RegistrationStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.event.RegistrationApprovedEvent;
import com.philippos.employeemanagement.exception.DepartmentNotFoundException;
import com.philippos.employeemanagement.exception.RegistrationRequestConflictException;
import com.philippos.employeemanagement.exception.RegistrationRequestNotFoundException;
import com.philippos.employeemanagement.mapper.RegistrationRequestMapper;
import com.philippos.employeemanagement.repository.DepartmentRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.RegistrationRequestRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationRequestApprovalServiceTest {
    private RegistrationRequestRepository repository;
    private DepartmentRepository departmentRepository;
    private EmployeeRepository employeeRepository;
    private UserRepository userRepository;
    private ApplicationEventPublisher eventPublisher;
    private RegistrationRequestService service;

    @BeforeEach
    void setUp() {
        repository = mock(RegistrationRequestRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        userRepository = mock(UserRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new RegistrationRequestService(repository, departmentRepository, employeeRepository,
                userRepository, mock(PasswordEncoder.class), new RegistrationRequestMapper(), eventPublisher);
    }

    @Test
    void approveCreatesEmployeeAndLinkedEmployeeUserWithExistingHashAndDepartment() {
        RegistrationRequest request = request(RegistrationStatus.PENDING);
        Department department = new Department("IT", null);
        department.setId(3L);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0); employee.setId(8L); return employee;
        });
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAndFlush(request)).thenReturn(request);

        service.approve(1L, new RegistrationApprovalRequest(3L));

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(employeeRepository).saveAndFlush(employeeCaptor.capture());
        verify(userRepository).saveAndFlush(userCaptor.capture());
        Employee employee = employeeCaptor.getValue();
        User user = userCaptor.getValue();
        assertEquals("Alex", employee.getFirstName());
        assertSame(department, employee.getDepartment());
        assertEquals("alex", user.getUsername());
        assertEquals("stored-bcrypt-hash", user.getPassword());
        assertEquals(Role.EMPLOYEE, user.getRole());
        assertSame(employee, user.getEmployee());
        assertEquals(RegistrationStatus.APPROVED, request.getStatus());
        assertNull(request.getPassword());
        ArgumentCaptor<RegistrationApprovedEvent> eventCaptor =
                ArgumentCaptor.forClass(RegistrationApprovedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("alex@example.com", eventCaptor.getValue().recipient());
        assertEquals("alex", eventCaptor.getValue().username());
    }

    @Test
    void rejectMarksRequestRejectedWithoutCreatingEmployeeOrUser() {
        RegistrationRequest request = request(RegistrationStatus.PENDING);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(repository.saveAndFlush(request)).thenReturn(request);

        service.reject(1L);

        assertEquals(RegistrationStatus.REJECTED, request.getStatus());
        assertNull(request.getPassword());
        verify(employeeRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void approveAlreadyProcessedRequestReturnsConflict() {
        when(repository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(request(RegistrationStatus.APPROVED)));
        assertThrows(RegistrationRequestConflictException.class,
                () -> service.approve(1L, new RegistrationApprovalRequest(3L)));
    }

    @Test
    void rejectAlreadyProcessedRequestReturnsConflict() {
        when(repository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(request(RegistrationStatus.REJECTED)));
        assertThrows(RegistrationRequestConflictException.class, () -> service.reject(1L));
    }

    @Test
    void missingRegistrationRequestReturnsNotFound() {
        when(repository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(RegistrationRequestNotFoundException.class,
                () -> service.approve(99L, new RegistrationApprovalRequest(3L)));
    }

    @Test
    void missingDepartmentReturnsNotFound() {
        when(repository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(request(RegistrationStatus.PENDING)));
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(DepartmentNotFoundException.class,
                () -> service.approve(1L, new RegistrationApprovalRequest(99L)));
        verify(employeeRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void existingEmployeeEmailReturnsConflictAndKeepsRequestPending() {
        RegistrationRequest request = request(RegistrationStatus.PENDING);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(employeeRepository.existsByEmailIgnoreCase("alex@example.com")).thenReturn(true);

        RegistrationRequestConflictException exception = assertThrows(
                RegistrationRequestConflictException.class,
                () -> service.approve(1L, new RegistrationApprovalRequest(3L)));

        assertEquals("An employee with this email already exists.", exception.getMessage());
        assertEquals(RegistrationStatus.PENDING, request.getStatus());
        assertEquals("stored-bcrypt-hash", request.getPassword());
        verify(employeeRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void employeeEmailRaceConditionReturnsConflictAndKeepsRequestPending() {
        RegistrationRequest request = request(RegistrationStatus.PENDING);
        Department department = new Department("IT", null);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(request));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));
        when(employeeRepository.saveAndFlush(any(Employee.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate employee email"));

        RegistrationRequestConflictException exception = assertThrows(
                RegistrationRequestConflictException.class,
                () -> service.approve(1L, new RegistrationApprovalRequest(3L)));

        assertEquals("An employee with this email already exists.", exception.getMessage());
        assertEquals(RegistrationStatus.PENDING, request.getStatus());
        assertEquals("stored-bcrypt-hash", request.getPassword());
        verify(userRepository, never()).saveAndFlush(any());
        verify(repository, never()).saveAndFlush(request);
        verify(eventPublisher, never()).publishEvent(any());
    }

    private RegistrationRequest request(RegistrationStatus status) {
        RegistrationRequest request = new RegistrationRequest();
        request.setId(1L); request.setFirstName("Alex"); request.setLastName("Smith");
        request.setEmail("alex@example.com"); request.setUsername("alex");
        request.setPassword("stored-bcrypt-hash"); request.setStatus(status);
        return request;
    }
}
