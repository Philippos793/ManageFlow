package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    List<TaskAttachment> findAllByTask_IdOrderByUploadedAtDesc(Long taskId);

    Optional<TaskAttachment> findByIdAndTask_Id(Long attachmentId, Long taskId);
}
