package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.controller.EmployeeInvitationController;
import com.philippos.employeemanagement.dto.response.EmployeeInvitationResponse;
import com.philippos.employeemanagement.dto.response.InvitationAcceptanceResponse;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.EmployeeInvitationService;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeInvitationController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class EmployeeInvitationSecurityTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @MockitoBean
    private EmployeeInvitationService invitationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void invitationCanBeValidatedWithoutAuthentication() throws Exception {
        when(invitationService.validateInvitation("raw-token")).thenReturn(
                new EmployeeInvitationResponse(
                        1L, "John Doe", Instant.parse("2026-09-17T10:00:00Z")));
        mockMvc.perform(post("/employee-invitations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeName").value("John Doe"));
        verify(invitationService).validateInvitation("raw-token");
    }

    @Test
    void invitationCanBeAcceptedWithoutAuthentication() throws Exception {
        when(invitationService.acceptInvitation(any())).thenReturn(
                new InvitationAcceptanceResponse(
                        "Your account has been created successfully.", "john.doe"));
        mockMvc.perform(post("/employee-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\",\"username\":\"John.Doe\","
                                + "\"password\":\"secure-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john.doe"));
    }

    @Test
    void invalidPublicAcceptanceRequestReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/employee-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\",\"username\":\"x\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }
}
