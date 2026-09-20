package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.RegistrationSubmissionRequest;
import com.philippos.employeemanagement.dto.response.RegistrationRequestResponse;
import com.philippos.employeemanagement.entity.RegistrationRequest;
import com.philippos.employeemanagement.entity.RegistrationStatus;
import com.philippos.employeemanagement.exception.RegistrationRequestConflictException;
import com.philippos.employeemanagement.mapper.RegistrationRequestMapper;
import com.philippos.employeemanagement.repository.RegistrationRequestRepository;
import com.philippos.employeemanagement.repository.DepartmentRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationRequestServiceTest {
    private RegistrationRequestRepository repository;
    private BCryptPasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private RegistrationRequestService service;

    @BeforeEach
    void setUp() {
        repository = mock(RegistrationRequestRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userRepository = mock(UserRepository.class);
        service = new RegistrationRequestService(repository, mock(DepartmentRepository.class),
                mock(EmployeeRepository.class), userRepository, passwordEncoder,
                new RegistrationRequestMapper(), mock(ApplicationEventPublisher.class));
    }

    @Test
    void submitStoresPendingRequestWithBcryptPassword() {
        when(repository.saveAndFlush(any(RegistrationRequest.class))).thenAnswer(invocation -> {
            RegistrationRequest request = invocation.getArgument(0);
            request.setId(1L);
            return request;
        });

        RegistrationRequestResponse response = service.submit(submission());

        ArgumentCaptor<RegistrationRequest> captor = ArgumentCaptor.forClass(RegistrationRequest.class);
        verify(repository).saveAndFlush(captor.capture());
        RegistrationRequest saved = captor.getValue();
        assertEquals("Alex", saved.getFirstName());
        assertEquals("alex@example.com", saved.getEmail());
        assertEquals(RegistrationStatus.PENDING, saved.getStatus());
        assertTrue(passwordEncoder.matches("temporary-password", saved.getPassword()));
        assertFalse(saved.getPassword().equals("temporary-password"));
        assertEquals(1L, response.getId());
        assertEquals(RegistrationStatus.PENDING, response.getStatus());
    }

    @Test
    void duplicatePendingUsernameReturnsConflict() {
        when(repository.existsByUsernameIgnoreCaseAndStatus("alex", RegistrationStatus.PENDING))
                .thenReturn(true);
        assertThrows(RegistrationRequestConflictException.class, () -> service.submit(submission()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void existingUsernameIgnoringCaseReturnsConflict() {
        when(userRepository.existsByUsernameIgnoreCase("alex")).thenReturn(true);

        RegistrationRequestConflictException exception = assertThrows(
                RegistrationRequestConflictException.class,
                () -> service.submit(new RegistrationSubmissionRequest(
                        "Alex", "Smith", "alex@example.com",
                        " ALEX ", "temporary-password")));

        assertEquals("Username already exists", exception.getMessage());
        verify(repository, never()).saveAndFlush(any());
    }
    @Test
    void duplicatePendingEmailReturnsConflict() {
        when(repository.existsByEmailIgnoreCaseAndStatus("alex@example.com", RegistrationStatus.PENDING))
                .thenReturn(true);
        assertThrows(RegistrationRequestConflictException.class, () -> service.submit(submission()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentDuplicateReturnsConflict() {
        when(repository.saveAndFlush(any(RegistrationRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate pending request"));
        assertThrows(RegistrationRequestConflictException.class, () -> service.submit(submission()));
    }

    @Test
    void getPendingReturnsSafeMappedResponsesInRepositoryOrder() {
        RegistrationRequest first = pending(1L, "first", LocalDateTime.of(2026, 1, 1, 10, 0));
        RegistrationRequest second = pending(2L, "second", LocalDateTime.of(2026, 1, 2, 10, 0));
        when(repository.findByStatusOrderByCreatedAtAsc(RegistrationStatus.PENDING))
                .thenReturn(List.of(first, second));

        List<RegistrationRequestResponse> responses = service.getPending();

        assertEquals(List.of("first", "second"), responses.stream()
                .map(RegistrationRequestResponse::getUsername).toList());
    }

    private RegistrationSubmissionRequest submission() {
        return new RegistrationSubmissionRequest(" Alex ", " Smith ", " Alex@Example.com ",
                "alex", "temporary-password");
    }

    private RegistrationRequest pending(Long id, String username, LocalDateTime createdAt) {
        RegistrationRequest request = new RegistrationRequest();
        request.setId(id); request.setFirstName("Alex"); request.setLastName("Smith");
        request.setEmail(username + "@example.com"); request.setUsername(username);
        request.setPassword("bcrypt-hash"); request.setStatus(RegistrationStatus.PENDING);
        request.setCreatedAt(createdAt);
        return request;
    }
}
