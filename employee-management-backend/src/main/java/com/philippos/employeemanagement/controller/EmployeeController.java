package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.request.EmployeeRequest;
import com.philippos.employeemanagement.dto.request.EmployeeReactivationRequest;
import com.philippos.employeemanagement.dto.request.HourlyRateRequest;
import com.philippos.employeemanagement.dto.response.EmployeeResponse;
import com.philippos.employeemanagement.dto.response.HourlyRateResponse;
import com.philippos.employeemanagement.service.EmployeeService;
import com.philippos.employeemanagement.service.EmployeeInvitationService;
import com.philippos.employeemanagement.dto.response.EmployeeInvitationResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeInvitationService invitationService;

    public EmployeeController(
            EmployeeService employeeService,
            EmployeeInvitationService invitationService) {
        this.employeeService = employeeService;
        this.invitationService = invitationService;
    }

    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @PostMapping
    public EmployeeResponse createEmployee(
            @Valid @RequestBody EmployeeRequest request) {

        return employeeService.saveEmployee(request);
    }

    @PostMapping("/{id}/invitation")
    public EmployeeInvitationResponse resendInvitation(@PathVariable Long id) {
        return invitationService.resendInvitation(id);
    }

    @PostMapping("/{id}/reactivate")
    public EmployeeResponse reactivateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeReactivationRequest request) {
        return employeeService.reactivateEmployee(id, request);
    }

    @PutMapping("/{id}")
    public EmployeeResponse updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {

        return employeeService.updateEmployee(id, request);
    }


    @PutMapping("/{id}/hourly-rate")
    public HourlyRateResponse updateHourlyRate(
            @PathVariable Long id,
            @Valid @RequestBody HourlyRateRequest request) {
        return employeeService.updateHourlyRate(id, request);
    }
    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
    }
}


