package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.response.TaskActivityResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskActivity;
import com.philippos.employeemanagement.entity.TaskActivityType;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.TaskNotFoundException;
import com.philippos.employeemanagement.mapper.TaskActivityMapper;
import com.philippos.employeemanagement.repository.TaskActivityRepository;
import com.philippos.employeemanagement.repository.TaskRepository;
import com.philippos.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskActivityServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    @Mock private TaskActivityRepository taskActivityRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;

    private TaskActivityService taskActivityService;
    private User admin;
    private User employeeUser;
    private Task task;

    @BeforeEach
    void setUp() {
        taskActivityService = new TaskActivityService(taskActivityRepository, taskRepository,
                userRepository, new TaskActivityMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        admin = new User("admin", "encoded", Role.ADMIN);
        admin.setId(1L);
        Employee employee = new Employee();
        employee.setId(7L);
        employeeUser = new User("employee", "encoded", Role.EMPLOYEE);
        employeeUser.setId(2L);
        employeeUser.setEmployee(employee);
        task = new Task();
        task.setId(11L);
        task.setCreatedByUser(admin);
        task.setAssignedEmployee(employee);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsTypedActivityWithActorAndClockTimestamp() {
        taskActivityService.record(task, admin, TaskActivityType.TASK_CREATED);

        ArgumentCaptor<TaskActivity> captor = ArgumentCaptor.forClass(TaskActivity.class);
        verify(taskActivityRepository).save(captor.capture());
        TaskActivity activity = captor.getValue();
        assertEquals(task, activity.getTask());
        assertEquals(admin, activity.getActorUser());
        assertEquals(TaskActivityType.TASK_CREATED, activity.getActivityType());
        assertEquals("Task created.", activity.getMessage());
        assertEquals(NOW, activity.getCreatedAt());
    }

    @Test
    void recordsRequestedChangesWithAdminFeedback() {
        taskActivityService.record(task, admin, TaskActivityType.TASK_CHANGES_REQUESTED,
                "Changes requested: Please revise the summary.");

        ArgumentCaptor<TaskActivity> captor = ArgumentCaptor.forClass(TaskActivity.class);
        verify(taskActivityRepository).save(captor.capture());
        assertEquals(TaskActivityType.TASK_CHANGES_REQUESTED, captor.getValue().getActivityType());
        assertEquals("Changes requested: Please revise the summary.", captor.getValue().getMessage());
    }

    @Test
    void creatorAndAssignedEmployeeReadChronologicalHistory() {
        TaskActivity first = activity(TaskActivityType.TASK_CREATED, admin, NOW.minusSeconds(60));
        TaskActivity second = activity(TaskActivityType.TASK_ACCEPTED, employeeUser, NOW);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskActivityRepository.findAllByTask_IdOrderByCreatedAtAscIdAsc(11L))
                .thenReturn(List.of(first, second));

        authenticate(admin);
        List<TaskActivityResponse> adminHistory = taskActivityService.getActivities(11L);
        assertEquals(List.of("admin", "employee"), adminHistory.stream()
                .map(TaskActivityResponse::getActorUsername).toList());
        assertEquals(List.of(TaskActivityType.TASK_CREATED, TaskActivityType.TASK_ACCEPTED),
                adminHistory.stream().map(TaskActivityResponse::getType).toList());

        authenticate(employeeUser);
        assertEquals(2, taskActivityService.getActivities(11L).size());
        verify(taskActivityRepository, times(2))
                .findAllByTask_IdOrderByCreatedAtAscIdAsc(11L);
    }

    @Test
    void otherUsersReceiveNotFoundForTaskHistory() {
        User otherAdmin = new User("other-admin", "encoded", Role.ADMIN);
        otherAdmin.setId(3L);
        Employee otherEmployee = new Employee();
        otherEmployee.setId(8L);
        User otherEmployeeUser = new User("other-employee", "encoded", Role.EMPLOYEE);
        otherEmployeeUser.setId(4L);
        otherEmployeeUser.setEmployee(otherEmployee);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        authenticate(otherAdmin);
        assertThrows(TaskNotFoundException.class, () -> taskActivityService.getActivities(11L));

        authenticate(otherEmployeeUser);
        assertThrows(TaskNotFoundException.class, () -> taskActivityService.getActivities(11L));
    }

    private TaskActivity activity(TaskActivityType type, User actor, Instant createdAt) {
        TaskActivity activity = new TaskActivity();
        activity.setTask(task);
        activity.setActorUser(actor);
        activity.setActivityType(type);
        activity.setMessage("Activity");
        activity.setCreatedAt(createdAt);
        return activity;
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of()));
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }
}
