package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.controller.DepartmentController;
import com.philippos.employeemanagement.dto.request.DepartmentRequest;
import com.philippos.employeemanagement.dto.response.DepartmentResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.DepartmentService;
import com.philippos.employeemanagement.service.JwtService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class DepartmentSecurityTest {

    private MockMvc mockMvc;
    @Autowired private WebApplicationContext applicationContext;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter springSecurityFilterChain;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private DepartmentService departmentService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(springSecurityFilterChain).build();
    }

    @Test
    void adminCanGetDepartments() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(departmentService.getAllDepartments()).thenReturn(List.of(response()));
        mockMvc.perform(get("/departments").header("Authorization", bearer("admin-token")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateDepartment() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(departmentService.createDepartment(any(DepartmentRequest.class))).thenReturn(response());
        mockMvc.perform(post("/departments").header("Authorization", bearer("admin-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanUpdateDepartment() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(departmentService.updateDepartment(org.mockito.ArgumentMatchers.eq(1L), any(DepartmentRequest.class)))
                .thenReturn(response());
        mockMvc.perform(put("/departments/{id}", 1L).header("Authorization", bearer("admin-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanDeleteDepartment() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        mockMvc.perform(delete("/departments/{id}", 1L).header("Authorization", bearer("admin-token")))
                .andExpect(status().isOk());
        verify(departmentService).deleteDepartment(1L);
    }

    @Test
    void employeeCanGetDepartments() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(departmentService.getAllDepartments()).thenReturn(List.of(response()));
        mockMvc.perform(get("/departments").header("Authorization", bearer("employee-token")))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotCreateDepartment() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(post("/departments").header("Authorization", bearer("employee-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(departmentService, never()).createDepartment(any(DepartmentRequest.class));
    }

    @Test
    void employeeCannotUpdateDepartment() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(put("/departments/{id}", 1L).header("Authorization", bearer("employee-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
        verify(departmentService, never()).updateDepartment(org.mockito.ArgumentMatchers.eq(1L), any(DepartmentRequest.class));
    }

    @Test
    void employeeCannotDeleteDepartment() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(delete("/departments/{id}", 1L).header("Authorization", bearer("employee-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
        verify(departmentService, never()).deleteDepartment(1L);
    }

    private void authenticate(String token, String username, Role role) {
        User user = new User(username, "encoded-password", role);
        if (role == Role.EMPLOYEE) {
            Employee employee = new Employee();
            employee.setStatus(EmployeeStatus.ACTIVE);
            user.setEmployee(employee);
        }
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));
    }

    private String bearer(String token) { return "Bearer " + token; }
    private DepartmentRequest request() { return new DepartmentRequest("IT", "Technology"); }
    private DepartmentResponse response() { return new DepartmentResponse(1L, "IT", "Technology"); }
}
