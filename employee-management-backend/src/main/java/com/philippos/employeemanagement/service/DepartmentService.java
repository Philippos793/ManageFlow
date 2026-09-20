package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.DepartmentRequest;
import com.philippos.employeemanagement.dto.response.DepartmentResponse;
import com.philippos.employeemanagement.entity.Department;
import com.philippos.employeemanagement.exception.DepartmentConflictException;
import com.philippos.employeemanagement.exception.DepartmentNotFoundException;
import com.philippos.employeemanagement.mapper.DepartmentMapper;
import com.philippos.employeemanagement.repository.DepartmentRepository;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            DepartmentMapper departmentMapper) {

        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.departmentMapper = departmentMapper;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        return departmentMapper.toResponse(findDepartment(id));
    }

    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String normalizedName = normalizeName(request.getName());

        if (departmentRepository.findFirstByNameIgnoreCase(normalizedName).isPresent()) {
            throw duplicateName(normalizedName);
        }

        if (departmentRepository.insertIfAbsent(normalizedName) == 0) {
            throw duplicateName(normalizedName);
        }

        Department department = departmentRepository
                .findFirstByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new IllegalStateException(
                        "Department could not be created"
                ));
        department.setDescription(request.getDescription());
        Department savedDepartment = departmentRepository.save(department);

        return departmentMapper.toResponse(savedDepartment);
    }

    public DepartmentResponse updateDepartment(
            Long id,
            DepartmentRequest request) {

        Department department = findDepartment(id);
        String normalizedName = normalizeName(request.getName());

        departmentRepository.findFirstByNameIgnoreCase(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw duplicateName(normalizedName);
                });

        departmentMapper.updateEntity(department, request, normalizedName);

        try {
            Department savedDepartment = departmentRepository.saveAndFlush(department);
            return departmentMapper.toResponse(savedDepartment);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName(normalizedName);
        }
    }

    public void deleteDepartment(Long id) {
        Department department = findDepartment(id);

        if (employeeRepository.existsByDepartment_Id(id)) {
            throw new DepartmentConflictException(
                    "Department cannot be deleted because it has employees"
            );
        }

        try {
            departmentRepository.delete(department);
            departmentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new DepartmentConflictException(
                    "Department cannot be deleted because it is in use"
            );
        }
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(
                        "Department with id " + id + " not found"
                ));
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private DepartmentConflictException duplicateName(String name) {
        return new DepartmentConflictException(
                "Department with name " + name + " already exists"
        );
    }
}