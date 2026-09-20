package com.philippos.employeemanagement.event;

import com.philippos.employeemanagement.service.EmailService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RegistrationApprovalEmailListenerTest {

    @Test
    void sendsApprovalEmailFromCommittedEvent() {
        EmailService emailService = mock(EmailService.class);
        RegistrationApprovalEmailListener listener =
                new RegistrationApprovalEmailListener(emailService);

        listener.sendApprovalEmail(
                new RegistrationApprovedEvent("alex@example.com", "alex"));

        verify(emailService).sendRegistrationApproved("alex@example.com", "alex");
    }
}
