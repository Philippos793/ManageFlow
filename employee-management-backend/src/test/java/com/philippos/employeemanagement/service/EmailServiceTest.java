package com.philippos.employeemanagement.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    @Test
    void sendsApprovalMessageWithoutPassword() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, "manageflow@example.com");

        service.sendRegistrationApproved("alex@example.com", "alex");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("ManageFlow registration approved", message.getSubject());
        assertEquals("alex@example.com", message.getTo()[0]);
        assertEquals("manageflow@example.com", message.getFrom());
        assertTrue(message.getText().contains("account is now active"));
        assertTrue(message.getText().contains("alex"));
        assertFalse(message.getText().toLowerCase().contains("password"));
    }

    @Test
    void mailFailureDoesNotPropagateToApprovalFlow() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailService service = new EmailService(mailSender, "");

        assertDoesNotThrow(() ->
                service.sendRegistrationApproved("alex@example.com", "alex"));
    }

    @Test
    void sendsInvitationWithSetupLinkAndWithoutPassword() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, "manageflow@example.com");

        service.sendEmployeeInvitation(
                "maria@example.com",
                "Maria Georgiou",
                "http://localhost:5173/accept-invitation#token=safe-token");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("Set up your ManageFlow account", message.getSubject());
        assertTrue(message.getText().contains("Maria Georgiou"));
        assertTrue(message.getText().contains("/accept-invitation#token=safe-token"));
        assertFalse(message.getText().toLowerCase().contains("your password"));
    }

    @Test
    void sendsReactivationNotificationWithLoginLinkAndNoCredentials() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, "manageflow@example.com");

        service.sendEmployeeReactivated(
                "maria@example.com", "Maria Georgiou",
                "http://localhost:5173/login");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("Your ManageFlow account has been reactivated", message.getSubject());
        assertEquals("maria@example.com", message.getTo()[0]);
        assertTrue(message.getText().contains("account has been reactivated"));
        assertTrue(message.getText().contains("existing credentials"));
        assertTrue(message.getText().contains("http://localhost:5173/login"));
        assertTrue(message.getText().contains("Maria Georgiou,\n\n"));
        assertTrue(message.getText().contains("reactivated.\n"));
        assertFalse(message.getText().contains("\\n"));
        assertFalse(message.getText().toLowerCase().contains("password"));
        assertFalse(message.getText().contains("maria.user"));
    }

    @Test
    void reactivationMailFailureDoesNotPropagate() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailService service = new EmailService(mailSender, "");

        assertDoesNotThrow(() -> service.sendEmployeeReactivated(
                "maria@example.com", "Maria Georgiou",
                "http://localhost:5173/login"));
    }
}
