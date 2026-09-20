package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.DepartmentRequest;
import com.philippos.employeemanagement.dto.response.DepartmentResponse;
import com.philippos.employeemanagement.entity.Department;
import com.philippos.employeemanagement.exception.DepartmentConflictException;
import com.philippos.employeemanagement.exception.DepartmentNotFoundException;
import com.philippos.employeemanagement.mapper.DepartmentMapper;
import com.philippos.employeemanagement.repository.DepartmentRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentMapper departmentMapper;
    @InjectMocks private DepartmentService departmentService;

    @Test
    void getAllDepartmentsReturnsMappedDepartments() {
        Department department = department();
        DepartmentResponse response = response();
        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(departmentMapper.toResponse(department)).thenReturn(response);
        assertEquals(List.of(response), departmentService.getAllDepartments());
    }

    @Test
    void getDepartmentByIdReturnsMappedDepartment() {
        Department department = department();
        DepartmentResponse response = response();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toResponse(department)).thenReturn(response);
        assertSame(response, departmentService.getDepartmentById(1L));
    }

    @Test
    void getDepartmentByIdThrowsWhenMissing() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(DepartmentNotFoundException.class, () -> departmentService.getDepartmentById(99L));
    }

    @Test
    void createDepartmentTrimsAndCreatesDepartment() {
        DepartmentRequest request = new DepartmentRequest(" IT ", "Technology");
        Department department = department();
        DepartmentResponse response = response();
        when(departmentRepository.findFirstByNameIgnoreCase("IT"))
                .thenReturn(Optional.empty(), Optional.of(department));
        when(departmentRepository.insertIfAbsent("IT")).thenReturn(1);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(response);

        assertSame(response, departmentService.createDepartment(request));
        assertEquals("Technology", department.getDescription());
        verify(departmentRepository).insertIfAbsent("IT");
    }

    @Test
    void createDepartmentRejectsExistingNameIgnoringCase() {
        DepartmentRequest request = new DepartmentRequest(" it ", null);
        when(departmentRepository.findFirstByNameIgnoreCase("it")).thenReturn(Optional.of(department()));
        assertThrows(DepartmentConflictException.class, () -> departmentService.createDepartment(request));
        verify(departmentRepository, never()).insertIfAbsent(any(String.class));
    }

    @Test
    void createDepartmentRejectsConcurrentDuplicate() {
        DepartmentRequest request = new DepartmentRequest("IT", null);
        when(departmentRepository.findFirstByNameIgnoreCase("IT")).thenReturn(Optional.empty());
        when(departmentRepository.insertIfAbsent("IT")).thenReturn(0);
        assertThrows(DepartmentConflictException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    void updateDepartmentUpdatesAndMapsEntity() {
        DepartmentRequest request = new DepartmentRequest(" IT ", "Updated");
        Department department = department();
        DepartmentResponse response = response();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.findFirstByNameIgnoreCase("IT")).thenReturn(Optional.of(department));
        when(departmentRepository.saveAndFlush(department)).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(response);

        assertSame(response, departmentService.updateDepartment(1L, request));
        verify(departmentMapper).updateEntity(department, request, "IT");
    }

    @Test
    void updateDepartmentRejectsNameUsedByAnotherDepartment() {
        Department department = department();
        Department another = new Department("Sales", null);
        another.setId(2L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.findFirstByNameIgnoreCase("Sales")).thenReturn(Optional.of(another));
        assertThrows(DepartmentConflictException.class,
                () -> departmentService.updateDepartment(1L, new DepartmentRequest("Sales", null)));
    }

    @Test
    void deleteDepartmentDeletesUnusedDepartment() {
        Department department = department();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByDepartment_Id(1L)).thenReturn(false);
        departmentService.deleteDepartment(1L);
        verify(departmentRepository).delete(department);
        verify(departmentRepository).flush();
    }

    @Test
    void deleteDepartmentRejectsDepartmentWithEmployees() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department()));
        when(employeeRepository.existsByDepartment_Id(1L)).thenReturn(true);
        assertThrows(DepartmentConflictException.class, () -> departmentService.deleteDepartment(1L));
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    private Department department() {
        Department department = new Department("IT", "Technology");
        department.setId(1L);
        return department;
    }

    private DepartmentResponse response() { return new DepartmentResponse(1L, "IT", "Technology"); }
}