package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.AcceptEmployeeInvitationRequest;
import com.philippos.employeemanagement.dto.response.EmployeeInvitationResponse;
import com.philippos.employeemanagement.dto.response.InvitationAcceptanceResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeInvitation;
import com.philippos.employeemanagement.entity.EmployeeInvitationStatus;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.event.EmployeeInvitationCreatedEvent;
import com.philippos.employeemanagement.event.EmployeeReactivatedEvent;
import com.philippos.employeemanagement.exception.EmployeeAccountConflictException;
import com.philippos.employeemanagement.exception.EmployeeInvitationConflictException;
import com.philippos.employeemanagement.exception.EmployeeInvitationNotFoundException;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.repository.EmployeeInvitationRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.util.AccountValidation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EmployeeInvitationService {
    private static final Duration INVITATION_LIFETIME = Duration.ofHours(48);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmployeeInvitationRepository invitationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public EmployeeInvitationService(
            EmployeeInvitationRepository invitationRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.invitationRepository = invitationRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public EmployeeInvitationResponse createInvitationForEmployee(Employee employee) {
        if (userRepository.existsByEmployee_Id(employee.getId())) {
            throw new EmployeeAccountConflictException(
                    "Employee already has a user account");
        }
        if (invitationRepository.findByEmployeeIdAndStatus(
                employee.getId(), EmployeeInvitationStatus.PENDING).isPresent()) {
            throw new EmployeeInvitationConflictException(
                    "Employee already has a pending invitation");
        }
        return issueInvitation(employee);
    }

    @Transactional
    public EmployeeInvitationResponse resendInvitation(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found"));
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeInvitationConflictException(
                    "Inactive employees cannot receive account invitations");
        }
        if (userRepository.existsByEmployee_Id(employeeId)) {
            throw new EmployeeAccountConflictException(
                    "Employee already has a user account");
        }

        invitationRepository.findByEmployeeIdAndStatus(
                employeeId, EmployeeInvitationStatus.PENDING).ifPresent(existing -> {
                    existing.setStatus(existing.getExpiresAt().isAfter(clock.instant())
                            ? EmployeeInvitationStatus.REVOKED
                            : EmployeeInvitationStatus.EXPIRED);
                    invitationRepository.saveAndFlush(existing);
                });
        return issueInvitation(employee);
    }

    @Transactional
    public void revokePendingInvitation(Long employeeId) {
        invitationRepository.findByEmployeeIdAndStatus(
                employeeId, EmployeeInvitationStatus.PENDING).ifPresent(invitation -> {
                    invitation.setStatus(EmployeeInvitationStatus.REVOKED);
                    invitationRepository.saveAndFlush(invitation);
                });
    }

    @Transactional
    public void handleEmployeeReactivation(Employee employee) {
        revokePendingInvitation(employee.getId());
        if (userRepository.existsByEmployee_Id(employee.getId())) {
            eventPublisher.publishEvent(new EmployeeReactivatedEvent(
                    employee.getEmail(),
                    employee.getFirstName() + " " + employee.getLastName()));
        } else {
            issueInvitation(employee);
        }
    }

    @Transactional(readOnly = true)
    public EmployeeInvitationResponse validateInvitation(String token) {
        EmployeeInvitation invitation = invitationRepository.findByTokenHash(hashToken(token))
                .orElseThrow(this::invalidInvitation);
        ensureUsable(invitation);
        return toResponse(invitation);
    }

    @Transactional
    public InvitationAcceptanceResponse acceptInvitation(
            AcceptEmployeeInvitationRequest request) {
        EmployeeInvitation invitation = invitationRepository
                .findByTokenHashForUpdate(hashToken(request.getToken()))
                .orElseThrow(this::invalidInvitation);
        ensureUsable(invitation);

        Employee employee = invitation.getEmployee();
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeInvitationConflictException(
                    "This employee account is inactive");
        }
        if (userRepository.existsByEmployee_Id(employee.getId())) {
            throw new EmployeeInvitationConflictException(
                    "Employee already has a user account");
        }

        String username = AccountValidation.normalizeUsername(request.getUsername());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new EmployeeInvitationConflictException("Username already exists");
        }

        User user = new User(
                username, passwordEncoder.encode(request.getPassword()), Role.EMPLOYEE);
        user.setEmployee(employee);
        try {
            userRepository.saveAndFlush(user);
            invitation.setStatus(EmployeeInvitationStatus.USED);
            invitation.setUsedAt(clock.instant());
            invitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException exception) {
            throw new EmployeeInvitationConflictException(
                    "Username or employee account already exists");
        }

        return new InvitationAcceptanceResponse(
                "Your account has been created successfully.", username);
    }

    private EmployeeInvitationResponse issueInvitation(Employee employee) {
        Instant now = clock.instant();
        String token = generateToken();
        EmployeeInvitation invitation = new EmployeeInvitation();
        invitation.setEmployee(employee);
        invitation.setTokenHash(hashToken(token));
        invitation.setStatus(EmployeeInvitationStatus.PENDING);
        invitation.setCreatedAt(now);
        invitation.setExpiresAt(now.plus(INVITATION_LIFETIME));

        try {
            EmployeeInvitation saved = invitationRepository.saveAndFlush(invitation);
            eventPublisher.publishEvent(new EmployeeInvitationCreatedEvent(
                    employee.getEmail(),
                    employee.getFirstName() + " " + employee.getLastName(),
                    token));
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new EmployeeInvitationConflictException(
                    "Employee already has a pending invitation");
        }
    }

    private void ensureUsable(EmployeeInvitation invitation) {
        if (invitation.getStatus() != EmployeeInvitationStatus.PENDING) {
            throw new EmployeeInvitationConflictException(
                    "Invitation has already been used or revoked");
        }
        if (!invitation.getExpiresAt().isAfter(clock.instant())) {
            throw new EmployeeInvitationConflictException("Invitation has expired");
        }
    }

    private EmployeeInvitationResponse toResponse(EmployeeInvitation invitation) {
        Employee employee = invitation.getEmployee();
        return new EmployeeInvitationResponse(
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                invitation.getExpiresAt());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw invalidInvitation();
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private EmployeeInvitationNotFoundException invalidInvitation() {
        return new EmployeeInvitationNotFoundException(
                "Invitation is invalid or no longer available");
    }
}
