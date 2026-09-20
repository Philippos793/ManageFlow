package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.request.EmployeeAccountRequest;
import com.philippos.employeemanagement.dto.response.EmployeeAccountResponse;
import com.philippos.employeemanagement.service.EmployeeAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final EmployeeAccountService employeeAccountService;
    public UserController(EmployeeAccountService employeeAccountService) {
        this.employeeAccountService = employeeAccountService;
    }
    @PostMapping("/employee-account")
    public ResponseEntity<EmployeeAccountResponse> createEmployeeAccount(
            @Valid @RequestBody EmployeeAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeAccountService.createEmployeeAccount(request));
    }
}
