package com.philippos.employeemanagement.event;

import com.philippos.employeemanagement.service.EmailService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmployeeReactivationEmailListenerTest {
    @Test
    void sendsNotificationToConfiguredFrontendLoginPage() {
        EmailService emailService = mock(EmailService.class);
        EmployeeReactivationEmailListener listener =
                new EmployeeReactivationEmailListener(
                        emailService, "https://manageflow.example.com/");

        listener.sendReactivationNotification(
                new EmployeeReactivatedEvent(
                        "maria@example.com", "Maria Georgiou"));

        verify(emailService).sendEmployeeReactivated(
                "maria@example.com",
                "Maria Georgiou",
                "https://manageflow.example.com/login");
    }
}
