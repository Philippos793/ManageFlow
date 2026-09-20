package com.philippos.employeemanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String senderAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String senderAddress) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }

    public void sendRegistrationApproved(String recipient, String username) {
        LOGGER.info("Attempting to send approval email to {}", recipient);

        SimpleMailMessage message = new SimpleMailMessage();
        if (senderAddress != null && !senderAddress.isBlank()) {
            message.setFrom(senderAddress);
        }
        message.setTo(recipient);
        message.setSubject("ManageFlow registration approved");
        message.setText("Your ManageFlow registration request has been approved.\n\n"
                + "Your account is now active. You can log in using the username you selected: "
                + username + ".");

        try {
            mailSender.send(message);
            LOGGER.info("Approval email sent successfully to {}", recipient);
        } catch (MailException exception) {
            LOGGER.error("Approval email failed for {}", recipient, exception);
        }
    }

    public void sendEmployeeInvitation(
            String recipient, String employeeName, String setupLink) {
        LOGGER.info("Attempting to send employee invitation email to {}", recipient);

        SimpleMailMessage message = new SimpleMailMessage();
        if (senderAddress != null && !senderAddress.isBlank()) {
            message.setFrom(senderAddress);
        }
        message.setTo(recipient);
        message.setSubject("Set up your ManageFlow account");
        message.setText("Hello " + employeeName + ",\n\n"
                + "An employee profile has been created for you in ManageFlow.\n"
                + "Use this secure link within 48 hours to set up your account:\n\n"
                + setupLink + "\n\n"
                + "If you were not expecting this invitation, you can ignore this email.");

        try {
            mailSender.send(message);
            LOGGER.info("Employee invitation email sent successfully to {}", recipient);
        } catch (MailException exception) {
            LOGGER.error("Employee invitation email failed for {}", recipient, exception);
        }
    }

    public void sendEmployeeReactivated(
            String recipient, String employeeName, String loginLink) {
        LOGGER.info("Attempting to send employee reactivation email to {}", recipient);

        SimpleMailMessage message = new SimpleMailMessage();
        if (senderAddress != null && !senderAddress.isBlank()) {
            message.setFrom(senderAddress);
        }
        message.setTo(recipient);
        message.setSubject("Your ManageFlow account has been reactivated");
        message.setText("Hello " + employeeName + ",\n\n"
                + "Your ManageFlow account has been reactivated.\n"
                + "You can now log in using your existing credentials:\n\n"
                + loginLink);

        try {
            mailSender.send(message);
            LOGGER.info("Employee reactivation email sent successfully to {}", recipient);
        } catch (MailException exception) {
            LOGGER.error("Employee reactivation email failed for {}", recipient, exception);
        }
    }
}
