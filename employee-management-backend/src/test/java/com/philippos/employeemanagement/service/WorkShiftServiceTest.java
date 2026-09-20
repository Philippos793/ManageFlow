package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.CurrentWorkShiftResponse;
import com.philippos.employeemanagement.dto.response.WorkShiftResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.entity.WorkShift;
import com.philippos.employeemanagement.exception.WorkShiftConflictException;
import com.philippos.employeemanagement.mapper.WorkShiftMapper;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.repository.WorkShiftRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkShiftServiceTest {
    @Mock private WorkShiftRepository workShiftRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private WorkShiftMapper workShiftMapper;
    @Mock private Clock clock;
    @InjectMocks private WorkShiftService workShiftService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("employee", null));
        employee = new Employee();
        employee.setId(10L);
        User user = new User("employee", "encoded", Role.EMPLOYEE);
        user.setEmployee(employee);
        lenient().when(userRepository.findByUsername("employee")).thenReturn(Optional.of(user));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-09-15T12:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Europe/Athens"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startShiftCreatesOpenShiftUsingServerTime() {
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(10L)).thenReturn(Optional.empty());
        when(workShiftRepository.saveAndFlush(any(WorkShift.class))).thenAnswer(invocation -> {
            WorkShift shift = invocation.getArgument(0);
            shift.setId(1L);
            return shift;
        });
        WorkShiftResponse response = new WorkShiftResponse(1L, 10L, clock.instant(), null);
        when(workShiftMapper.toResponse(any(WorkShift.class))).thenReturn(response);

        assertSame(response, workShiftService.startShift());

        ArgumentCaptor<WorkShift> captor = ArgumentCaptor.forClass(WorkShift.class);
        verify(workShiftRepository).saveAndFlush(captor.capture());
        assertSame(employee, captor.getValue().getEmployee());
        assertNull(captor.getValue().getEndTime());
        assertEquals(clock.instant(), captor.getValue().getStartTime());
    }

    @Test
    void startShiftConvertsConcurrentOpenShiftViolationToConflict() {
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(10L))
                .thenReturn(Optional.empty());
        when(workShiftRepository.saveAndFlush(any(WorkShift.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate open shift"));

        WorkShiftConflictException exception = assertThrows(
                WorkShiftConflictException.class, () -> workShiftService.startShift());

        assertEquals("Employee already has an open shift", exception.getMessage());
    }
    @Test
    void startShiftRejectsDuplicateOpenShift() {
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(10L))
                .thenReturn(Optional.of(new WorkShift()));
        WorkShiftConflictException exception = assertThrows(
                WorkShiftConflictException.class, () -> workShiftService.startShift());
        assertEquals("Employee already has an open shift", exception.getMessage());
        verify(workShiftRepository, never()).save(any());
    }

    @Test
    void endShiftClosesOpenShiftUsingServerTime() {
        WorkShift shift = new WorkShift(employee, clock.instant().minusSeconds(8 * 3600), null);
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(10L))
                .thenReturn(Optional.of(shift));
        when(workShiftRepository.save(shift)).thenReturn(shift);
        WorkShiftResponse response = new WorkShiftResponse(1L, 10L, shift.getStartTime(), clock.instant());
        when(workShiftMapper.toResponse(shift)).thenReturn(response);

        assertSame(response, workShiftService.endShift());
        assertNotNull(shift.getEndTime());
        assertEquals(clock.instant(), shift.getEndTime());
    }

    @Test
    void endShiftRejectsWhenThereIsNoOpenShift() {
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(10L)).thenReturn(Optional.empty());
        WorkShiftConflictException exception = assertThrows(
                WorkShiftConflictException.class, () -> workShiftService.endShift());
        assertEquals("Employee does not have an open shift", exception.getMessage());
        verify(workShiftRepository, never()).save(any());
    }

    @Test
    void shiftActionRejectsEmployeeUserWithoutLinkedEmployee() {
        User user = new User("employee", "encoded", Role.EMPLOYEE);
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(user));
        WorkShiftConflictException exception = assertThrows(
                WorkShiftConflictException.class, () -> workShiftService.startShift());
        assertEquals("Authenticated user is not linked to an employee", exception.getMessage());
        verifyNoInteractions(workShiftRepository);
    }

    @Test
    void currentShiftReturnsActiveStatusAndStartTime() {
        Instant startTime = Instant.parse("2026-09-14T06:00:00Z");
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(10L))
                .thenReturn(Optional.of(new WorkShift(employee, startTime, null)));

        CurrentWorkShiftResponse response = workShiftService.getCurrentShift();

        assertTrue(response.isActive());
        assertEquals(startTime, response.getStartTime());
    }

    @Test
    void currentShiftReturnsInactiveStatusWhenNoShiftIsOpen() {
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(10L))
                .thenReturn(Optional.empty());

        CurrentWorkShiftResponse response = workShiftService.getCurrentShift();

        assertFalse(response.isActive());
        assertNull(response.getStartTime());
    }

    @Test
    void currentShiftRejectsUserWithoutLinkedEmployee() {
        when(userRepository.findByUsername("employee"))
                .thenReturn(Optional.of(new User("employee", "encoded", Role.EMPLOYEE)));

        WorkShiftConflictException exception = assertThrows(
                WorkShiftConflictException.class, () -> workShiftService.getCurrentShift());

        assertEquals("Authenticated user is not linked to an employee", exception.getMessage());
        verifyNoInteractions(workShiftRepository);
    }
    @Test
    void monthlyReportCalculatesWorkedMinutesAndFormattedHours() {
        Employee reportEmployee = employee("Maria", "Papadopoulou", 10L);
        WorkShift shift = shift(reportEmployee, "2026-05-10T09:00", "2026-05-10T10:30");
        when(employeeRepository.findAll()).thenReturn(List.of(reportEmployee));
        when(workShiftRepository.findCompletedShiftsOverlapping(
                localInstant("2026-05-01T00:00"),
                localInstant("2026-06-01T00:00"))).thenReturn(List.of(shift));

        var report = workShiftService.getMonthlyReport(2026, 5);

        assertEquals(1, report.size());
        assertEquals(90, report.getFirst().getTotalWorkedMinutes());
        assertEquals("1h 30m", report.getFirst().getTotalWorkedHours());
        assertEquals("Maria Papadopoulou", report.getFirst().getFullName());
    }

    @Test
    void monthlyReportSplitsCrossMonthShiftBetweenSeptemberAndOctober() {
        Employee reportEmployee = employee("Maria", "Papadopoulou", 10L);
        WorkShift overnightShift = shift(
                reportEmployee, "2026-09-30T22:00", "2026-10-01T02:00");
        when(employeeRepository.findAll()).thenReturn(List.of(reportEmployee));
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(overnightShift));

        var september = workShiftService.getMonthlyReport(2026, 9).getFirst();
        var october = workShiftService.getMonthlyReport(2026, 10).getFirst();

        assertEquals(120, september.getTotalWorkedMinutes());
        assertEquals("2h 0m", september.getTotalWorkedHours());
        assertEquals(120, october.getTotalWorkedMinutes());
        assertEquals("2h 0m", october.getTotalWorkedHours());
    }
    @Test
    void monthlyReportRetainsInactiveEmployeeHistory() {
        Employee reportEmployee = employee("Maria", "Papadopoulou", 10L);
        reportEmployee.setStatus(EmployeeStatus.INACTIVE);
        WorkShift shift = shift(reportEmployee, "2026-05-10T09:00", "2026-05-10T10:30");
        when(employeeRepository.findAll()).thenReturn(List.of(reportEmployee));
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(shift));

        var report = workShiftService.getMonthlyReport(2026, 5);

        assertEquals(1, report.size());
        assertEquals(90, report.getFirst().getTotalWorkedMinutes());
        assertEquals("Maria Papadopoulou", report.getFirst().getFullName());
    }

    @Test
    void monthlyReportCombinesMultipleCompletedShifts() {
        Employee reportEmployee = employee("Maria", "Papadopoulou", 10L);
        when(employeeRepository.findAll()).thenReturn(List.of(reportEmployee));
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(
                        shift(reportEmployee, "2026-05-02T09:00", "2026-05-02T10:00"),
                        shift(reportEmployee, "2026-05-03T09:00", "2026-05-03T11:30")));

        var row = workShiftService.getMonthlyReport(2026, 5).getFirst();

        assertEquals(210, row.getTotalWorkedMinutes());
        assertEquals("3h 30m", row.getTotalWorkedHours());
        assertEquals(2, row.getCompletedShifts());
    }

    @Test
    void monthlyReportDoesNotCountOpenShift() {
        Employee reportEmployee = employee("Maria", "Papadopoulou", 10L);
        WorkShift openShift = new WorkShift(
                reportEmployee, localInstant("2026-05-04T09:00"), null);
        when(employeeRepository.findAll()).thenReturn(List.of(reportEmployee));
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(openShift));

        var row = workShiftService.getMonthlyReport(2026, 5).getFirst();

        assertEquals(0, row.getTotalWorkedMinutes());
        assertEquals(0, row.getCompletedShifts());
    }

    @Test
    void monthlyReportFiltersShiftsByRequestedMonth() {
        Employee reportEmployee = employee("Maria", "Papadopoulou", 10L);
        WorkShift mayShift = shift(reportEmployee, "2026-05-31T22:00", "2026-05-31T23:00");
        WorkShift juneShift = shift(reportEmployee, "2026-06-01T09:00", "2026-06-01T10:00");
        when(employeeRepository.findAll()).thenReturn(List.of(reportEmployee));
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of(mayShift, juneShift));

        var row = workShiftService.getMonthlyReport(2026, 5).getFirst();

        assertEquals(60, row.getTotalWorkedMinutes());
        assertEquals(1, row.getCompletedShifts());
        verify(workShiftRepository).findCompletedShiftsOverlapping(
                localInstant("2026-05-01T00:00"),
                localInstant("2026-06-01T00:00"));
    }

    @Test
    void monthlyReportUsesBusinessZoneBoundariesAcrossDstChange() {
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(workShiftRepository.findCompletedShiftsOverlapping(any(), any()))
                .thenReturn(List.of());

        workShiftService.getMonthlyReport(2026, 10);

        verify(workShiftRepository).findCompletedShiftsOverlapping(
                Instant.parse("2026-09-30T21:00:00Z"),
                Instant.parse("2026-10-31T22:00:00Z"));
    }

    private Employee employee(String firstName, String lastName, Long id) {
        Employee result = new Employee();
        result.setId(id);
        result.setFirstName(firstName);
        result.setLastName(lastName);
        return result;
    }

    private WorkShift shift(Employee owner, String start, String end) {
        return new WorkShift(owner, localInstant(start), localInstant(end));
    }

    private Instant localInstant(String value) {
        return LocalDateTime.parse(value).atZone(ZoneId.of("Europe/Athens")).toInstant();
    }
}
