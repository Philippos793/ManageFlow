package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.request.EmployeeRequest;
import com.philippos.employeemanagement.dto.response.EmployeeResponse;
import com.philippos.employeemanagement.entity.Department;
import com.philippos.employeemanagement.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeRequest request, Department department) {
        return new Employee(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                department
        );
    }

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment().getName()
        );
    }

    public void updateEntity(
            Employee employee,
            EmployeeRequest request,
            Department department) {

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(department);
    }
}
