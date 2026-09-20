package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.controller.RegistrationRequestController;
import com.philippos.employeemanagement.dto.response.RegistrationRequestResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.RegistrationStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.RegistrationRequestConflictException;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.JwtService;
import com.philippos.employeemanagement.service.RegistrationRequestService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationRequestController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class RegistrationRequestSecurityTest {
    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter securityFilter;
    @MockitoBean private RegistrationRequestService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    @Test
    void publicCanSubmitRequestWithoutCreatingLoginResponseData() throws Exception {
        when(service.submit(any())).thenReturn(response());
        mockMvc.perform(post("/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void invalidSubmissionReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"\",\"email\":\"bad\",\"username\":\"\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(service, never()).submit(any());
    }

    @Test
    void duplicatePendingSubmissionReturnsConflict() throws Exception {
        when(service.submit(any())).thenThrow(
                new RegistrationRequestConflictException("A pending request already exists for this username"));
        mockMvc.perform(post("/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void adminCanGetPendingRequestsWithoutPasswordData() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(service.getPending()).thenReturn(List.of(response()));
        mockMvc.perform(get("/registration-requests/pending")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alex"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void employeeCannotGetPendingRequests() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(get("/registration-requests/pending")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).getPending();
    }

    @Test
    void adminCanApproveRegistrationRequest() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(service.approve(any(), any())).thenReturn(response());
        mockMvc.perform(post("/registration-requests/1/approve")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanRejectRegistrationRequest() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(service.reject(1L)).thenReturn(response());
        mockMvc.perform(post("/registration-requests/1/reject")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotApproveRegistrationRequest() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(post("/registration-requests/1/approve")
                        .header("Authorization", "Bearer employee-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":3}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).approve(any(), any());
    }

    @Test
    void employeeCannotRejectRegistrationRequest() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(post("/registration-requests/1/reject")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).reject(any());
    }
    private RegistrationRequestResponse response() {
        return new RegistrationRequestResponse(1L, "Alex", "Smith", "alex@example.com",
                "alex", RegistrationStatus.PENDING, LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    private String validJson() {
        return "{\"firstName\":\"Alex\",\"lastName\":\"Smith\",\"email\":\"alex@example.com\"," +
                "\"username\":\"alex\",\"password\":\"temporary-password\"}";
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
