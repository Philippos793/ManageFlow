package com.philippos.employeemanagement.dto.response;

import com.philippos.employeemanagement.entity.TaskAttachmentType;

import java.time.Instant;

public class TaskAttachmentResponse {
    private Long id;
    private TaskAttachmentType attachmentType;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Instant uploadedAt;
    private Long uploaderUserId;
    private String uploaderUsername;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TaskAttachmentType getAttachmentType() { return attachmentType; }
    public void setAttachmentType(TaskAttachmentType attachmentType) { this.attachmentType = attachmentType; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Long getUploaderUserId() { return uploaderUserId; }
    public void setUploaderUserId(Long uploaderUserId) { this.uploaderUserId = uploaderUserId; }
    public String getUploaderUsername() { return uploaderUsername; }
    public void setUploaderUsername(String uploaderUsername) { this.uploaderUsername = uploaderUsername; }
}
