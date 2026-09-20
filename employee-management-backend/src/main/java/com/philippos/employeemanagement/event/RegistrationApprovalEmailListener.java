package com.philippos.employeemanagement.event;

import com.philippos.employeemanagement.service.EmailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RegistrationApprovalEmailListener {

    private final EmailService emailService;

    public RegistrationApprovalEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendApprovalEmail(RegistrationApprovedEvent event) {
        emailService.sendRegistrationApproved(event.recipient(), event.username());
    }
}
