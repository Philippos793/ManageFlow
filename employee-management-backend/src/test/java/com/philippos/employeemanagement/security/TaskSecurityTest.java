package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.config.SecurityConfig;
import com.philippos.employeemanagement.controller.TaskController;
import com.philippos.employeemanagement.dto.response.TaskResponse;
import com.philippos.employeemanagement.dto.response.TaskAttachmentResponse;
import com.philippos.employeemanagement.dto.response.TaskActivityResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.TaskAttachment;
import com.philippos.employeemanagement.entity.TaskAttachmentType;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.TaskStatus;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.JwtService;
import com.philippos.employeemanagement.service.TaskService;
import com.philippos.employeemanagement.service.TaskAttachmentService;
import com.philippos.employeemanagement.service.TaskActivityService;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class TaskSecurityTest {
    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter securityFilter;
    @MockitoBean private TaskService taskService;
    @MockitoBean private TaskAttachmentService taskAttachmentService;
    @MockitoBean private TaskActivityService taskActivityService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    @Test
    void adminCanAssignTask() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        TaskResponse response = new TaskResponse();
        response.setId(11L);
        response.setTitle("Prepare report");
        response.setEmployeeId(7L);
        response.setStatus(TaskStatus.PENDING);
        response.setProgress(0);
        response.setCreatedAt(Instant.parse("2026-09-16T12:00:00Z"));
        when(taskService.createTask(any())).thenReturn(response);

        perform("admin-token", validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.progress").value(0));
    }

    @Test
    void employeeCannotAssignTask() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        perform("employee-token", validRequest())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(taskService, never()).createTask(any());
    }

    @Test
    void adminCanRequestChangesForCompletedTask() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        TaskResponse response = taskResponse();
        response.setStatus(TaskStatus.IN_PROGRESS);
        when(taskService.requestChanges(any(), any())).thenReturn(response);

        requestChangesRequest("admin-token", "{\"feedback\":\"Please revise the summary.\",\"additionalTimeMinutes\":30}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void employeeCannotRequestTaskChanges() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);

        requestChangesRequest("employee-token", "{\"feedback\":\"Please revise the summary.\",\"additionalTimeMinutes\":30}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(taskService, never()).requestChanges(any(), any());
    }

    @Test
    void requestChangesRequiresFeedback() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);

        requestChangesRequest("admin-token", "{\"feedback\":\"   \",\"additionalTimeMinutes\":30}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.feedback").value("Feedback is required"));
        verify(taskService, never()).requestChanges(any(), any());
    }

    @Test
    void taskCreatorAdminCanEditTask() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(taskService.updateTask(any(), any())).thenReturn(taskResponse());

        editRequest("admin-token", validEditRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void employeeCannotEditTask() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);

        editRequest("employee-token", validEditRequest())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(taskService, never()).updateTask(any(), any());
    }

    @Test
    void adminCanArchiveTask() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(taskService.archiveTask(11L)).thenReturn(taskResponse());

        archiveRequest("admin-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void employeeCannotArchiveTask() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);

        archiveRequest("employee-token")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(taskService, never()).archiveTask(any());
    }

    @Test
    void invalidTaskEditReturnsBadRequestWithoutCallingService() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);

        editRequest("admin-token", "{\"title\":\"\",\"timeAllowedMinutes\":0,\"checklistItems\":[{\"description\":\"\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").value("Title is required"))
                .andExpect(jsonPath("$.validationErrors.timeAllowedMinutes")
                        .value("Time allowed must be greater than 0"))
                .andExpect(jsonPath("$.validationErrors['checklistItems[0].description']")
                        .value("Checklist item description is required"));
        verify(taskService, never()).updateTask(any(), any());
    }

    @Test
    void invalidInputReturnsBadRequestWithoutCallingService() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        perform("admin-token", "{\"title\":\"\",\"employeeId\":null,\"timeAllowedMinutes\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").value("Title is required"))
                .andExpect(jsonPath("$.validationErrors.employeeId").value("Employee ID is required"))
                .andExpect(jsonPath("$.validationErrors.timeAllowedMinutes")
                        .value("Time allowed must be greater than 0"));
        verify(taskService, never()).createTask(any());
    }

    @Test
    void adminCanListTasks() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        when(taskService.getTasks()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCanListAndReadTasks() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(taskService.getTasks()).thenReturn(java.util.List.of());
        when(taskService.getTask(11L)).thenReturn(taskResponse());

        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/tasks/11")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isOk());
    }

    @Test
    void taskCreatorAndAssignedEmployeeCanReadTaskActivities() throws Exception {
        TaskActivityResponse activity = new TaskActivityResponse();
        activity.setMessage("Task created.");

        authenticate("admin-token", "admin", Role.ADMIN);
        when(taskActivityService.getActivities(11L)).thenReturn(java.util.List.of(activity));
        activitiesRequest("admin-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Task created."));

        authenticate("employee-token", "employee", Role.EMPLOYEE);
        activitiesRequest("employee-token").andExpect(status().isOk());
    }

    @Test
    void employeeCanAcceptAndDeclineAssignedTask() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(taskService.acceptTask(11L)).thenReturn(taskResponse());
        when(taskService.declineTask(12L)).thenReturn(taskResponse());

        mockMvc.perform(post("/tasks/11/accept")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/tasks/12/decline")
                        .header("Authorization", "Bearer employee-token"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotAcceptOrDeclineTask() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        mockMvc.perform(post("/tasks/11/accept")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/tasks/11/decline")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
        verify(taskService, never()).acceptTask(any());
        verify(taskService, never()).declineTask(any());
    }

    @Test
    void employeeCanUpdateProgress() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(taskService.updateProgress(any(), any())).thenReturn(taskResponse());
        progressRequest("employee-token", "{\"progress\":60}")
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotUpdateProgress() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);
        progressRequest("admin-token", "{\"progress\":60}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(taskService, never()).updateProgress(any(), any());
    }

    @Test
    void invalidProgressReturns400() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        for (String body : java.util.List.of("{}", "{\"progress\":-1}", "{\"progress\":101}")) {
            progressRequest("employee-token", body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.validationErrors.progress").exists());
        }
        verify(taskService, never()).updateProgress(any(), any());
    }

    @Test
    void employeeCanUpdateChecklistItem() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(taskService.updateChecklistItem(any(), any(), any())).thenReturn(taskResponse());

        checklistRequest("employee-token", "{\"completed\":true}")
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotUpdateChecklistItem() throws Exception {
        authenticate("admin-token", "admin", Role.ADMIN);

        checklistRequest("admin-token", "{\"completed\":true}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(taskService, never()).updateChecklistItem(any(), any(), any());
    }

    @Test
    void invalidChecklistUpdateReturns400() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);

        checklistRequest("employee-token", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.completed").exists());
        verify(taskService, never()).updateChecklistItem(any(), any(), any());
    }

    @Test
    void progressConflictUsesApiErrorFormat() throws Exception {
        authenticate("employee-token", "employee", Role.EMPLOYEE);
        when(taskService.updateProgress(any(), any())).thenThrow(
                new com.philippos.employeemanagement.exception.TaskConflictException(
                        "Task progress cannot be decreased"));
        progressRequest("employee-token", "{\"progress\":10}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Task progress cannot be decreased"));
    }

    @Test
    void adminAndEmployeeCanUploadAttachments() throws Exception {
        TaskAttachmentResponse response = attachmentResponse();
        when(taskAttachmentService.upload(any(), any())).thenReturn(response);

        authenticate("admin-token", "admin", Role.ADMIN);
        uploadRequest("admin-token")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachmentType").value("ADMIN_RESOURCE"));

        authenticate("employee-token", "employee", Role.EMPLOYEE);
        uploadRequest("employee-token")
                .andExpect(status().isCreated());
    }

    @Test
    void authenticatedRolesCanDownloadAttachments() throws Exception {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setOriginalFilename("instructions.pdf");
        attachment.setContentType("application/pdf");
        attachment.setFileSize(4L);
        when(taskAttachmentService.download(any(), any())).thenReturn(
                new TaskAttachmentService.AttachmentDownload(
                        attachment, new ByteArrayResource(new byte[] {1, 2, 3, 4})));

        authenticate("admin-token", "admin", Role.ADMIN);
        downloadRequest("admin-token")
                .andExpect(status().isOk());

        authenticate("employee-token", "employee", Role.EMPLOYEE);
        downloadRequest("employee-token")
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCannotUploadOrDownloadAttachments() throws Exception {
        uploadRequest(null).andExpect(status().isUnauthorized());
        downloadRequest(null).andExpect(status().isUnauthorized());
        verify(taskAttachmentService, never()).upload(any(), any());
        verify(taskAttachmentService, never()).download(any(), any());
    }

    private org.springframework.test.web.servlet.ResultActions progressRequest(String token, String body)
            throws Exception {
        return mockMvc.perform(patch("/tasks/11/progress")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions requestChangesRequest(String token, String body)
            throws Exception {
        return mockMvc.perform(post("/tasks/11/request-changes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions editRequest(String token, String body)
            throws Exception {
        return mockMvc.perform(patch("/tasks/11")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions archiveRequest(String token)
            throws Exception {
        return mockMvc.perform(patch("/tasks/11/archive")
                .header("Authorization", "Bearer " + token));
    }

    private org.springframework.test.web.servlet.ResultActions activitiesRequest(String token)
            throws Exception {
        return mockMvc.perform(get("/tasks/11/activities")
                .header("Authorization", "Bearer " + token));
    }

    private org.springframework.test.web.servlet.ResultActions checklistRequest(String token, String body)
            throws Exception {
        return mockMvc.perform(patch("/tasks/11/checklist/21")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions uploadRequest(String token) throws Exception {
        var request = multipart("/tasks/11/attachments")
                .file("file", "content".getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private org.springframework.test.web.servlet.ResultActions downloadRequest(String token) throws Exception {
        var request = get("/tasks/11/attachments/21/download");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private org.springframework.test.web.servlet.ResultActions perform(String token, String body)
            throws Exception {
        return mockMvc.perform(post("/tasks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String validRequest() {
        return "{\"title\":\"Prepare report\",\"description\":\"Monthly report\","
                + "\"employeeId\":7,\"timeAllowedMinutes\":120}";
    }

    private String validEditRequest() {
        return "{\"title\":\"Prepare updated report\",\"description\":\"Updated details\","
                + "\"timeAllowedMinutes\":240,\"checklistItems\":[{\"description\":\"Review data\"}]}";
    }

    private TaskResponse taskResponse() {
        TaskResponse response = new TaskResponse();
        response.setId(11L);
        response.setTitle("Prepare report");
        response.setEmployeeId(7L);
        response.setStatus(TaskStatus.PENDING);
        response.setProgress(0);
        response.setCreatedAt(Instant.parse("2026-09-16T12:00:00Z"));
        return response;
    }

    private TaskAttachmentResponse attachmentResponse() {
        TaskAttachmentResponse response = new TaskAttachmentResponse();
        response.setId(21L);
        response.setAttachmentType(TaskAttachmentType.ADMIN_RESOURCE);
        response.setOriginalFilename("instructions.pdf");
        response.setContentType("application/pdf");
        response.setFileSize(4L);
        response.setUploadedAt(Instant.parse("2026-09-17T12:00:00Z"));
        response.setUploaderUserId(1L);
        response.setUploaderUsername("admin");
        return response;
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
