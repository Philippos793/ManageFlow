package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.WorkShift;
import com.philippos.employeemanagement.mapper.WorkShiftMapper;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.repository.WorkShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeMonthlyDetailsTest {
    @Mock WorkShiftRepository shifts;
    @Mock UserRepository users;
    @Mock EmployeeRepository employees;
    @Mock WorkShiftMapper mapper;
    WorkShiftService service;
    Employee employee;
    Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-05-15T12:00:00Z"), ZoneId.of("Europe/Athens"));
        service = new WorkShiftService(shifts, users, employees, mapper, clock);
        employee = new Employee(); employee.setId(1L);
        employee.setFirstName("Maria"); employee.setLastName("Georgiou");
        employee.setHourlyRate(new BigDecimal("12.50"));
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
    }

    @Test
    void aggregatesMultipleShiftsOnSameDay() {
        when(shifts.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(shift("2026-05-03T08:00", "2026-05-03T10:00"),
                        shift("2026-05-03T11:00", "2026-05-03T12:30")));
        var result = service.getEmployeeMonthlyDetails(1L, 2026, 5);
        assertEquals(210, result.getDailyWorkedTimes().get(2).getWorkedMinutes());
        assertEquals("3h 30m", result.getDailyWorkedTimes().get(2).getWorkedTime());
    }

    @Test
    void includesEveryDayAndZeroForDaysWithoutShift() {
        when(shifts.findCompletedShiftsOverlapping(any(), any())).thenReturn(List.of());
        var result = service.getEmployeeMonthlyDetails(1L, 2026, 2);
        assertEquals(28, result.getDailyWorkedTimes().size());
        assertEquals(0, result.getDailyWorkedTimes().get(10).getWorkedMinutes());
    }

    @Test
    void calculatesMonthlyTotalAndIgnoresOpenShift() {
        when(shifts.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(shift("2026-05-03T08:00", "2026-05-03T10:00"),
                        new WorkShift(employee, localInstant("2026-05-04T08:00"), null),
                        shift("2026-05-05T08:00", "2026-05-05T09:15")));
        var result = service.getEmployeeMonthlyDetails(1L, 2026, 5);
        assertEquals(195, result.getTotalWorkedMinutes());
        assertEquals("3h 15m", result.getTotalWorkedHours());
        assertEquals(new BigDecimal("12.50"), result.getHourlyRate());
    }

    @Test
    void splitsCrossMonthShiftIntoTheCorrectDailyEntries() {
        WorkShift overnightShift = shift(
                "2026-09-30T22:00", "2026-10-01T02:00");
        when(shifts.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(overnightShift));

        var september = service.getEmployeeMonthlyDetails(1L, 2026, 9);
        var october = service.getEmployeeMonthlyDetails(1L, 2026, 10);

        assertEquals(120, september.getDailyWorkedTimes().get(29).getWorkedMinutes());
        assertEquals(120, september.getTotalWorkedMinutes());
        assertEquals(120, october.getDailyWorkedTimes().getFirst().getWorkedMinutes());
        assertEquals(120, october.getTotalWorkedMinutes());
    }
    @Test
    void nullableHourlyRateIsReturned() {
        employee.setHourlyRate(null);
        when(shifts.findCompletedShiftsOverlapping(any(), any())).thenReturn(List.of());
        assertNull(service.getEmployeeMonthlyDetails(1L, 2026, 5).getHourlyRate());
    }

    @Test
    void dailyReportUsesActualElapsedMinutesOnSpringDstDay() {
        Instant dayStart = localInstant("2026-03-29T00:00");
        Instant dayEnd = localInstant("2026-03-30T00:00");
        when(shifts.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(new WorkShift(employee, dayStart, dayEnd)));

        var result = service.getEmployeeMonthlyDetails(1L, 2026, 3);

        assertEquals(23 * 60,
                result.getDailyWorkedTimes().get(28).getWorkedMinutes());
    }

    private WorkShift shift(String start, String end) {
        return new WorkShift(employee, localInstant(start), localInstant(end));
    }

    private Instant localInstant(String value) {
        return LocalDateTime.parse(value).atZone(clock.getZone()).toInstant();
    }
}
