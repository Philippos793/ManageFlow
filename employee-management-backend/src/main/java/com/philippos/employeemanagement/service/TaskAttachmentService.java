package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.TaskAttachmentResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskActivityType;
import com.philippos.employeemanagement.entity.TaskAttachment;
import com.philippos.employeemanagement.entity.TaskAttachmentType;
import com.philippos.employeemanagement.entity.TaskStatus;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.exception.TaskAttachmentNotFoundException;
import com.philippos.employeemanagement.exception.TaskConflictException;
import com.philippos.employeemanagement.exception.TaskNotFoundException;
import com.philippos.employeemanagement.mapper.TaskAttachmentMapper;
import com.philippos.employeemanagement.repository.TaskAttachmentRepository;
import com.philippos.employeemanagement.repository.TaskRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.util.List;

@Service
@Transactional
public class TaskAttachmentService {
    private final TaskRepository taskRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final UserRepository userRepository;
    private final TaskAttachmentMapper taskAttachmentMapper;
    private final TaskAttachmentStorageService storageService;
    private final TaskActivityService taskActivityService;
    private final Clock clock;

    public TaskAttachmentService(TaskRepository taskRepository,
                                 TaskAttachmentRepository taskAttachmentRepository,
                                 UserRepository userRepository,
                                 TaskAttachmentMapper taskAttachmentMapper,
                                 TaskAttachmentStorageService storageService,
                                 TaskActivityService taskActivityService,
                                 Clock clock) {
        this.taskRepository = taskRepository;
        this.taskAttachmentRepository = taskAttachmentRepository;
        this.userRepository = userRepository;
        this.taskAttachmentMapper = taskAttachmentMapper;
        this.storageService = storageService;
        this.taskActivityService = taskActivityService;
        this.clock = clock;
    }

    public TaskAttachmentResponse upload(Long taskId, MultipartFile file) {
        User uploader = getAuthenticatedUser();
        Task task = findAccessibleTask(taskId, uploader);
        if (task.isArchived()) {
            throw new TaskConflictException("Archived tasks cannot accept attachments");
        }
        TaskAttachmentType attachmentType = resolveUploadType(task, uploader);

        TaskAttachmentStorageService.StoredFile storedFile = storageService.store(file);
        try {
            TaskAttachment attachment = taskAttachmentMapper.toEntity(
                    task, uploader, attachmentType, storedFile, clock.instant());
            TaskAttachment savedAttachment = taskAttachmentRepository.save(attachment);
            taskActivityService.record(task, uploader, TaskActivityType.ATTACHMENT_UPLOADED);
            return taskAttachmentMapper.toResponse(savedAttachment);
        } catch (RuntimeException exception) {
            storageService.deleteQuietly(storedFile.storedFilename());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> getAttachments(Long taskId) {
        User user = getAuthenticatedUser();
        findAccessibleTask(taskId, user);
        return taskAttachmentRepository.findAllByTask_IdOrderByUploadedAtDesc(taskId).stream()
                .map(taskAttachmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Long taskId, Long attachmentId) {
        User user = getAuthenticatedUser();
        findAccessibleTask(taskId, user);
        TaskAttachment attachment = taskAttachmentRepository.findByIdAndTask_Id(attachmentId, taskId)
                .orElseThrow(() -> new TaskAttachmentNotFoundException(
                        "Attachment with id " + attachmentId + " not found"));
        Resource resource = storageService.load(attachment.getStoredFilename());
        return new AttachmentDownload(attachment, resource);
    }

    private TaskAttachmentType resolveUploadType(Task task, User uploader) {
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new TaskConflictException(
                    "Attachments can only be uploaded to in-progress tasks");
        }
        if (uploader.getRole() == Role.ADMIN) {
            return TaskAttachmentType.ADMIN_RESOURCE;
        }
        return TaskAttachmentType.EMPLOYEE_SUBMISSION;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }

    private Task findAccessibleTask(Long taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " not found"));
        if (user.getRole() == Role.ADMIN
                && !task.getCreatedByUser().getId().equals(user.getId())) {
            throw taskNotFound(taskId);
        }
        if (user.getRole() == Role.EMPLOYEE
                && !task.getAssignedEmployee().getId().equals(requireLinkedEmployee(user).getId())) {
            throw taskNotFound(taskId);
        }
        return task;
    }

    private Employee requireLinkedEmployee(User user) {
        if (user.getEmployee() == null) {
            throw new EmployeeNotFoundException(
                    "Authenticated employee does not have a linked employee record");
        }
        return user.getEmployee();
    }

    private TaskNotFoundException taskNotFound(Long taskId) {
        return new TaskNotFoundException("Task with id " + taskId + " not found");
    }

    public record AttachmentDownload(TaskAttachment attachment, Resource resource) {
    }
}
