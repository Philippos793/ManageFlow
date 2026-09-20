package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.exception.TaskAttachmentValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskAttachmentStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesFileWithGeneratedSafeReference() throws Exception {
        TaskAttachmentStorageService storageService =
                new TaskAttachmentStorageService(temporaryDirectory.toString(), 10L);

        TaskAttachmentStorageService.StoredFile stored = storageService.store(
                new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[] {1, 2, 3}));

        assertEquals("report.pdf", stored.originalFilename());
        assertEquals(3L, stored.size());
        assertTrue(Files.exists(temporaryDirectory.resolve(stored.storedFilename())));
        assertTrue(storageService.load(stored.storedFilename()).exists());
    }

    @Test
    void rejectsPathTraversalAndFilesOverConfiguredLimit() {
        TaskAttachmentStorageService storageService =
                new TaskAttachmentStorageService(temporaryDirectory.toString(), 3L);

        assertThrows(TaskAttachmentValidationException.class, () -> storageService.store(
                new MockMultipartFile("file", "../secret.txt", "text/plain", new byte[] {1})));
        assertThrows(TaskAttachmentValidationException.class, () -> storageService.store(
                new MockMultipartFile("file", "large.txt", "text/plain", new byte[] {1, 2, 3, 4})));
        assertThrows(TaskAttachmentValidationException.class,
                () -> storageService.load("../outside-file"));
    }
}
