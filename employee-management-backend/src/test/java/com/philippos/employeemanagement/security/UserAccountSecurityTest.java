package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.controller.UserController;
import com.philippos.employeemanagement.dto.response.EmployeeAccountResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.EmployeeAccountConflictException;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.EmployeeAccountService;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class UserAccountSecurityTest {
    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter securityFilter;
    @MockitoBean private EmployeeAccountService employeeAccountService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    @Test
    void adminCanCreateEmployeeAccountWithoutPasswordInResponse() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(employeeAccountService.createEmployeeAccount(any()))
                .thenReturn(new EmployeeAccountResponse(10L, 7L, "employee.user", Role.EMPLOYEE));

        performRequest("admin-token", validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.employeeId").value(7))
                .andExpect(jsonPath("$.username").value("employee.user"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist());
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        performRequest("admin-token", "{\"employeeId\":null,\"username\":\"\",\"temporaryPassword\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.employeeId").value("Employee ID is required"));
        verify(employeeAccountService, never()).createEmployeeAccount(any());
    }

    @Test
    void missingEmployeeReturnsNotFound() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(employeeAccountService.createEmployeeAccount(any()))
                .thenThrow(new EmployeeNotFoundException("Employee not found"));
        performRequest("admin-token", validRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void accountConflictReturnsConflict() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(employeeAccountService.createEmployeeAccount(any()))
                .thenThrow(new EmployeeAccountConflictException("Username already exists"));
        performRequest("admin-token", validRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void employeeGetsForbidden() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        performRequest("employee-token", validRequest())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(employeeAccountService, never()).createEmployeeAccount(any());
    }

    private org.springframework.test.web.servlet.ResultActions performRequest(String token, String content)
            throws Exception {
        return mockMvc.perform(post("/users/employee-account")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));
    }

    private String validRequest() {
        return "{\"employeeId\":7,\"username\":\"employee.user\",\"temporaryPassword\":\"password\"}";
    }

    private void authenticate(String token, String username, Role role) {
        User user = new User(username, "encoded", role);
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
}
