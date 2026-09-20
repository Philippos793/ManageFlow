package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.TaskAttachmentResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskAttachment;
import com.philippos.employeemanagement.entity.TaskActivityType;
import com.philippos.employeemanagement.entity.TaskAttachmentType;
import com.philippos.employeemanagement.entity.TaskStatus;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.TaskConflictException;
import com.philippos.employeemanagement.exception.TaskNotFoundException;
import com.philippos.employeemanagement.mapper.TaskAttachmentMapper;
import com.philippos.employeemanagement.repository.TaskAttachmentRepository;
import com.philippos.employeemanagement.repository.TaskRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    @Mock private TaskRepository taskRepository;
    @Mock private TaskAttachmentRepository taskAttachmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskAttachmentStorageService storageService;
    @Mock private TaskActivityService taskActivityService;

    private TaskAttachmentService attachmentService;
    private User admin;
    private Employee employee;
    private Task task;

    @BeforeEach
    void setUp() {
        attachmentService = new TaskAttachmentService(taskRepository, taskAttachmentRepository,
                userRepository, new TaskAttachmentMapper(), storageService,
                taskActivityService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        admin = new User("admin", "encoded", Role.ADMIN);
        admin.setId(1L);
        employee = new Employee();
        employee.setId(7L);
        employee.setFirstName("Nikos");
        employee.setLastName("Papadopoulos");
        task = task(11L, employee, admin, TaskStatus.PENDING);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void taskCreatorUploadsAdminResourceDuringAnActiveTask() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        authenticate(admin);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(storageService.store(any())).thenReturn(storedFile());
        when(taskAttachmentRepository.save(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId(21L);
            return attachment;
        });

        TaskAttachmentResponse response = attachmentService.upload(11L, file());

        ArgumentCaptor<TaskAttachment> captor = ArgumentCaptor.forClass(TaskAttachment.class);
        verify(taskAttachmentRepository).save(captor.capture());
        assertEquals(TaskAttachmentType.ADMIN_RESOURCE, captor.getValue().getAttachmentType());
        assertEquals(admin, captor.getValue().getUploader());
        assertEquals(NOW, captor.getValue().getUploadedAt());
        assertEquals(TaskAttachmentType.ADMIN_RESOURCE, response.getAttachmentType());
        assertEquals(admin.getId(), response.getUploaderUserId());
        assertEquals(admin.getUsername(), response.getUploaderUsername());
        assertEquals(NOW, response.getUploadedAt());
        verify(taskActivityService).record(eq(task), eq(admin),
                eq(TaskActivityType.ATTACHMENT_UPLOADED));
    }

    @Test
    void assignedEmployeeUploadsSubmissionOnlyToInProgressTask() {
        User employeeUser = employeeUser(employee);
        task.setStatus(TaskStatus.IN_PROGRESS);
        authenticate(employeeUser);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(storageService.store(any())).thenReturn(storedFile());
        when(taskAttachmentRepository.save(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId(21L);
            return attachment;
        });

        TaskAttachmentResponse response = attachmentService.upload(11L, file());

        assertEquals(TaskAttachmentType.EMPLOYEE_SUBMISSION, response.getAttachmentType());
        assertEquals(employeeUser.getId(), response.getUploaderUserId());
        assertEquals(employeeUser.getUsername(), response.getUploaderUsername());
        assertEquals(NOW, response.getUploadedAt());
        verify(taskActivityService).record(eq(task), eq(employeeUser),
                eq(TaskActivityType.ATTACHMENT_UPLOADED));
    }

    @Test
    void uploadsAreRejectedForBothRolesOutsideInProgressTasks() {
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        for (TaskStatus status : List.of(TaskStatus.PENDING, TaskStatus.DECLINED,
                TaskStatus.COMPLETED)) {
            task.setStatus(status);

            authenticate(admin);
            assertThrows(TaskConflictException.class,
                    () -> attachmentService.upload(11L, file()));

            authenticate(employeeUser(employee));
            assertThrows(TaskConflictException.class,
                    () -> attachmentService.upload(11L, file()));
        }

        verify(storageService, never()).store(any());
    }

    @Test
    void archivedTaskCannotAcceptNewAttachments() {
        task.setArchived(true);
        authenticate(admin);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        assertThrows(TaskConflictException.class, () -> attachmentService.upload(11L, file()));
        verify(storageService, never()).store(any());
    }

    @Test
    void otherEmployeeReceivesNotFoundWithoutAccessingStorage() {
        Employee otherEmployee = new Employee();
        otherEmployee.setId(8L);
        authenticate(employeeUser(otherEmployee));
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        assertThrows(TaskNotFoundException.class, () -> attachmentService.upload(11L, file()));
        verify(storageService, never()).store(any());
    }

    @Test
    void anotherAdminCannotManageTaskCreatedByDifferentAdmin() {
        User otherAdmin = new User("other-admin", "encoded", Role.ADMIN);
        otherAdmin.setId(2L);
        authenticate(otherAdmin);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        assertThrows(TaskNotFoundException.class, () -> attachmentService.getAttachments(11L));
        verify(taskAttachmentRepository, never()).findAllByTask_IdOrderByUploadedAtDesc(any());
    }

    @Test
    void assignedEmployeeCanListAndDownloadOwnTaskAttachments() {
        User employeeUser = employeeUser(employee);
        authenticate(employeeUser);
        TaskAttachment attachment = attachment(task, employeeUser);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskAttachmentRepository.findAllByTask_IdOrderByUploadedAtDesc(11L))
                .thenReturn(List.of(attachment));
        when(taskAttachmentRepository.findByIdAndTask_Id(21L, 11L))
                .thenReturn(Optional.of(attachment));
        when(storageService.load("stored.upload"))
                .thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));

        List<TaskAttachmentResponse> attachments = attachmentService.getAttachments(11L);
        TaskAttachmentService.AttachmentDownload download = attachmentService.download(11L, 21L);

        assertEquals(1, attachments.size());
        assertEquals("submission.pdf", attachments.get(0).getOriginalFilename());
        assertTrue(download.resource().exists());
    }

    @Test
    void taskCreatorCanListAndDownloadEmployeeSubmissions() {
        User employeeUser = employeeUser(employee);
        TaskAttachment employeeSubmission = attachment(task, employeeUser);
        task.setStatus(TaskStatus.IN_PROGRESS);
        authenticate(admin);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskAttachmentRepository.findAllByTask_IdOrderByUploadedAtDesc(11L))
                .thenReturn(List.of(employeeSubmission));
        when(taskAttachmentRepository.findByIdAndTask_Id(21L, 11L))
                .thenReturn(Optional.of(employeeSubmission));
        when(storageService.load("stored.upload"))
                .thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));

        List<TaskAttachmentResponse> attachments = attachmentService.getAttachments(11L);
        TaskAttachmentService.AttachmentDownload download = attachmentService.download(11L, 21L);

        assertEquals(1, attachments.size());
        assertEquals(TaskAttachmentType.EMPLOYEE_SUBMISSION, attachments.get(0).getAttachmentType());
        assertEquals(employeeUser.getId(), attachments.get(0).getUploaderUserId());
        assertEquals(employeeUser.getUsername(), attachments.get(0).getUploaderUsername());
        assertEquals(NOW, attachments.get(0).getUploadedAt());
        assertTrue(download.resource().exists());
    }

    @Test
    void assignedEmployeeCanListAndDownloadAdminResources() {
        User employeeUser = employeeUser(employee);
        TaskAttachment adminResource = attachment(task, admin);
        adminResource.setAttachmentType(TaskAttachmentType.ADMIN_RESOURCE);
        task.setStatus(TaskStatus.IN_PROGRESS);
        authenticate(employeeUser);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskAttachmentRepository.findAllByTask_IdOrderByUploadedAtDesc(11L))
                .thenReturn(List.of(adminResource));
        when(taskAttachmentRepository.findByIdAndTask_Id(21L, 11L))
                .thenReturn(Optional.of(adminResource));
        when(storageService.load("stored.upload"))
                .thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));

        List<TaskAttachmentResponse> attachments = attachmentService.getAttachments(11L);
        TaskAttachmentService.AttachmentDownload download = attachmentService.download(11L, 21L);

        assertEquals(1, attachments.size());
        assertEquals(TaskAttachmentType.ADMIN_RESOURCE, attachments.get(0).getAttachmentType());
        assertEquals(admin.getUsername(), attachments.get(0).getUploaderUsername());
        assertTrue(download.resource().exists());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of()));
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    private User employeeUser(Employee linkedEmployee) {
        User user = new User("employee", "encoded", Role.EMPLOYEE);
        user.setId(2L);
        user.setEmployee(linkedEmployee);
        return user;
    }

    private Task task(Long id, Employee assignedEmployee, User createdBy, TaskStatus status) {
        Task value = new Task();
        value.setId(id);
        value.setAssignedEmployee(assignedEmployee);
        value.setCreatedByUser(createdBy);
        value.setStatus(status);
        return value;
    }

    private TaskAttachment attachment(Task attachmentTask, User uploader) {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setId(21L);
        attachment.setTask(attachmentTask);
        attachment.setUploader(uploader);
        attachment.setAttachmentType(TaskAttachmentType.EMPLOYEE_SUBMISSION);
        attachment.setOriginalFilename("submission.pdf");
        attachment.setStoredFilename("stored.upload");
        attachment.setContentType("application/pdf");
        attachment.setFileSize(3L);
        attachment.setUploadedAt(NOW);
        return attachment;
    }

    private TaskAttachmentStorageService.StoredFile storedFile() {
        return new TaskAttachmentStorageService.StoredFile(
                "submission.pdf", "stored.upload", "application/pdf", 3L);
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "submission.pdf", "application/pdf", new byte[] {1, 2, 3});
    }
}
