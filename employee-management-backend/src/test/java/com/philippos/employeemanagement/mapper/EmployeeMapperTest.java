package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.request.EmployeeRequest;
import com.philippos.employeemanagement.dto.response.EmployeeResponse;
import com.philippos.employeemanagement.entity.Department;
import com.philippos.employeemanagement.entity.Employee;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EmployeeMapperTest {

    private final EmployeeMapper employeeMapper = new EmployeeMapper();

    @Test
    void toEntityMapsRequestAndDepartmentRelationship() {
        EmployeeRequest request = request("John", "IT");
        Department department = new Department("IT", "Technology");

        Employee employee = employeeMapper.toEntity(request, department);

        assertEquals("John", employee.getFirstName());
        assertEquals("Doe", employee.getLastName());
        assertEquals("john.doe@example.com", employee.getEmail());
        assertSame(department, employee.getDepartment());
    }

    @Test
    void toResponseKeepsDepartmentNameInApiContract() {
        Department department = new Department("IT", "Technology");
        Employee employee = new Employee("John", "Doe", "john.doe@example.com", department);
        employee.setId(1L);

        EmployeeResponse response = employeeMapper.toResponse(employee);

        assertEquals(1L, response.getId());
        assertEquals("IT", response.getDepartment());
    }

    @Test
    void updateEntityUpdatesDepartmentRelationship() {
        Department oldDepartment = new Department("Sales", null);
        Department newDepartment = new Department("IT", null);
        Employee employee = new Employee("Old", "Name", "old@example.com", oldDepartment);
        EmployeeRequest request = request("John", "IT");

        employeeMapper.updateEntity(employee, request, newDepartment);

        assertEquals("John", employee.getFirstName());
        assertSame(newDepartment, employee.getDepartment());
    }

    private EmployeeRequest request(String firstName, String department) {
        return new EmployeeRequest(firstName, "Doe", "john.doe@example.com", department);
    }
}
