package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.RegistrationApprovalRequest;
import com.philippos.employeemanagement.dto.request.RegistrationSubmissionRequest;
import com.philippos.employeemanagement.dto.response.RegistrationRequestResponse;
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
import com.philippos.employeemanagement.util.AccountValidation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class RegistrationRequestService {
    private final RegistrationRequestRepository repository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationRequestMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    public RegistrationRequestService(RegistrationRequestRepository repository,
                                      DepartmentRepository departmentRepository,
                                      EmployeeRepository employeeRepository,
                                      UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      RegistrationRequestMapper mapper,
                                      ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    public RegistrationRequestResponse submit(RegistrationSubmissionRequest submission) {
        String username = AccountValidation.normalizeUsername(submission.getUsername());
        String email = submission.getEmail().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByUsernameIgnoreCaseAndStatus(username, RegistrationStatus.PENDING)) {
            throw new RegistrationRequestConflictException("A pending request already exists for this username");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new RegistrationRequestConflictException("Username already exists");
        }
        if (repository.existsByEmailIgnoreCaseAndStatus(email, RegistrationStatus.PENDING)) {
            throw new RegistrationRequestConflictException("A pending request already exists for this email");
        }
        RegistrationRequest request = new RegistrationRequest();
        request.setFirstName(submission.getFirstName().trim());
        request.setLastName(submission.getLastName().trim());
        request.setEmail(email);
        request.setUsername(username);
        request.setPassword(passwordEncoder.encode(submission.getPassword()));
        request.setStatus(RegistrationStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        try {
            return mapper.toResponse(repository.saveAndFlush(request));
        } catch (DataIntegrityViolationException exception) {
            throw new RegistrationRequestConflictException(
                    "A pending request already exists for this username or email");
        }
    }

    @Transactional(readOnly = true)
    public List<RegistrationRequestResponse> getPending() {
        return repository.findByStatusOrderByCreatedAtAsc(RegistrationStatus.PENDING)
                .stream().map(mapper::toResponse).toList();
    }

    public RegistrationRequestResponse approve(Long id, RegistrationApprovalRequest approval) {
        RegistrationRequest request = findPendingForUpdate(id);
        if (employeeRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw duplicateEmployeeEmail();
        }
        Department department = departmentRepository.findById(approval.getDepartmentId())
                .orElseThrow(() -> new DepartmentNotFoundException(
                        "Department with id " + approval.getDepartmentId() + " not found"));
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new RegistrationRequestConflictException("Username already exists");
        }

        Employee employee = new Employee(
                request.getFirstName(), request.getLastName(), request.getEmail(), department);
        try {
            employeeRepository.saveAndFlush(employee);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmployeeEmail();
        }

        String passwordHash = request.getPassword();
        User user = new User(request.getUsername(), passwordHash, Role.EMPLOYEE);
        user.setEmployee(employee);
        try {
            userRepository.saveAndFlush(user);
            request.setStatus(RegistrationStatus.APPROVED);
            request.setPassword(null);
            RegistrationRequest approvedRequest = repository.saveAndFlush(request);
            eventPublisher.publishEvent(new RegistrationApprovedEvent(
                    request.getEmail(), request.getUsername()));
            return mapper.toResponse(approvedRequest);
        } catch (DataIntegrityViolationException exception) {
            throw new RegistrationRequestConflictException(
                    "The employee account could not be created because its username is already in use");
        }
    }

    public RegistrationRequestResponse reject(Long id) {
        RegistrationRequest request = findPendingForUpdate(id);
        request.setStatus(RegistrationStatus.REJECTED);
        request.setPassword(null);
        return mapper.toResponse(repository.saveAndFlush(request));
    }

    private RegistrationRequest findPendingForUpdate(Long id) {
        RegistrationRequest request = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RegistrationRequestNotFoundException(
                        "Registration request with id " + id + " not found"));
        if (request.getStatus() != RegistrationStatus.PENDING) {
            throw new RegistrationRequestConflictException(
                    "Registration request has already been processed");
        }
        return request;
    }

    private RegistrationRequestConflictException duplicateEmployeeEmail() {
        return new RegistrationRequestConflictException("An employee with this email already exists.");
    }
}
