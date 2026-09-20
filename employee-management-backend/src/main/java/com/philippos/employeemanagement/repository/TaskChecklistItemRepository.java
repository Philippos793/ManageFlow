package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.TaskChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {
    Optional<TaskChecklistItem> findByIdAndTask_Id(Long id, Long taskId);
    List<TaskChecklistItem> findAllByTask_Id(Long taskId);
}
