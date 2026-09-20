package com.philippos.employeemanagement.mapper;

import com.philippos.employeemanagement.dto.response.TaskAttachmentResponse;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskAttachment;
import com.philippos.employeemanagement.entity.TaskAttachmentType;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.service.TaskAttachmentStorageService.StoredFile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TaskAttachmentMapper {

    public TaskAttachment toEntity(Task task, User uploader, TaskAttachmentType attachmentType,
                                   StoredFile storedFile, Instant uploadedAt) {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setTask(task);
        attachment.setUploader(uploader);
        attachment.setAttachmentType(attachmentType);
        attachment.setOriginalFilename(storedFile.originalFilename());
        attachment.setStoredFilename(storedFile.storedFilename());
        attachment.setContentType(storedFile.contentType());
        attachment.setFileSize(storedFile.size());
        attachment.setUploadedAt(uploadedAt);
        return attachment;
    }

    public TaskAttachmentResponse toResponse(TaskAttachment attachment) {
        TaskAttachmentResponse response = new TaskAttachmentResponse();
        response.setId(attachment.getId());
        response.setAttachmentType(attachment.getAttachmentType());
        response.setOriginalFilename(attachment.getOriginalFilename());
        response.setContentType(attachment.getContentType());
        response.setFileSize(attachment.getFileSize());
        response.setUploadedAt(attachment.getUploadedAt());
        response.setUploaderUserId(attachment.getUploader().getId());
        response.setUploaderUsername(attachment.getUploader().getUsername());
        return response;
    }
}
