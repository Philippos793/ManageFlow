package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.controller.DashboardController;
import com.philippos.employeemanagement.dto.response.AdminDashboardSummaryResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.DashboardService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class DashboardSecurityTest {
    private MockMvc mockMvc;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilter;
    @MockitoBean DashboardService dashboardService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    @BeforeEach void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    @Test
    void adminCanGetSummary() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(dashboardService.getAdminSummary())
                .thenReturn(new AdminDashboardSummaryResponse(10, 5, 2, 10200, "170h 0m"));
        mockMvc.perform(get("/dashboard/admin-summary").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalEmployees").value(10))
                .andExpect(jsonPath("$.hoursThisMonth").value("170h 0m"));
    }

    @Test
    void employeeGetsForbidden() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(get("/dashboard/admin-summary").header("Authorization", "Bearer employee-token"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verify(dashboardService, never()).getAdminSummary();
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
