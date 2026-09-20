package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.controller.WorkShiftController;
import com.philippos.employeemanagement.dto.response.CurrentWorkShiftResponse;
import com.philippos.employeemanagement.dto.response.WorkShiftResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.WorkShiftConflictException;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.JwtService;
import com.philippos.employeemanagement.service.WorkShiftService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkShiftController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class WorkShiftSecurityTest {
    private MockMvc mockMvc;
    @Autowired private WebApplicationContext applicationContext;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter springSecurityFilterChain;
    @MockitoBean private WorkShiftService workShiftService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(springSecurityFilterChain).build();
    }

    @Test
    void employeeCanStartShift() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(workShiftService.startShift()).thenReturn(response(null));
        mockMvc.perform(post("/work-shifts/start")
                        .header("Authorization", bearer("employee-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(10));
    }

    @Test
    void employeeCanEndShift() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(workShiftService.endShift()).thenReturn(response(Instant.now()));
        mockMvc.perform(post("/work-shifts/end")
                        .header("Authorization", bearer("employee-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTime").value(endsWith("Z")));
    }

    @Test
    void concurrentOpenShiftViolationReturnsConflictResponse() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(workShiftService.startShift()).thenThrow(
                new WorkShiftConflictException("Employee already has an open shift"));

        mockMvc.perform(post("/work-shifts/start")
                        .header("Authorization", bearer("employee-token")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Employee already has an open shift"));
    }
    @Test
    void employeeWithoutLinkedEmployeeIsRejectedBeforeWorkShiftService() throws Exception {
        User user = new User("employee", "encoded-password", Role.EMPLOYEE);
        when(jwtService.extractUsername("employee-token")).thenReturn("employee");
        when(jwtService.isTokenValid("employee-token")).thenReturn(true);
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(user));
        mockMvc.perform(post("/work-shifts/start")
                        .header("Authorization", bearer("employee-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or expired token"));
        verify(workShiftService, never()).startShift();
    }

    @Test
    void adminCannotStartShift() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        mockMvc.perform(post("/work-shifts/start")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(workShiftService, never()).startShift();
    }

    @Test
    void adminCannotEndShift() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        mockMvc.perform(post("/work-shifts/end")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(workShiftService, never()).endShift();
    }


    @Test
    void adminCanGetMonthlyReport() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(workShiftService.getMonthlyReport(2026, 5)).thenReturn(List.of());
        mockMvc.perform(get("/work-shifts/report")
                        .param("year", "2026")
                        .param("month", "5")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isOk());
        verify(workShiftService).getMonthlyReport(2026, 5);
    }

    @Test
    void employeeCannotGetMonthlyReport() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(get("/work-shifts/report")
                        .param("year", "2026")
                        .param("month", "5")
                        .header("Authorization", bearer("employee-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(workShiftService, never()).getMonthlyReport(2026, 5);
    }
    @Test
    void monthlyReportRejectsYearBefore2000() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);

        mockMvc.perform(get("/work-shifts/report")
                        .param("year", "1999")
                        .param("month", "5")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"));
        verify(workShiftService, never()).getMonthlyReport(1999, 5);
    }

    @Test
    void monthlyReportRejectsMonthAbove12() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);

        mockMvc.perform(get("/work-shifts/report")
                        .param("year", "2026")
                        .param("month", "13")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"));
        verify(workShiftService, never()).getMonthlyReport(2026, 13);
    }

    @Test
    void employeeMonthlyDetailsRejectsYearBefore2000() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);

        mockMvc.perform(get("/work-shifts/report/1")
                        .param("year", "1999")
                        .param("month", "5")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"));
        verify(workShiftService, never()).getEmployeeMonthlyDetails(1L, 1999, 5);
    }

    @Test
    void employeeMonthlyDetailsRejectsMonthBelow1() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);

        mockMvc.perform(get("/work-shifts/report/1")
                        .param("year", "2026")
                        .param("month", "0")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"));
        verify(workShiftService, never()).getEmployeeMonthlyDetails(1L, 2026, 0);
    }
    @Test
    void employeeCanGetCurrentShift() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(workShiftService.getCurrentShift())
                .thenReturn(new CurrentWorkShiftResponse(false, null));

        mockMvc.perform(get("/work-shifts/current")
                        .header("Authorization", bearer("employee-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.startTime").doesNotExist());
    }

    @Test
    void adminCannotGetCurrentShift() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);

        mockMvc.perform(get("/work-shifts/current")
                        .header("Authorization", bearer("admin-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(workShiftService, never()).getCurrentShift();
    }

    @Test
    void employeeCannotGetEmployeeMonthlyDetails() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        mockMvc.perform(get("/work-shifts/report/1")
                        .param("year", "2026").param("month", "5")
                        .header("Authorization", bearer("employee-token")))
                .andExpect(status().isForbidden());
        verify(workShiftService, never()).getEmployeeMonthlyDetails(1L, 2026, 5);
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

    private WorkShiftResponse response(Instant endTime) {
        return new WorkShiftResponse(1L, 10L, Instant.now().minusSeconds(3600), endTime);
    }
}
