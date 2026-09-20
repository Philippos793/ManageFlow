package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.EmployeeRequest;
import com.philippos.employeemanagement.dto.request.EmployeeReactivationRequest;
import com.philippos.employeemanagement.dto.request.HourlyRateRequest;
import com.philippos.employeemanagement.dto.response.EmployeeResponse;
import com.philippos.employeemanagement.dto.response.HourlyRateResponse;
import com.philippos.employeemanagement.entity.Department;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.TaskStatus;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeInvitationService invitationService;
    private final WorkShiftRepository workShiftRepository;
    private final TaskRepository taskRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            EmployeeMapper employeeMapper,
            EmployeeInvitationService invitationService,
            WorkShiftRepository workShiftRepository,
            TaskRepository taskRepository) {

        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.employeeMapper = employeeMapper;
        this.invitationService = invitationService;
        this.workShiftRepository = workShiftRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAllByStatus(EmployeeStatus.ACTIVE).stream()
                .map(employeeMapper::toResponse).toList();
    }

    public EmployeeResponse saveEmployee(EmployeeRequest request) {
        employeeRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .ifPresent(existing -> {
                    if (existing.getStatus() == EmployeeStatus.INACTIVE) {
                        requireMatchingNames(existing, request.getFirstName(), request.getLastName());
                        throw new InactiveEmployeeConflictException(existing.getId());
                    }
                    throw duplicateEmail();
                });

        Department department = resolveDepartment(request.getDepartment());
        Employee employee = employeeMapper.toEntity(request, department);
        try {
            Employee savedEmployee = employeeRepository.saveAndFlush(employee);
            invitationService.createInvitationForEmployee(savedEmployee);
            return employeeMapper.toResponse(savedEmployee);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmail();
        }
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee with id " + id + " not found"));
        return employeeMapper.toResponse(employee);
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee with id " + id + " not found"));
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(request.getEmail().trim(), id)) {
            throw duplicateEmail();
        }

        Department department = resolveDepartment(request.getDepartment());
        employeeMapper.updateEntity(employee, request, department);
        try {
            Employee savedEmployee = employeeRepository.saveAndFlush(employee);
            return employeeMapper.toResponse(savedEmployee);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmail();
        }
    }


    public HourlyRateResponse updateHourlyRate(Long id, HourlyRateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee with id " + id + " not found"));
        employee.setHourlyRate(request.getHourlyRate());
        Employee savedEmployee = employeeRepository.save(employee);
        return new HourlyRateResponse(savedEmployee.getId(), savedEmployee.getHourlyRate());
    }
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee with id " + id + " not found"));
        if (workShiftRepository.findFirstByEmployeeIdAndEndTimeIsNull(id).isPresent()) {
            throw new WorkShiftConflictException(
                    "Employee has an open shift and cannot be deactivated.");
        }
        if (taskRepository.existsByAssignedEmployee_IdAndArchivedFalseAndStatusIn(
                id, Set.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS))) {
            throw new EmployeeConflictException(
                    "Employee has active tasks and cannot be deactivated.");
        }
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.saveAndFlush(employee);
        invitationService.revokePendingInvitation(employee.getId());
    }

    public EmployeeResponse reactivateEmployee(Long id, EmployeeReactivationRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee with id " + id + " not found"));
        if (employee.getStatus() == EmployeeStatus.ACTIVE) {
            throw new EmployeeConflictException("Employee is already active");
        }

        requireMatchingNames(employee, request.firstName(), request.lastName());
        employee.setStatus(EmployeeStatus.ACTIVE);
        Employee savedEmployee = employeeRepository.saveAndFlush(employee);
        invitationService.handleEmployeeReactivation(savedEmployee);
        return employeeMapper.toResponse(savedEmployee);
    }

    private void requireMatchingNames(Employee employee, String firstName, String lastName) {
        if (!sameName(employee.getFirstName(), firstName)
                || !sameName(employee.getLastName(), lastName)) {
            throw new EmployeeConflictException(
                    "An inactive employee with this email already exists, but the first or last name does not match. Reactivation from Add Employee is not allowed.");
        }
    }

    private boolean sameName(String storedName, String submittedName) {
        return storedName != null && submittedName != null
                && !submittedName.trim().isEmpty()
                && storedName.trim().equalsIgnoreCase(submittedName.trim());
    }

    private EmployeeConflictException duplicateEmail() {
        return new EmployeeConflictException("An employee with this email already exists");
    }
    private Department resolveDepartment(String name) {
        String normalizedName = name.trim();

        return departmentRepository.findFirstByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new DepartmentNotFoundException(
                        "Department with name " + normalizedName + " not found"));
    }
}
