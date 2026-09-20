package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.request.DepartmentRequest;
import com.philippos.employeemanagement.dto.response.DepartmentResponse;
import com.philippos.employeemanagement.exception.DepartmentConflictException;
import com.philippos.employeemanagement.exception.DepartmentNotFoundException;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.DepartmentService;
import com.philippos.employeemanagement.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private DepartmentService departmentService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JwtService jwtService;

    @Test
    void shouldReturnAllDepartments() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of(response()));
        mockMvc.perform(get("/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("IT"))
                .andExpect(jsonPath("$[0].description").value("Technology"));
    }

    @Test
    void shouldReturnDepartmentById() throws Exception {
        when(departmentService.getDepartmentById(1L)).thenReturn(response());
        mockMvc.perform(get("/departments/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IT"));
    }

    @Test
    void shouldCreateDepartment() throws Exception {
        when(departmentService.createDepartment(any(DepartmentRequest.class))).thenReturn(response());
        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldUpdateDepartment() throws Exception {
        when(departmentService.updateDepartment(eq(1L), any(DepartmentRequest.class))).thenReturn(response());
        mockMvc.perform(put("/departments/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IT"));
    }

    @Test
    void shouldDeleteDepartment() throws Exception {
        mockMvc.perform(delete("/departments/{id}", 1L)).andExpect(status().isOk());
        verify(departmentService).deleteDepartment(1L);
    }

    @Test
    void blankNameReturnsBadRequest() throws Exception {
        DepartmentRequest request = new DepartmentRequest("  ", "Technology");
        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").value("Department name is required"));
        verify(departmentService, never()).createDepartment(any(DepartmentRequest.class));
    }

    @Test
    void missingDepartmentReturnsNotFound() throws Exception {
        when(departmentService.getDepartmentById(99L))
                .thenThrow(new DepartmentNotFoundException("Department with id 99 not found"));
        mockMvc.perform(get("/departments/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Department with id 99 not found"));
    }

    @Test
    void duplicateDepartmentReturnsConflict() throws Exception {
        when(departmentService.createDepartment(any(DepartmentRequest.class)))
                .thenThrow(new DepartmentConflictException("Department with name IT already exists"));
        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void departmentInUseReturnsConflict() throws Exception {
        doThrow(new DepartmentConflictException("Department cannot be deleted because it has employees"))
                .when(departmentService).deleteDepartment(1L);
        mockMvc.perform(delete("/departments/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private DepartmentRequest request() { return new DepartmentRequest("IT", "Technology"); }
    private DepartmentResponse response() { return new DepartmentResponse(1L, "IT", "Technology"); }
}