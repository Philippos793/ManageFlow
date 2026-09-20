package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.request.AcceptEmployeeInvitationRequest;
import com.philippos.employeemanagement.dto.request.InvitationTokenRequest;
import com.philippos.employeemanagement.dto.response.EmployeeInvitationResponse;
import com.philippos.employeemanagement.dto.response.InvitationAcceptanceResponse;
import com.philippos.employeemanagement.service.EmployeeInvitationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employee-invitations")
public class EmployeeInvitationController {
    private final EmployeeInvitationService invitationService;

    public EmployeeInvitationController(EmployeeInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/validate")
    public EmployeeInvitationResponse validate(
            @Valid @RequestBody InvitationTokenRequest request) {
        return invitationService.validateInvitation(request.getToken());
    }

    @PostMapping("/accept")
    public InvitationAcceptanceResponse accept(
            @Valid @RequestBody AcceptEmployeeInvitationRequest request) {
        return invitationService.acceptInvitation(request);
    }
}
