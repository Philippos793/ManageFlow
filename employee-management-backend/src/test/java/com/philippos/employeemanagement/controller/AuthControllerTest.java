package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.dto.request.LoginRequest;
import com.philippos.employeemanagement.security.CustomAccessDeniedHandler;
import com.philippos.employeemanagement.security.CustomAuthenticationEntryPoint;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.JwtService;
import com.philippos.employeemanagement.service.UserService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({UserService.class, SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void validCredentialsReturnJwt() throws Exception {
        User user = new User("admin", "encoded-password", Role.ADMIN);
        user.setId(1L);

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "encoded-password"))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        performLogin("admin", "correct-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(jwtService).generateToken(user);
    }

    @Test
    void invalidPasswordReturnsUnauthorized() throws Exception {
        User user = new User("admin", "encoded-password", Role.ADMIN);

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        performLogin("admin", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password"));

        verify(jwtService, never()).generateToken(user);
    }

    @Test
    void unknownUsernameReturnsUnauthorized() throws Exception {
        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        performLogin("unknown", "password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password"));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void inactiveEmployeeCannotLogin() throws Exception {
        Employee employee = new Employee();
        employee.setStatus(EmployeeStatus.INACTIVE);
        User user = new User("employee", "encoded-password", Role.EMPLOYEE);
        user.setEmployee(employee);
        when(userRepository.findByUsername("employee"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "encoded-password"))
                .thenReturn(true);

        performLogin("employee", "correct-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Employee account is inactive"));

        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void employeeWithoutLinkedEmployeeCannotLogin() throws Exception {
        User user = new User("employee", "encoded-password", Role.EMPLOYEE);
        when(userRepository.findByUsername("employee"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "encoded-password"))
                .thenReturn(true);

        performLogin("employee", "correct-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Employee account is unavailable"));

        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void linkedEmployeeCanLogin() throws Exception {
        Employee employee = new Employee();
        employee.setStatus(EmployeeStatus.ACTIVE);
        User user = new User("employee", "encoded-password", Role.EMPLOYEE);
        user.setId(2L);
        user.setEmployee(employee);
        when(userRepository.findByUsername("employee"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "encoded-password"))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        performLogin("employee", "correct-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        verify(jwtService).generateToken(user);
    }

    @Test
    void loginNormalizesUsernameWithoutChangingPassword() throws Exception {
        User user = new User("admin.user", "encoded-password", Role.ADMIN);
        when(userRepository.findByUsername("admin.user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(" Password With Spaces ", "encoded-password"))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        performLogin(" ADMIN.USER ", " Password With Spaces ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin.user"));

        verify(passwordEncoder).matches(" Password With Spaces ", "encoded-password");
    }

    @Test
    void loginPasswordOver72CharactersReturnsBadRequest() throws Exception {
        performLogin("admin", "x".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password")
                        .value("Password must not exceed 72 characters"));

        verify(userRepository, never()).findByUsername(anyString());
    }
    @Test
    void publicRegistrationIsNotAvailable() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new-user\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(userRepository, never()).save(any(User.class));
    }
    @Test
    void blankLoginCredentialsReturnBadRequest() throws Exception {
        performLogin("", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.password")
                        .value("Password is required"));

        verify(userRepository, never()).findByUsername(anyString());
    }

    private ResultActions performLogin(
            String username,
            String password) throws Exception {

        LoginRequest request = new LoginRequest(username, password);

        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}









