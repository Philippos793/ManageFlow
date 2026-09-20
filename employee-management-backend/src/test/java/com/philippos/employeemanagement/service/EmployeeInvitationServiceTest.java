package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.AcceptEmployeeInvitationRequest;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeInvitation;
import com.philippos.employeemanagement.entity.EmployeeInvitationStatus;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.event.EmployeeInvitationCreatedEvent;
import com.philippos.employeemanagement.event.EmployeeReactivatedEvent;
import com.philippos.employeemanagement.exception.EmployeeInvitationConflictException;
import com.philippos.employeemanagement.repository.EmployeeInvitationRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeInvitationServiceTest {
    @Mock EmployeeInvitationRepository invitations;
    @Mock EmployeeRepository employees;
    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApplicationEventPublisher events;

    private final Instant now = Instant.parse("2026-09-15T12:00:00Z");
    private EmployeeInvitationService service;
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = new EmployeeInvitationService(
                invitations, employees, users, passwordEncoder, events,
                Clock.fixed(now, ZoneOffset.UTC));
        employee = new Employee();
        employee.setId(7L);
        employee.setFirstName("Maria");
        employee.setLastName("Georgiou");
        employee.setEmail("maria@example.com");
        employee.setStatus(EmployeeStatus.ACTIVE);
    }

    @Test
    void createsHashedInvitationWithFortyEightHourExpiration() {
        when(invitations.saveAndFlush(any(EmployeeInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createInvitationForEmployee(employee);

        ArgumentCaptor<EmployeeInvitation> invitationCaptor =
                ArgumentCaptor.forClass(EmployeeInvitation.class);
        verify(invitations).saveAndFlush(invitationCaptor.capture());
        EmployeeInvitation invitation = invitationCaptor.getValue();
        assertEquals(64, invitation.getTokenHash().length());
        assertEquals(now.plusSeconds(48 * 3600), invitation.getExpiresAt());
        assertEquals(EmployeeInvitationStatus.PENDING, invitation.getStatus());
        assertEquals(7L, response.getEmployeeId());

        ArgumentCaptor<EmployeeInvitationCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(EmployeeInvitationCreatedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertNotEquals(eventCaptor.getValue().token(), invitation.getTokenHash());
        assertEquals(43, eventCaptor.getValue().token().length());
    }

    @Test
    void acceptsInvitationAndCreatesLinkedEmployeeUser() {
        EmployeeInvitation invitation = pendingInvitation();
        when(invitations.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(invitation));
        when(passwordEncoder.encode("secure-pass")).thenReturn("bcrypt-hash");

        var response = service.acceptInvitation(
                new AcceptEmployeeInvitationRequest(
                        "raw-token", " Maria.User ", "secure-pass"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(userCaptor.capture());
        User user = userCaptor.getValue();
        assertEquals("maria.user", user.getUsername());
        assertEquals("bcrypt-hash", user.getPassword());
        assertEquals(Role.EMPLOYEE, user.getRole());
        assertEquals(employee, user.getEmployee());
        assertEquals(EmployeeInvitationStatus.USED, invitation.getStatus());
        assertEquals(now, invitation.getUsedAt());
        assertEquals("maria.user", response.getUsername());
    }

    @Test
    void usedInvitationCannotBeAcceptedAgain() {
        EmployeeInvitation invitation = pendingInvitation();
        invitation.setStatus(EmployeeInvitationStatus.USED);
        when(invitations.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(invitation));

        assertThrows(EmployeeInvitationConflictException.class, () ->
                service.acceptInvitation(new AcceptEmployeeInvitationRequest(
                        "raw-token", "maria", "secure-pass")));
        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void expiredInvitationCannotBeAccepted() {
        EmployeeInvitation invitation = pendingInvitation();
        invitation.setExpiresAt(now);
        when(invitations.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(invitation));

        EmployeeInvitationConflictException exception = assertThrows(
                EmployeeInvitationConflictException.class, () ->
                        service.acceptInvitation(new AcceptEmployeeInvitationRequest(
                                "raw-token", "maria", "secure-pass")));
        assertEquals("Invitation has expired", exception.getMessage());
    }

    @Test
    void duplicateUsernameIsRejected() {
        when(invitations.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(pendingInvitation()));
        when(users.existsByUsernameIgnoreCase("maria")).thenReturn(true);

        EmployeeInvitationConflictException exception = assertThrows(
                EmployeeInvitationConflictException.class, () ->
                        service.acceptInvitation(new AcceptEmployeeInvitationRequest(
                                "raw-token", "Maria", "secure-pass")));
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void resendRevokesPreviousInvitationAndIssuesANewOne() {
        EmployeeInvitation previous = pendingInvitation();
        when(employees.findById(7L)).thenReturn(Optional.of(employee));
        when(invitations.findByEmployeeIdAndStatus(
                7L, EmployeeInvitationStatus.PENDING))
                .thenReturn(Optional.of(previous));
        when(invitations.saveAndFlush(any(EmployeeInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.resendInvitation(7L);

        assertEquals(EmployeeInvitationStatus.REVOKED, previous.getStatus());
        verify(invitations).saveAndFlush(previous);
        verify(events).publishEvent(any(EmployeeInvitationCreatedEvent.class));
    }

    @Test
    void deactivationRevokesPendingInvitation() {
        EmployeeInvitation pending = pendingInvitation();
        when(invitations.findByEmployeeIdAndStatus(
                7L, EmployeeInvitationStatus.PENDING))
                .thenReturn(Optional.of(pending));

        service.revokePendingInvitation(7L);

        assertEquals(EmployeeInvitationStatus.REVOKED, pending.getStatus());
        verify(invitations).saveAndFlush(pending);
    }

    @Test
    void reactivationWithLinkedUserDoesNotCreateInvitation() {
        EmployeeInvitation pending = pendingInvitation();
        when(invitations.findByEmployeeIdAndStatus(
                7L, EmployeeInvitationStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(users.existsByEmployee_Id(7L)).thenReturn(true);

        service.handleEmployeeReactivation(employee);

        assertEquals(EmployeeInvitationStatus.REVOKED, pending.getStatus());
        verify(events, never()).publishEvent(any(EmployeeInvitationCreatedEvent.class));
        ArgumentCaptor<EmployeeReactivatedEvent> eventCaptor =
                ArgumentCaptor.forClass(EmployeeReactivatedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertEquals("maria@example.com", eventCaptor.getValue().recipient());
        assertEquals("Maria Georgiou", eventCaptor.getValue().employeeName());
    }

    @Test
    void reactivationWithoutLinkedUserCreatesFreshInvitation() {
        EmployeeInvitation pending = pendingInvitation();
        when(invitations.findByEmployeeIdAndStatus(
                7L, EmployeeInvitationStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(invitations.saveAndFlush(any(EmployeeInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.handleEmployeeReactivation(employee);

        assertEquals(EmployeeInvitationStatus.REVOKED, pending.getStatus());
        verify(events).publishEvent(any(EmployeeInvitationCreatedEvent.class));
        verify(events, never()).publishEvent(any(EmployeeReactivatedEvent.class));
    }

    private EmployeeInvitation pendingInvitation() {
        EmployeeInvitation invitation = new EmployeeInvitation();
        invitation.setEmployee(employee);
        invitation.setTokenHash("hash");
        invitation.setStatus(EmployeeInvitationStatus.PENDING);
        invitation.setCreatedAt(now);
        invitation.setExpiresAt(now.plusSeconds(3600));
        return invitation;
    }
}
