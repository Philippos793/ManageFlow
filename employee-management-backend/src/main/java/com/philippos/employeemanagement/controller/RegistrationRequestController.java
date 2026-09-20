package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.request.RegistrationApprovalRequest;
import com.philippos.employeemanagement.dto.request.RegistrationSubmissionRequest;
import com.philippos.employeemanagement.dto.response.RegistrationRequestResponse;
import com.philippos.employeemanagement.service.RegistrationRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/registration-requests")
public class RegistrationRequestController {
    private final RegistrationRequestService service;
    public RegistrationRequestController(RegistrationRequestService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<RegistrationRequestResponse> submit(
            @Valid @RequestBody RegistrationSubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(request));
    }

    @GetMapping("/pending")
    public List<RegistrationRequestResponse> getPending() { return service.getPending(); }

    @PostMapping("/{id}/approve")
    public RegistrationRequestResponse approve(@PathVariable Long id,
            @Valid @RequestBody RegistrationApprovalRequest request) {
        return service.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    public RegistrationRequestResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }
}
