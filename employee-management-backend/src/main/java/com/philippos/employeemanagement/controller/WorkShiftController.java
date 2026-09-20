package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.response.CurrentWorkShiftResponse;
import com.philippos.employeemanagement.dto.response.EmployeeMonthlyWorkDetailsResponse;
import com.philippos.employeemanagement.dto.response.WorkShiftReportResponse;
import com.philippos.employeemanagement.dto.response.WorkShiftResponse;
import com.philippos.employeemanagement.service.WorkShiftService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/work-shifts")
public class WorkShiftController {
    private final WorkShiftService workShiftService;

    public WorkShiftController(WorkShiftService workShiftService) {
        this.workShiftService = workShiftService;
    }

    @PostMapping("/start")
    public WorkShiftResponse startShift() {
        return workShiftService.startShift();
    }

    @PostMapping("/end")
    public WorkShiftResponse endShift() {
        return workShiftService.endShift();
    }


    @GetMapping("/current")
    public CurrentWorkShiftResponse getCurrentShift() {
        return workShiftService.getCurrentShift();
    }

    @GetMapping("/report/{employeeId}")
    public EmployeeMonthlyWorkDetailsResponse getEmployeeMonthlyDetails(
            @PathVariable Long employeeId,
            @RequestParam @Min(value = 2000, message = "Year must be at least 2000") int year,
            @RequestParam @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12") int month) {
        return workShiftService.getEmployeeMonthlyDetails(employeeId, year, month);
    }
    @GetMapping("/report")
    public List<WorkShiftReportResponse> getMonthlyReport(
            @RequestParam @Min(value = 2000, message = "Year must be at least 2000") int year,
            @RequestParam @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12") int month) {
        return workShiftService.getMonthlyReport(year, month);
    }
}