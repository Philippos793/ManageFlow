package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.AdminDashboardSummaryResponse;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.repository.DepartmentRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.WorkShiftRepository;
import com.philippos.employeemanagement.util.WorkShiftTimeCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkShiftRepository workShiftRepository;
    private final Clock clock;

    public DashboardService(EmployeeRepository employeeRepository,
                            DepartmentRepository departmentRepository,
                            WorkShiftRepository workShiftRepository,
                            Clock clock) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.workShiftRepository = workShiftRepository;
        this.clock = clock;
    }

    public AdminDashboardSummaryResponse getAdminSummary() {
        YearMonth currentMonth = YearMonth.now(clock);
        Instant start = currentMonth.atDay(1)
                .atStartOfDay(clock.getZone()).toInstant();
        Instant end = currentMonth.plusMonths(1).atDay(1)
                .atStartOfDay(clock.getZone()).toInstant();

        long minutes = WorkShiftTimeCalculator.calculateTotalMinutesWithin(
                workShiftRepository.findCompletedShiftsOverlapping(start, end),
                start,
                end);

        return new AdminDashboardSummaryResponse(
                employeeRepository.countByStatus(EmployeeStatus.ACTIVE),
                departmentRepository.count(),
                workShiftRepository.countByEndTimeIsNull(),
                minutes,
                formatHours(minutes));
    }

    private String formatHours(long minutes) {
        return minutes / 60 + "h " + minutes % 60 + "m";
    }
}
