package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.request.DepartmentRequest;
import com.philippos.employeemanagement.dto.response.DepartmentResponse;
import com.philippos.employeemanagement.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription()
        );
    }

    public void updateEntity(
            Department department,
            DepartmentRequest request,
            String normalizedName) {

        department.setName(normalizedName);
        department.setDescription(request.getDescription());
    }
}