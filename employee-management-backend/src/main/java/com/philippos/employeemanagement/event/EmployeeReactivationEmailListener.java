package com.philippos.employeemanagement.event;

import com.philippos.employeemanagement.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmployeeReactivationEmailListener {
    private final EmailService emailService;
    private final String frontendBaseUrl;

    public EmployeeReactivationEmailListener(
            EmailService emailService,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendReactivationNotification(EmployeeReactivatedEvent event) {
        emailService.sendEmployeeReactivated(
                event.recipient(), event.employeeName(), frontendBaseUrl + "/login");
    }
}
