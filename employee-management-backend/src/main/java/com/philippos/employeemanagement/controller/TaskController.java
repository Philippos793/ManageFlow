package com.philippos.employeemanagement.controller;

import com.philippos.employeemanagement.dto.request.TaskCreateRequest;
import com.philippos.employeemanagement.dto.request.TaskEditRequest;
import com.philippos.employeemanagement.dto.request.TaskProgressRequest;
import com.philippos.employeemanagement.dto.request.TaskRequestChangesRequest;
import com.philippos.employeemanagement.dto.request.TaskChecklistItemUpdateRequest;
import com.philippos.employeemanagement.dto.response.TaskResponse;
import com.philippos.employeemanagement.dto.response.TaskAttachmentResponse;
import com.philippos.employeemanagement.dto.response.TaskActivityResponse;
import com.philippos.employeemanagement.service.TaskActivityService;
import com.philippos.employeemanagement.service.TaskAttachmentService;
import com.philippos.employeemanagement.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TaskAttachmentService taskAttachmentService;
    private final TaskActivityService taskActivityService;

    public TaskController(TaskService taskService, TaskAttachmentService taskAttachmentService,
                          TaskActivityService taskActivityService) {
        this.taskService = taskService;
        this.taskAttachmentService = taskAttachmentService;
        this.taskActivityService = taskActivityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(request);
    }

    @GetMapping
    public List<TaskResponse> getTasks() {
        return taskService.getTasks();
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @GetMapping("/{id}/activities")
    public List<TaskActivityResponse> getActivities(@PathVariable Long id) {
        return taskActivityService.getActivities(id);
    }

    @PostMapping("/{id}/accept")
    public TaskResponse acceptTask(@PathVariable Long id) {
        return taskService.acceptTask(id);
    }

    @PatchMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id,
                                   @Valid @RequestBody TaskEditRequest request) {
        return taskService.updateTask(id, request);
    }

    @PatchMapping("/{id}/archive")
    public TaskResponse archiveTask(@PathVariable Long id) {
        return taskService.archiveTask(id);
    }

    @PatchMapping("/{id}/progress")
    public TaskResponse updateProgress(@PathVariable Long id,
                                       @Valid @RequestBody TaskProgressRequest request) {
        return taskService.updateProgress(id, request);
    }

    @PatchMapping("/{taskId}/checklist/{itemId}")
    public TaskResponse updateChecklistItem(@PathVariable Long taskId,
                                            @PathVariable Long itemId,
                                            @Valid @RequestBody TaskChecklistItemUpdateRequest request) {
        return taskService.updateChecklistItem(taskId, itemId, request);
    }

    @PostMapping("/{id}/decline")
    public TaskResponse declineTask(@PathVariable Long id) {
        return taskService.declineTask(id);
    }

    @PostMapping("/{id}/request-changes")
    public TaskResponse requestChanges(@PathVariable Long id,
                                       @Valid @RequestBody TaskRequestChangesRequest request) {
        return taskService.requestChanges(id, request);
    }

    @PostMapping(path = "/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TaskAttachmentResponse uploadAttachment(@PathVariable Long taskId,
                                                   @RequestPart("file") MultipartFile file) {
        return taskAttachmentService.upload(taskId, file);
    }

    @GetMapping("/{taskId}/attachments")
    public List<TaskAttachmentResponse> getAttachments(@PathVariable Long taskId) {
        return taskAttachmentService.getAttachments(taskId);
    }

    @GetMapping("/{taskId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long taskId,
                                                        @PathVariable Long attachmentId) {
        TaskAttachmentService.AttachmentDownload download =
                taskAttachmentService.download(taskId, attachmentId);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(download.attachment().getContentType());
        } catch (IllegalArgumentException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.attachment().getOriginalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentLength(download.attachment().getFileSize())
                .body(download.resource());
    }
}
