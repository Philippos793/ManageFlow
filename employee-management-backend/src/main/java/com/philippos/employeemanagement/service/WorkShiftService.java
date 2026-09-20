package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.CurrentWorkShiftResponse;
import com.philippos.employeemanagement.dto.response.DailyWorkedTimeResponse;
import com.philippos.employeemanagement.dto.response.EmployeeMonthlyWorkDetailsResponse;
import com.philippos.employeemanagement.dto.response.WorkShiftReportResponse;
import com.philippos.employeemanagement.dto.response.WorkShiftResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.entity.WorkShift;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.exception.WorkShiftConflictException;
import com.philippos.employeemanagement.mapper.WorkShiftMapper;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.repository.WorkShiftRepository;
import com.philippos.employeemanagement.util.WorkShiftTimeCalculator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkShiftService {
    private final WorkShiftRepository workShiftRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkShiftMapper workShiftMapper;
    private final Clock clock;

    public WorkShiftService(WorkShiftRepository workShiftRepository,
                            UserRepository userRepository,
                            EmployeeRepository employeeRepository,
                            WorkShiftMapper workShiftMapper,
                            Clock clock) {
        this.workShiftRepository = workShiftRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.workShiftMapper = workShiftMapper;
        this.clock = clock;
    }

    public WorkShiftResponse startShift() {
        Employee employee = getAuthenticatedEmployee();
        if (findOpenShift(employee).isPresent()) {
            throw new WorkShiftConflictException("Employee already has an open shift");
        }

        WorkShift workShift = new WorkShift(employee, clock.instant(), null);
        try {
            return workShiftMapper.toResponse(workShiftRepository.saveAndFlush(workShift));
        } catch (DataIntegrityViolationException exception) {
            throw new WorkShiftConflictException("Employee already has an open shift");
        }
    }

    public WorkShiftResponse endShift() {
        Employee employee = getAuthenticatedEmployee();
        WorkShift workShift = findOpenShift(employee)
                .orElseThrow(() -> new WorkShiftConflictException(
                        "Employee does not have an open shift"));

        workShift.setEndTime(clock.instant());
        return workShiftMapper.toResponse(workShiftRepository.save(workShift));
    }

    @Transactional(readOnly = true)
    public CurrentWorkShiftResponse getCurrentShift() {
        Employee employee = getAuthenticatedEmployee();
        return findOpenShift(employee)
                .map(shift -> new CurrentWorkShiftResponse(true, shift.getStartTime()))
                .orElseGet(() -> new CurrentWorkShiftResponse(false, null));
    }
    @Transactional(readOnly = true)
    public List<WorkShiftReportResponse> getMonthlyReport(int year, int month) {
        YearMonth requestedMonth = YearMonth.of(year, month);
        Instant start = monthStart(requestedMonth);
        Instant end = monthStart(requestedMonth.plusMonths(1));

        Map<Long, List<WorkShift>> shiftsByEmployee = workShiftRepository
                .findCompletedShiftsOverlapping(start, end)
                .stream()
                .filter(shift -> WorkShiftTimeCalculator.isCompletedAndOverlapping(
                        shift, start, end))
                .collect(Collectors.groupingBy(shift -> shift.getEmployee().getId()));

        return employeeRepository.findAll().stream()
                .map(employee -> createReportRow(
                        employee,
                        shiftsByEmployee.getOrDefault(employee.getId(), List.of()),
                        start,
                        end))
                .toList();
    }


    @Transactional(readOnly = true)
    public EmployeeMonthlyWorkDetailsResponse getEmployeeMonthlyDetails(
            Long employeeId, int year, int month) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee with id " + employeeId + " not found"));
        YearMonth requestedMonth = YearMonth.of(year, month);
        Instant start = monthStart(requestedMonth);
        Instant end = monthStart(requestedMonth.plusMonths(1));
        List<WorkShift> completedShifts = workShiftRepository
                .findCompletedShiftsOverlapping(start, end)
                .stream()
                .filter(shift -> WorkShiftTimeCalculator.isCompletedAndOverlapping(
                        shift, start, end))
                .filter(shift -> shift.getEmployee().getId().equals(employeeId))
                .toList();

        List<DailyWorkedTimeResponse> days = new ArrayList<>();
        long totalMinutes = 0;
        for (int day = 1; day <= requestedMonth.lengthOfMonth(); day++) {
            Instant dayStart = requestedMonth.atDay(day)
                    .atStartOfDay(clock.getZone()).toInstant();
            Instant dayEnd = requestedMonth.atDay(day).plusDays(1)
                    .atStartOfDay(clock.getZone()).toInstant();
            long minutes = WorkShiftTimeCalculator.calculateTotalMinutesWithin(
                    completedShifts, dayStart, dayEnd);
            totalMinutes += minutes;
            days.add(new DailyWorkedTimeResponse(day, minutes, formatHours(minutes)));
        }

        return new EmployeeMonthlyWorkDetailsResponse(
                employeeId,
                employee.getFirstName() + " " + employee.getLastName(),
                year, month, days, totalMinutes, formatHours(totalMinutes),
                employee.getHourlyRate());
    }
    private WorkShiftReportResponse createReportRow(
            Employee employee,
            List<WorkShift> completedShifts,
            Instant periodStart,
            Instant periodEnd) {
        long totalMinutes = WorkShiftTimeCalculator.calculateTotalMinutesWithin(
                completedShifts, periodStart, periodEnd);

        return new WorkShiftReportResponse(
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                totalMinutes,
                formatHours(totalMinutes),
                completedShifts.size());
    }

    private String formatHours(long totalMinutes) {
        return totalMinutes / 60 + "h " + totalMinutes % 60 + "m";
    }

    private Instant monthStart(YearMonth month) {
        return month.atDay(1).atStartOfDay(clock.getZone()).toInstant();
    }

    private Optional<WorkShift> findOpenShift(Employee employee) {
        return workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(employee.getId());
    }

    private Employee getAuthenticatedEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new WorkShiftConflictException(
                        "Authenticated user was not found"));

        if (user.getEmployee() == null) {
            throw new WorkShiftConflictException(
                    "Authenticated user is not linked to an employee");
        }
        return user.getEmployee();
    }
}
