package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.exception.TaskAttachmentNotFoundException;
import com.philippos.employeemanagement.exception.TaskAttachmentValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class TaskAttachmentStorageService {
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 255;

    private final Path storageRoot;
    private final long maxFileSizeBytes;

    public TaskAttachmentStorageService(
            @Value("${app.task-attachments.storage-path:uploads/task-attachments}") String storagePath,
            @Value("${app.task-attachments.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException("Task attachment maximum size must be greater than zero");
        }
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public StoredFile store(MultipartFile file) {
        validateFile(file);
        String originalFilename = validateOriginalFilename(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());
        String storedFilename = UUID.randomUUID() + ".upload";
        Path target = resolveStoredPath(storedFilename);

        try {
            Files.createDirectories(storageRoot);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store task attachment", exception);
        }
        return new StoredFile(originalFilename, storedFilename, contentType, file.getSize());
    }

    public Resource load(String storedFilename) {
        Path storedPath = resolveStoredPath(storedFilename);
        if (!Files.isRegularFile(storedPath)) {
            throw new TaskAttachmentNotFoundException("Attachment file is not available");
        }
        return new FileSystemResource(storedPath);
    }

    public void deleteQuietly(String storedFilename) {
        try {
            Files.deleteIfExists(resolveStoredPath(storedFilename));
        } catch (IOException | TaskAttachmentValidationException ignored) {
            // A failed cleanup must not hide the original database error.
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new TaskAttachmentValidationException("Attachment file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new TaskAttachmentValidationException(
                    "Attachment exceeds the maximum allowed file size");
        }
    }

    private String validateOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()
                || originalFilename.length() > MAX_FILENAME_LENGTH
                || originalFilename.indexOf('\u0000') >= 0
                || originalFilename.contains("..")
                || originalFilename.contains("/")
                || originalFilename.contains("\\")) {
            throw new TaskAttachmentValidationException("Attachment filename is invalid");
        }
        return originalFilename.trim();
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        if (contentType.length() > MAX_CONTENT_TYPE_LENGTH
                || contentType.indexOf('\r') >= 0 || contentType.indexOf('\n') >= 0) {
            throw new TaskAttachmentValidationException("Attachment content type is invalid");
        }
        return contentType;
    }

    private Path resolveStoredPath(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()
                || storedFilename.contains("..")
                || storedFilename.contains("/")
                || storedFilename.contains("\\")) {
            throw new TaskAttachmentValidationException("Attachment storage reference is invalid");
        }
        Path resolved = storageRoot.resolve(storedFilename).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new TaskAttachmentValidationException("Attachment storage reference is invalid");
        }
        return resolved;
    }

    public record StoredFile(String originalFilename, String storedFilename,
                             String contentType, long size) {
    }
}
