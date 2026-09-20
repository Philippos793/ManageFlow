package com.philippos.employeemanagement.controller;

import tools.jackson.databind.ObjectMapper;
import com.philippos.employeemanagement.dto.request.EmployeeRequest;
import com.philippos.employeemanagement.dto.request.EmployeeReactivationRequest;
import com.philippos.employeemanagement.dto.response.EmployeeResponse;
import com.philippos.employeemanagement.exception.EmployeeConflictException;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.exception.DepartmentNotFoundException;
import com.philippos.employeemanagement.exception.InactiveEmployeeConflictException;
import com.philippos.employeemanagement.exception.WorkShiftConflictException;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.EmployeeService;
import com.philippos.employeemanagement.service.EmployeeInvitationService;
import com.philippos.employeemanagement.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private EmployeeInvitationService invitationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnAllEmployees() throws Exception {
        EmployeeResponse employee = createEmployeeResponse();

        when(employeeService.getAllEmployees())
                .thenReturn(List.of(employee));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"))
                .andExpect(jsonPath("$[0].department").value("IT"));
    }

    @Test
    void shouldReturnEmployeeById() throws Exception {
        EmployeeResponse employee = createEmployeeResponse();

        when(employeeService.getEmployeeById(1L))
                .thenReturn(employee);

        mockMvc.perform(get("/employees/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.department").value("IT"));
    }

    @Test
    void shouldCreateEmployee() throws Exception {
        EmployeeRequest request = createEmployeeRequest();
        EmployeeResponse response = createEmployeeResponse();

        when(employeeService.saveEmployee(any(EmployeeRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.department").value("IT"));

        verify(employeeService).saveEmployee(any(EmployeeRequest.class));
    }

    @Test
    void shouldUpdateEmployee() throws Exception {
        EmployeeRequest request = createEmployeeRequest();
        EmployeeResponse response = createEmployeeResponse();

        when(employeeService.updateEmployee(
                eq(1L),
                any(EmployeeRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/employees/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.department").value("IT"));

        verify(employeeService).updateEmployee(
                eq(1L),
                any(EmployeeRequest.class)
        );
    }

    @Test
    void duplicateEmailOnCreateReturnsConflict() throws Exception {
        when(employeeService.saveEmployee(any(EmployeeRequest.class)))
                .thenThrow(new EmployeeConflictException(
                        "An employee with this email already exists"));

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("An employee with this email already exists"));
    }

    @Test
    void inactiveDuplicateEmailReturnsReactivationDetails() throws Exception {
        when(employeeService.saveEmployee(any(EmployeeRequest.class)))
                .thenThrow(new InactiveEmployeeConflictException(42L));

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("An inactive employee with this email already exists"))
                .andExpect(jsonPath("$.code").value("INACTIVE_EMPLOYEE"))
                .andExpect(jsonPath("$.employeeId").value(42));
    }

    @Test
    void shouldReactivateEmployee() throws Exception {
        when(employeeService.reactivateEmployee(eq(1L), any(EmployeeReactivationRequest.class)))
                .thenReturn(createEmployeeResponse());

        mockMvc.perform(post("/employees/{id}/reactivate", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmployeeReactivationRequest("John", "Doe"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(employeeService).reactivateEmployee(eq(1L), any(EmployeeReactivationRequest.class));
    }

    @Test
    void duplicateEmailOnUpdateReturnsConflict() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenThrow(new EmployeeConflictException(
                        "An employee with this email already exists"));

        mockMvc.perform(put("/employees/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("An employee with this email already exists"));
    }

    @Test
    void unknownDepartmentOnCreateReturnsNotFound() throws Exception {
        when(employeeService.saveEmployee(any(EmployeeRequest.class)))
                .thenThrow(new DepartmentNotFoundException("Department with name IT not found"));

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Department with name IT not found"));
    }
    @Test
    void shouldDeleteEmployee() throws Exception {
        mockMvc.perform(delete("/employees/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(employeeService).deleteEmployee(1L);
    }

    @Test
    void shouldRejectEmailWithoutDomainExtension() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "John",
                "Doe",
                "test@gmail",
                "IT"
        );

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.email")
                        .value("Email must contain a valid domain"));

        verify(employeeService, never())
                .saveEmployee(any(EmployeeRequest.class));
    }

    @Test
    void shouldRejectEmailWithOneCharacterDomainExtension() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "John",
                "Doe",
                "test@something.c",
                "IT"
        );

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.email")
                        .value("Email must contain a valid domain"));

        verify(employeeService, never())
                .saveEmployee(any(EmployeeRequest.class));
    }

    @Test
    void shouldRejectBlankFirstName() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "",
                "Doe",
                "john.doe@example.com",
                "IT"
        );

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.firstName")
                        .value("First name is required"));

        verify(employeeService, never())
                .saveEmployee(any(EmployeeRequest.class));
    }
    @Test
    void updateMissingEmployeeReturnsNotFound() throws Exception {
        EmployeeRequest request = createEmployeeRequest();
        when(employeeService.updateEmployee(
                eq(99L),
                any(EmployeeRequest.class)
        )).thenThrow(new EmployeeNotFoundException(
                "Employee with id 99 not found"
        ));

        mockMvc.perform(put("/employees/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Employee with id 99 not found"));
    }

    @Test
    void deleteMissingEmployeeReturnsNotFound() throws Exception {
        doThrow(new EmployeeNotFoundException(
                "Employee with id 99 not found"
        )).when(employeeService).deleteEmployee(99L);

        mockMvc.perform(delete("/employees/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Employee with id 99 not found"));
    }

    @Test
    void openShiftPreventsEmployeeDeactivationWithConflict() throws Exception {
        doThrow(new WorkShiftConflictException(
                "Employee has an open shift and cannot be deactivated."))
                .when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/employees/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Employee has an open shift and cannot be deactivated."));
    }

    @Test
    void activeTasksPreventEmployeeDeactivationWithConflict() throws Exception {
        doThrow(new EmployeeConflictException(
                "Employee has active tasks and cannot be deactivated."))
                .when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/employees/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Employee has active tasks and cannot be deactivated."));
    }
    @Test
    void unexpectedErrorReturnsInternalServerError() throws Exception {
        when(employeeService.getAllEmployees())
                .thenThrow(new RuntimeException("Database unavailable"));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error")
                        .value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred"));
    }
    private EmployeeRequest createEmployeeRequest() {
        return new EmployeeRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "IT"
        );
    }

    private EmployeeResponse createEmployeeResponse() {
        return new EmployeeResponse(
                1L,
                "John",
                "Doe",
                "john.doe@example.com",
                "IT"
        );
    }
}




