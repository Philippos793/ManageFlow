package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.entity.WorkShift;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.repository.DepartmentRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.WorkShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock EmployeeRepository employeeRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock WorkShiftRepository workShiftRepository;
    @Mock Clock clock;
    @InjectMocks DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-05-15T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Athens"));
    }

    @Test
    void returnsActiveEmployeeAndDepartmentTotals() {
        when(employeeRepository.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(10L);
        when(departmentRepository.count()).thenReturn(5L);
        var result = dashboardService.getAdminSummary();
        assertEquals(10, result.getTotalEmployees());
        assertEquals(5, result.getTotalDepartments());
        verify(employeeRepository).countByStatus(EmployeeStatus.ACTIVE);
        verify(employeeRepository, never()).count();
    }

    @Test
    void returnsActiveShiftCount() {
        when(workShiftRepository.countByEndTimeIsNull()).thenReturn(2L);
        assertEquals(2, dashboardService.getAdminSummary().getActiveShifts());
    }

    @Test
    void sumsCompletedHoursForCurrentMonth() {
        Instant first = localInstant(YearMonth.of(2026, 5).atDay(2).atTime(8, 0));
        Instant second = localInstant(YearMonth.of(2026, 5).atDay(3).atTime(9, 0));
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(new WorkShift(null, first, first.plusSeconds(2 * 3600)),
                        new WorkShift(null, second, second.plusSeconds(90 * 60))));
        var result = dashboardService.getAdminSummary();
        assertEquals(210, result.getHoursThisMonthMinutes());
        assertEquals("3h 30m", result.getHoursThisMonth());
    }

    @Test
    void clipsShiftThatStartedBeforeCurrentMonth() {
        Instant monthStart = localInstant(YearMonth.of(2026, 5).atDay(1).atStartOfDay());
        WorkShift crossingShift = new WorkShift(
                null, monthStart.minusSeconds(2 * 3600), monthStart.plusSeconds(2 * 3600));
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(crossingShift));

        var result = dashboardService.getAdminSummary();

        assertEquals(120, result.getHoursThisMonthMinutes());
        assertEquals("2h 0m", result.getHoursThisMonth());
    }
    @Test
    void openShiftDoesNotCountTowardsMonthlyHours() {
        Instant start = localInstant(YearMonth.of(2026, 5).atDay(4).atTime(8, 0));
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(new WorkShift(null, start, null)));
        var result = dashboardService.getAdminSummary();
        assertEquals(0, result.getHoursThisMonthMinutes());
        assertEquals("0h 0m", result.getHoursThisMonth());
    }

    private Instant localInstant(LocalDateTime value) {
        return value.atZone(clock.getZone()).toInstant();
    }
}
