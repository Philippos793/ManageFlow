package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.EmployeeRequest;
import com.philippos.employeemanagement.dto.request.EmployeeReactivationRequest;
import com.philippos.employeemanagement.dto.request.HourlyRateRequest;
import com.philippos.employeemanagement.dto.response.EmployeeResponse;
import com.philippos.employeemanagement.entity.Department;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.TaskStatus;
import com.philippos.employeemanagement.entity.WorkShift;
import com.philippos.employeemanagement.exception.EmployeeConflictException;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.exception.DepartmentNotFoundException;
import com.philippos.employeemanagement.exception.InactiveEmployeeConflictException;
import com.philippos.employeemanagement.exception.WorkShiftConflictException;
import com.philippos.employeemanagement.mapper.EmployeeMapper;
import com.philippos.employeemanagement.repository.DepartmentRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.TaskRepository;
import com.philippos.employeemanagement.repository.WorkShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private EmployeeInvitationService invitationService;

    @Mock
    private WorkShiftRepository workShiftRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void getAllEmployeesReturnsMappedEmployees() {
        Employee firstEmployee = new Employee();
        Employee secondEmployee = new Employee();
        EmployeeResponse firstResponse = createResponse(1L, "John");
        EmployeeResponse secondResponse = createResponse(2L, "Jane");

        when(employeeRepository.findAllByStatus(EmployeeStatus.ACTIVE))
                .thenReturn(List.of(firstEmployee, secondEmployee));
        when(employeeMapper.toResponse(firstEmployee)).thenReturn(firstResponse);
        when(employeeMapper.toResponse(secondEmployee)).thenReturn(secondResponse);

        List<EmployeeResponse> result = employeeService.getAllEmployees();

        assertEquals(List.of(firstResponse, secondResponse), result);
        verify(employeeRepository).findAllByStatus(EmployeeStatus.ACTIVE);
        verify(employeeMapper).toResponse(firstEmployee);
        verify(employeeMapper).toResponse(secondEmployee);
    }

    @Test
    void getEmployeeByIdReturnsMappedEmployee() {
        Employee employee = new Employee();
        EmployeeResponse response = createResponse(1L, "John");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = employeeService.getEmployeeById(1L);

        assertSame(response, result);
        verify(employeeRepository).findById(1L);
        verify(employeeMapper).toResponse(employee);
    }

    @Test
    void saveEmployeeUsesExistingDepartment() {
        EmployeeRequest request = createRequest();
        Department department = new Department("IT", null);
        Employee employee = new Employee();
        Employee savedEmployee = new Employee();
        EmployeeResponse response = createResponse(1L, "John");
        when(departmentRepository.findFirstByNameIgnoreCase("IT")).thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(request, department)).thenReturn(employee);
        when(employeeRepository.saveAndFlush(employee)).thenReturn(savedEmployee);
        when(employeeMapper.toResponse(savedEmployee)).thenReturn(response);

        EmployeeResponse result = employeeService.saveEmployee(request);

        assertSame(response, result);
        verify(departmentRepository).findFirstByNameIgnoreCase("IT");
        verify(employeeMapper).toEntity(request, department);
        verify(employeeRepository).saveAndFlush(employee);
        verify(invitationService).createInvitationForEmployee(savedEmployee);
        verify(employeeMapper).toResponse(savedEmployee);
    }

    @Test
    void saveEmployeeRejectsUnknownDepartmentWithoutCreatingIt() {
        EmployeeRequest request = createRequest();
        when(departmentRepository.findFirstByNameIgnoreCase("IT")).thenReturn(Optional.empty());

        DepartmentNotFoundException exception = assertThrows(
                DepartmentNotFoundException.class, () -> employeeService.saveEmployee(request));

        assertEquals("Department with name IT not found", exception.getMessage());
        verify(departmentRepository, never()).insertIfAbsent(any(String.class));
        verify(employeeMapper, never()).toEntity(any(), any());
        verify(employeeRepository, never()).saveAndFlush(any(Employee.class));
    }

    @Test
    void updateEmployeeRejectsUnknownDepartmentWithoutCreatingIt() {
        EmployeeRequest request = createRequest();
        Employee employee = new Employee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(departmentRepository.findFirstByNameIgnoreCase("IT")).thenReturn(Optional.empty());

        DepartmentNotFoundException exception = assertThrows(
                DepartmentNotFoundException.class, () -> employeeService.updateEmployee(1L, request));

        assertEquals("Department with name IT not found", exception.getMessage());
        verify(departmentRepository, never()).insertIfAbsent(any(String.class));
        verify(employeeMapper, never()).updateEntity(any(), any(), any());
        verify(employeeRepository, never()).saveAndFlush(any(Employee.class));
    }

    @Test
    void saveEmployeeTrimsNameAndFindsDepartmentIgnoringCase() {
        EmployeeRequest request = createRequest();
        request.setDepartment(" it ");
        Department department = new Department("IT", null);
        Employee employee = new Employee();
        Employee savedEmployee = new Employee();
        EmployeeResponse response = createResponse(1L, "John");
        when(departmentRepository.findFirstByNameIgnoreCase("it")).thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(request, department)).thenReturn(employee);
        when(employeeRepository.saveAndFlush(employee)).thenReturn(savedEmployee);
        when(employeeMapper.toResponse(savedEmployee)).thenReturn(response);

        EmployeeResponse result = employeeService.saveEmployee(request);

        assertSame(response, result);
        verify(departmentRepository).findFirstByNameIgnoreCase("it");
        verify(employeeMapper).toEntity(request, department);
    }

    @Test
    void updateEmployeeUpdatesSavesAndReturnsMappedEmployee() {
        EmployeeRequest request = createRequest();
        Department department = new Department("IT", null);
        Employee employee = new Employee();
        Employee savedEmployee = new Employee();
        EmployeeResponse response = createResponse(1L, "John");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(departmentRepository.findFirstByNameIgnoreCase("IT")).thenReturn(Optional.of(department));
        when(employeeRepository.saveAndFlush(employee)).thenReturn(savedEmployee);
        when(employeeMapper.toResponse(savedEmployee)).thenReturn(response);

        EmployeeResponse result = employeeService.updateEmployee(1L, request);

        assertSame(response, result);
        verify(employeeRepository).findById(1L);
        verify(employeeMapper).updateEntity(employee, request, department);
        verify(employeeRepository).saveAndFlush(employee);
        verify(employeeMapper).toResponse(savedEmployee);
    }

    @Test
    void saveEmployeeRejectsExistingEmailIgnoringCase() {
        EmployeeRequest request = createRequest();
        request.setEmail("JOHN.DOE@EXAMPLE.COM");
        Employee existing = new Employee();
        existing.setStatus(EmployeeStatus.ACTIVE);
        when(employeeRepository.findByEmailIgnoreCase("JOHN.DOE@EXAMPLE.COM"))
                .thenReturn(Optional.of(existing));

        EmployeeConflictException exception = assertThrows(
                EmployeeConflictException.class, () -> employeeService.saveEmployee(request));

        assertEquals("An employee with this email already exists", exception.getMessage());
    }

    @Test
    void saveEmployeeIdentifiesInactiveEmployeeForReactivation() {
        Employee existing = new Employee();
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setId(42L);
        existing.setStatus(EmployeeStatus.INACTIVE);
        when(employeeRepository.findByEmailIgnoreCase("john.doe@example.com"))
                .thenReturn(Optional.of(existing));

        InactiveEmployeeConflictException exception = assertThrows(
                InactiveEmployeeConflictException.class,
                () -> employeeService.saveEmployee(createRequest()));

        assertEquals(42L, exception.getEmployeeId());
        verify(employeeRepository, never()).saveAndFlush(any(Employee.class));
    }

    @Test
    void saveEmployeeAllowsReactivationMatchIgnoringCaseAndWhitespace() {
        Employee existing = inactiveEmployee(" John ", " DOE ");
        when(employeeRepository.findByEmailIgnoreCase("john.doe@example.com"))
                .thenReturn(Optional.of(existing));

        InactiveEmployeeConflictException exception = assertThrows(
                InactiveEmployeeConflictException.class,
                () -> employeeService.saveEmployee(createRequest()));

        assertEquals(existing.getId(), exception.getEmployeeId());
    }

    @Test
    void saveEmployeeRejectsInactiveEmployeeWithDifferentFirstName() {
        Employee existing = inactiveEmployee("Jane", "Doe");
        when(employeeRepository.findByEmailIgnoreCase("john.doe@example.com"))
                .thenReturn(Optional.of(existing));

        EmployeeConflictException exception = assertThrows(
                EmployeeConflictException.class,
                () -> employeeService.saveEmployee(createRequest()));

        assertEquals(nameMismatchMessage(), exception.getMessage());
    }

    @Test
    void saveEmployeeRejectsInactiveEmployeeWithDifferentLastName() {
        Employee existing = inactiveEmployee("John", "Smith");
        when(employeeRepository.findByEmailIgnoreCase("john.doe@example.com"))
                .thenReturn(Optional.of(existing));

        EmployeeConflictException exception = assertThrows(
                EmployeeConflictException.class,
                () -> employeeService.saveEmployee(createRequest()));

        assertEquals(nameMismatchMessage(), exception.getMessage());
    }

    @Test
    void updateEmployeeRejectsAnotherEmployeesEmailIgnoringCase() {
        EmployeeRequest request = createRequest();
        request.setEmail("JOHN.DOE@EXAMPLE.COM");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(new Employee()));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot(
                "JOHN.DOE@EXAMPLE.COM", 1L)).thenReturn(true);

        EmployeeConflictException exception = assertThrows(
                EmployeeConflictException.class,
                () -> employeeService.updateEmployee(1L, request));

        assertEquals("An employee with this email already exists", exception.getMessage());
    }

    @Test
    void saveEmployeeConvertsConcurrentEmailViolationToConflict() {
        EmployeeRequest request = createRequest();
        Department department = new Department("IT", null);
        Employee employee = new Employee();
        when(departmentRepository.findFirstByNameIgnoreCase("IT"))
                .thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(request, department)).thenReturn(employee);
        when(employeeRepository.saveAndFlush(employee))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        EmployeeConflictException exception = assertThrows(
                EmployeeConflictException.class, () -> employeeService.saveEmployee(request));

        assertEquals("An employee with this email already exists", exception.getMessage());
    }
    @Test
    void deleteEmployeeMarksEmployeeInactiveWithoutDeletingIt() {
        Employee employee = new Employee();
        employee.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(1L))
                .thenReturn(Optional.empty());
        when(taskRepository.existsByAssignedEmployee_IdAndArchivedFalseAndStatusIn(
                any(), any())).thenReturn(false);

        employeeService.deleteEmployee(1L);

        assertEquals(EmployeeStatus.INACTIVE, employee.getStatus());
        verify(workShiftRepository).findFirstByEmployeeIdAndEndTimeIsNull(1L);
        verify(taskRepository).existsByAssignedEmployee_IdAndArchivedFalseAndStatusIn(
                eq(1L), eq(Set.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)));
        verify(employeeRepository).saveAndFlush(employee);
        verify(invitationService).revokePendingInvitation(1L);
        verify(employeeRepository, never()).deleteById(1L);
    }

    @Test
    void deleteEmployeeRejectsDeactivationWhenEmployeeHasAnOpenShift() {
        Employee employee = new Employee();
        employee.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(1L))
                .thenReturn(Optional.of(new WorkShift()));

        WorkShiftConflictException exception = assertThrows(
                WorkShiftConflictException.class, () -> employeeService.deleteEmployee(1L));

        assertEquals("Employee has an open shift and cannot be deactivated.",
                exception.getMessage());
        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
        verify(employeeRepository, never()).saveAndFlush(employee);
        verify(invitationService, never()).revokePendingInvitation(1L);
    }

    @Test
    void deleteEmployeeRejectsDeactivationWhenEmployeeHasActiveTasks() {
        Employee employee = new Employee();
        employee.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(1L))
                .thenReturn(Optional.empty());
        when(taskRepository.existsByAssignedEmployee_IdAndArchivedFalseAndStatusIn(
                eq(1L), eq(Set.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS))))
                .thenReturn(true);

        EmployeeConflictException exception = assertThrows(
                EmployeeConflictException.class, () -> employeeService.deleteEmployee(1L));

        assertEquals("Employee has active tasks and cannot be deactivated.",
                exception.getMessage());
        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
        verify(employeeRepository, never()).saveAndFlush(employee);
        verify(invitationService, never()).revokePendingInvitation(1L);
    }

    @Test
    void reactivateEmployeeKeepsExistingRecordAndHistoryFields() {
        Employee employee = new Employee();
        employee.setFirstName(" John ");
        employee.setLastName(" Doe ");
        employee.setId(1L);
        employee.setStatus(EmployeeStatus.INACTIVE);
        employee.setHourlyRate(new BigDecimal("18.50"));
        EmployeeResponse response = createResponse(1L, "John");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.saveAndFlush(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = employeeService.reactivateEmployee(
                1L, new EmployeeReactivationRequest("john", " DOE "));

        assertSame(response, result);
        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
        assertEquals(new BigDecimal("18.50"), employee.getHourlyRate());
        assertEquals(" John ", employee.getFirstName());
        assertEquals(" Doe ", employee.getLastName());
        verify(invitationService).handleEmployeeReactivation(employee);
        verify(employeeRepository, never()).deleteById(1L);
    }

    @Test
    void reactivateEmployeeRejectsDirectRequestWithWrongNames() {
        Employee employee = inactiveEmployee("John", "Doe");
        when(employeeRepository.findById(42L)).thenReturn(Optional.of(employee));

        EmployeeConflictException exception = assertThrows(
                EmployeeConflictException.class,
                () -> employeeService.reactivateEmployee(
                        42L, new EmployeeReactivationRequest("Jane", "Doe")));

        assertEquals(nameMismatchMessage(), exception.getMessage());
        assertEquals(EmployeeStatus.INACTIVE, employee.getStatus());
        verify(employeeRepository, never()).saveAndFlush(any(Employee.class));
        verify(invitationService, never()).handleEmployeeReactivation(any(Employee.class));
    }

    @Test
    void updateEmployeeThrowsWhenEmployeeDoesNotExist() {
        EmployeeRequest request = createRequest();
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(99L, request));
    }

    @Test
    void deleteEmployeeThrowsWhenEmployeeDoesNotExist() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(99L));
    }

    private EmployeeRequest createRequest() {
        return new EmployeeRequest("John", "Doe", "john.doe@example.com", "IT");
    }

    private Employee inactiveEmployee(String firstName, String lastName) {
        Employee employee = new Employee();
        employee.setId(42L);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setStatus(EmployeeStatus.INACTIVE);
        return employee;
    }

    private String nameMismatchMessage() {
        return "An inactive employee with this email already exists, but the first or last name "
                + "does not match. Reactivation from Add Employee is not allowed.";
    }

    private EmployeeResponse createResponse(Long id, String firstName) {
        return new EmployeeResponse(id, firstName, "Doe", firstName.toLowerCase() + ".doe@example.com", "IT");
    }

    @Test
    void updateHourlyRateUpdatesAndReturnsRate() {
        Employee employee = new Employee();
        employee.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);

        var response = employeeService.updateHourlyRate(
                1L, new HourlyRateRequest(new BigDecimal("15.75")));

        assertEquals(new BigDecimal("15.75"), employee.getHourlyRate());
        assertEquals(new BigDecimal("15.75"), response.getHourlyRate());
    }
}
