package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.controller.EmployeeController;
import com.philippos.employeemanagement.dto.request.EmployeeRequest;
import com.philippos.employeemanagement.dto.request.EmployeeReactivationRequest;
import com.philippos.employeemanagement.dto.response.EmployeeResponse;
import com.philippos.employeemanagement.dto.response.EmployeeInvitationResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.EmployeeService;
import com.philippos.employeemanagement.service.EmployeeInvitationService;
import com.philippos.employeemanagement.service.JwtService;
import io.jsonwebtoken.JwtException;
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
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import({
        SecurityConfig.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class EmployeeSecurityTest {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String EMPLOYEE_TOKEN = "employee-token";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private EmployeeInvitationService invitationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void adminCanGetEmployees() throws Exception {
        authenticate(ADMIN_TOKEN, "admin", Role.ADMIN);
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(createEmployeeResponse()));

        mockMvc.perform(get("/employees")
                        .header("Authorization", bearer(ADMIN_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateEmployee() throws Exception {
        authenticate(ADMIN_TOKEN, "admin", Role.ADMIN);
        EmployeeRequest request = createEmployeeRequest();

        when(employeeService.saveEmployee(any(EmployeeRequest.class)))
                .thenReturn(createEmployeeResponse());

        mockMvc.perform(post("/employees")
                        .header("Authorization", bearer(ADMIN_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
                
        // To see if the service method was called with the correct parameters.
        verify(employeeService).saveEmployee(any(EmployeeRequest.class));
    }

    @Test
    void adminCanUpdateEmployee() throws Exception {
        authenticate(ADMIN_TOKEN, "admin", Role.ADMIN);
        EmployeeRequest request = createEmployeeRequest();

        when(employeeService.updateEmployee(
                eq(1L),
                any(EmployeeRequest.class)
        )).thenReturn(createEmployeeResponse());

        mockMvc.perform(put("/employees/{id}", 1L)
                        .header("Authorization", bearer(ADMIN_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(employeeService).updateEmployee(
                eq(1L),
                any(EmployeeRequest.class)
        );
    }

    @Test
    void adminCanDeleteEmployee() throws Exception {
        authenticate(ADMIN_TOKEN, "admin", Role.ADMIN);

        mockMvc.perform(delete("/employees/{id}", 1L)
                        .header("Authorization", bearer(ADMIN_TOKEN)))
                .andExpect(status().isOk());

        verify(employeeService).deleteEmployee(1L);
    }

    @Test
    void adminCanResendEmployeeInvitation() throws Exception {
        authenticate(ADMIN_TOKEN, "admin", Role.ADMIN);
        when(invitationService.resendInvitation(1L)).thenReturn(
                new EmployeeInvitationResponse(1L, "John Doe", Instant.now()));
        mockMvc.perform(post("/employees/{id}/invitation", 1L)
                        .header("Authorization", bearer(ADMIN_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1));
        verify(invitationService).resendInvitation(1L);
    }

    @Test
    void adminCanReactivateEmployee() throws Exception {
        authenticate(ADMIN_TOKEN, "admin", Role.ADMIN);
        when(employeeService.reactivateEmployee(eq(1L), any(EmployeeReactivationRequest.class)))
                .thenReturn(createEmployeeResponse());
        mockMvc.perform(post("/employees/{id}/reactivate", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmployeeReactivationRequest("John", "Doe")))
                        .header("Authorization", bearer(ADMIN_TOKEN)))
                .andExpect(status().isOk());
        verify(employeeService).reactivateEmployee(eq(1L), any(EmployeeReactivationRequest.class));
    }

    @Test
    void employeeCanGetEmployees() throws Exception {
        authenticate(EMPLOYEE_TOKEN, "employee", Role.EMPLOYEE);
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(createEmployeeResponse()));

        mockMvc.perform(get("/employees")
                        .header("Authorization", bearer(EMPLOYEE_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotCreateEmployee() throws Exception {
        authenticate(EMPLOYEE_TOKEN, "employee", Role.EMPLOYEE);

        mockMvc.perform(post("/employees")
                        .header("Authorization", bearer(EMPLOYEE_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createEmployeeRequest()
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verify(employeeService, never())
                .saveEmployee(any(EmployeeRequest.class));
    }

    @Test
    void employeeCannotUpdateEmployee() throws Exception {
        authenticate(EMPLOYEE_TOKEN, "employee", Role.EMPLOYEE);

        mockMvc.perform(put("/employees/{id}", 1L)
                        .header("Authorization", bearer(EMPLOYEE_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createEmployeeRequest()
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verify(employeeService, never())
                .updateEmployee(eq(1L), any(EmployeeRequest.class));
    }

    @Test
    void employeeCannotDeleteEmployee() throws Exception {
        authenticate(EMPLOYEE_TOKEN, "employee", Role.EMPLOYEE);

        mockMvc.perform(delete("/employees/{id}", 1L)
                        .header("Authorization", bearer(EMPLOYEE_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verify(employeeService, never()).deleteEmployee(1L);
    }

    @Test
    void employeeCannotResendEmployeeInvitation() throws Exception {
        authenticate(EMPLOYEE_TOKEN, "employee", Role.EMPLOYEE);
        mockMvc.perform(post("/employees/{id}/invitation", 1L)
                        .header("Authorization", bearer(EMPLOYEE_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(invitationService, never()).resendInvitation(1L);
    }

    @Test
    void employeeCannotReactivateEmployee() throws Exception {
        authenticate(EMPLOYEE_TOKEN, "employee", Role.EMPLOYEE);
        mockMvc.perform(post("/employees/{id}/reactivate", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EmployeeReactivationRequest("John", "Doe")))
                        .header("Authorization", bearer(EMPLOYEE_TOKEN)))
                .andExpect(status().isForbidden());
        verify(employeeService, never()).reactivateEmployee(
                eq(1L), any(EmployeeReactivationRequest.class));
    }
    @Test
    void missingJwtReturnsUnifiedUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required"));

        verify(employeeService, never()).getAllEmployees();
    }

    @Test
    void invalidJwtReturnsUnifiedUnauthorizedResponse() throws Exception {
        when(jwtService.extractUsername("invalid-token"))
                .thenThrow(new JwtException("Invalid token"));

        mockMvc.perform(get("/employees")
                        .header(
                                "Authorization",
                                bearer("invalid-token")
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or expired token"));
    }
    @Test
    void expiredJwtReturnsUnifiedUnauthorizedResponse() throws Exception {
        when(jwtService.extractUsername("expired-token"))
                .thenThrow(new JwtException("Token expired"));

        expectUnauthorizedTokenResponse("expired-token");
    }

    @Test
    void tamperedJwtReturnsUnifiedUnauthorizedResponse() throws Exception {
        when(jwtService.extractUsername("tampered-token"))
                .thenThrow(new JwtException("Invalid signature"));

        expectUnauthorizedTokenResponse("tampered-token");
    }

    @Test
    void jwtForUnknownUsernameReturnsUnifiedUnauthorizedResponse()
            throws Exception {

        when(jwtService.extractUsername("unknown-user-token"))
                .thenReturn("unknown-user");
        when(jwtService.isTokenValid("unknown-user-token"))
                .thenReturn(true);
        when(userRepository.findByUsername("unknown-user"))
                .thenReturn(Optional.empty());

        expectUnauthorizedTokenResponse("unknown-user-token");
    }

    @Test
    void jwtForInactiveEmployeeReturnsUnifiedUnauthorizedResponse()
            throws Exception {
        Employee employee = new Employee();
        employee.setStatus(EmployeeStatus.INACTIVE);
        User user = new User("inactive-employee", "encoded-password", Role.EMPLOYEE);
        user.setEmployee(employee);
        when(jwtService.extractUsername("inactive-employee-token"))
                .thenReturn("inactive-employee");
        when(jwtService.isTokenValid("inactive-employee-token"))
                .thenReturn(true);
        when(userRepository.findByUsername("inactive-employee"))
                .thenReturn(Optional.of(user));

        expectUnauthorizedTokenResponse("inactive-employee-token");
    }

    @Test
    void jwtForEmployeeWithoutLinkedEmployeeReturnsUnifiedUnauthorizedResponse()
            throws Exception {
        User user = new User("unlinked-employee", "encoded-password", Role.EMPLOYEE);
        when(jwtService.extractUsername("unlinked-employee-token"))
                .thenReturn("unlinked-employee");
        when(jwtService.isTokenValid("unlinked-employee-token"))
                .thenReturn(true);
        when(userRepository.findByUsername("unlinked-employee"))
                .thenReturn(Optional.of(user));

        expectUnauthorizedTokenResponse("unlinked-employee-token");
    }

    private void expectUnauthorizedTokenResponse(String token)
            throws Exception {

        mockMvc.perform(get("/employees")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or expired token"));

        verify(employeeService, never()).getAllEmployees();
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

    private String bearer(String token) {
        return "Bearer " + token;
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




