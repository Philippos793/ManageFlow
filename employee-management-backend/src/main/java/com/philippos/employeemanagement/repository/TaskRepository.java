package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByArchivedFalse();

    List<Task> findAllByAssignedEmployee_IdAndArchivedFalse(Long employeeId);

    boolean existsByAssignedEmployee_IdAndArchivedFalseAndStatusIn(
            Long employeeId, Collection<TaskStatus> statuses);
}
