package com.philippos.employeemanagement.repository;

import com.philippos.employeemanagement.entity.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
    List<TaskActivity> findAllByTask_IdOrderByCreatedAtAscIdAsc(Long taskId);
}
