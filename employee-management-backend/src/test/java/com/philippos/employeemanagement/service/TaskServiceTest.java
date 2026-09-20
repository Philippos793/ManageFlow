package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.TaskCreateRequest;
import com.philippos.employeemanagement.dto.request.TaskEditRequest;
import com.philippos.employeemanagement.dto.request.TaskChecklistItemEditRequest;
import com.philippos.employeemanagement.dto.request.TaskChecklistItemRequest;
import com.philippos.employeemanagement.dto.request.TaskChecklistItemUpdateRequest;
import com.philippos.employeemanagement.dto.request.TaskProgressRequest;
import com.philippos.employeemanagement.dto.request.TaskRequestChangesRequest;
import com.philippos.employeemanagement.dto.response.TaskResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.NotificationType;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.Task;
import com.philippos.employeemanagement.entity.TaskAttachment;
import com.philippos.employeemanagement.entity.TaskActivityType;
import com.philippos.employeemanagement.entity.TaskChecklistItem;
import com.philippos.employeemanagement.entity.TaskStatus;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.exception.EmployeeConflictException;
import com.philippos.employeemanagement.exception.EmployeeNotFoundException;
import com.philippos.employeemanagement.exception.TaskConflictException;
import com.philippos.employeemanagement.exception.TaskNotFoundException;
import com.philippos.employeemanagement.mapper.TaskMapper;
import com.philippos.employeemanagement.mapper.TaskChecklistItemMapper;
import com.philippos.employeemanagement.repository.EmployeeRepository;
import com.philippos.employeemanagement.repository.TaskRepository;
import com.philippos.employeemanagement.repository.TaskChecklistItemRepository;
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
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    @Mock private TaskRepository taskRepository;
    @Mock private TaskChecklistItemRepository taskChecklistItemRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private TaskActivityService taskActivityService;

    private TaskService taskService;
    private User admin;
    private User assignedEmployeeUser;
    private Employee employee;

    @BeforeEach
    void setUp() {
        TaskChecklistItemMapper checklistItemMapper = new TaskChecklistItemMapper();
        taskService = new TaskService(taskRepository, taskChecklistItemRepository,
                employeeRepository, userRepository, new TaskMapper(checklistItemMapper),
                checklistItemMapper, notificationService, taskActivityService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        admin = new User("admin", "encoded", Role.ADMIN);
        admin.setId(1L);
        employee = new Employee();
        employee.setId(7L);
        employee.setFirstName("Nikos");
        employee.setLastName("Papadopoulos");
        employee.setStatus(EmployeeStatus.ACTIVE);
        assignedEmployeeUser = new User("nikos", "encoded", Role.EMPLOYEE);
        assignedEmployeeUser.setId(2L);
        assignedEmployeeUser.setEmployee(employee);
        lenient().when(userRepository.findByEmployee_Id(7L))
                .thenReturn(Optional.of(assignedEmployeeUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsPendingTaskForActiveEmployeeWithLinkedAccount() {
        User linkedUser = new User("nikos", "encoded", Role.EMPLOYEE);
        linkedUser.setEmployee(employee);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(userRepository.findByEmployee_Id(7L)).thenReturn(Optional.of(linkedUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        TaskResponse response = taskService.createTask(validRequest());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task task = captor.getValue();
        assertEquals("Prepare monthly report", task.getTitle());
        assertEquals(employee, task.getAssignedEmployee());
        assertEquals(admin, task.getCreatedByUser());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(0, task.getProgress());
        assertEquals(NOW, task.getCreatedAt());
        assertNull(task.getAcceptedAt());
        assertNull(task.getDeadline());
        assertNull(task.getCompletedAt());
        assertEquals(11L, response.getId());
        verify(notificationService).create(eq(linkedUser), eq(task), eq(NotificationType.TASK_ASSIGNED));
        verify(taskActivityService).record(eq(task), eq(admin), eq(TaskActivityType.TASK_CREATED));
    }

    @Test
    void missingEmployeeIsRejected() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(EmployeeNotFoundException.class,
                () -> taskService.createTask(validRequest()));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void inactiveEmployeeIsRejected() {
        employee.setStatus(EmployeeStatus.INACTIVE);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        assertThrows(EmployeeConflictException.class,
                () -> taskService.createTask(validRequest()));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void employeeWithoutLinkedAccountIsRejected() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(userRepository.findByEmployee_Id(7L)).thenReturn(Optional.empty());
        assertThrows(EmployeeConflictException.class,
                () -> taskService.createTask(validRequest()));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createsOptionalChecklistItemsWithTheTask() {
        User linkedUser = new User("nikos", "encoded", Role.EMPLOYEE);
        linkedUser.setEmployee(employee);
        TaskCreateRequest request = validRequest();
        request.setChecklistItems(List.of(
                new TaskChecklistItemRequest("Review the source data"),
                new TaskChecklistItemRequest("Share the completed report")));
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(userRepository.findByEmployee_Id(7L)).thenReturn(Optional.of(linkedUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        when(taskChecklistItemRepository.saveAll(any())).thenAnswer(invocation -> {
            List<TaskChecklistItem> items = invocation.getArgument(0);
            items.get(0).setId(21L);
            items.get(1).setId(22L);
            return items;
        });

        TaskResponse response = taskService.createTask(request);

        assertEquals(2, response.getChecklistItems().size());
        assertEquals("Review the source data", response.getChecklistItems().get(0).getDescription());
        assertEquals(false, response.getChecklistItems().get(0).isCompleted());
        verify(taskChecklistItemRepository).saveAll(any());
    }

    @Test
    void adminSeesAllTasks() {
        when(taskRepository.findAllByArchivedFalse()).thenReturn(java.util.List.of(task(11L, employee)));
        assertEquals(1, taskService.getTasks().size());
        verify(taskRepository).findAllByArchivedFalse();
    }

    @Test
    void employeeSeesOnlyOwnTasks() {
        authenticateEmployee(employee);
        when(taskRepository.findAllByAssignedEmployee_IdAndArchivedFalse(7L))
                .thenReturn(java.util.List.of(task(11L, employee)));

        assertEquals(1, taskService.getTasks().size());
        verify(taskRepository).findAllByAssignedEmployee_IdAndArchivedFalse(7L);
        verify(taskRepository, never()).findAllByArchivedFalse();
    }

    @Test
    void employeeCannotReadAnotherEmployeesTask() {
        authenticateEmployee(employee);
        Employee other = new Employee();
        other.setId(8L);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task(11L, other)));

        assertThrows(TaskNotFoundException.class, () -> taskService.getTask(11L));
    }

    @Test
    void assignedEmployeeAcceptsPendingTaskAndDeadlineIsCalculated() {
        authenticateEmployee(employee);
        Task task = task(11L, employee);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.acceptTask(11L);

        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertEquals(NOW, response.getAcceptedAt());
        assertEquals(NOW.plusSeconds(120 * 60L), response.getDeadline());
        verify(notificationService).create(eq(admin), eq(task), eq(NotificationType.TASK_ACCEPTED));
        verify(taskActivityService).record(eq(task), any(User.class),
                eq(TaskActivityType.TASK_ACCEPTED));
    }

    @Test
    void assignedEmployeeDeclinesPendingTaskWithoutAcceptanceTimestamps() {
        authenticateEmployee(employee);
        Task task = task(11L, employee);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.declineTask(11L);

        assertEquals(TaskStatus.DECLINED, response.getStatus());
        assertNull(response.getAcceptedAt());
        assertNull(response.getDeadline());
        verify(notificationService).create(eq(admin), eq(task), eq(NotificationType.TASK_DECLINED));
        verify(taskActivityService).record(eq(task), any(User.class),
                eq(TaskActivityType.TASK_DECLINED));
    }

    @Test
    void taskCreatorEditsPendingTaskDetailsAndChecklist() {
        Task task = task(11L, employee);
        TaskChecklistItem retainedItem = checklistItem(task, 21L, false);
        task.setChecklistItems(new ArrayList<>(List.of(retainedItem)));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskChecklistItemRepository.findAllByTask_Id(11L)).thenReturn(List.of(retainedItem));
        when(taskChecklistItemRepository.saveAll(any())).thenAnswer(invocation -> {
            List<TaskChecklistItem> items = invocation.getArgument(0);
            items.get(0).setId(22L);
            return items;
        });
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateTask(11L, editRequest(
                "Updated monthly report", "Updated instructions", 240,
                List.of(editItem(21L, "Review updated figures"), editItem(null, "Share the report"))));

        assertEquals("Updated monthly report", response.getTitle());
        assertEquals("Updated instructions", response.getDescription());
        assertEquals(240, response.getTimeAllowedMinutes());
        assertEquals(TaskStatus.PENDING, response.getStatus());
        assertEquals(0, response.getProgress());
        assertEquals(2, response.getChecklistItems().size());
        assertEquals("Review updated figures", retainedItem.getDescription());
        verify(taskChecklistItemRepository).saveAll(any());
        verify(notificationService).create(eq(assignedEmployeeUser), eq(task),
                eq(NotificationType.TASK_UPDATED));
        verify(taskActivityService).record(eq(task), eq(admin), eq(TaskActivityType.TASK_UPDATED));
    }

    @Test
    void editingInProgressTaskRecalculatesDeadlineAndPreservesCompletedChecklistItems() {
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setAcceptedAt(NOW.minusSeconds(120));
        task.setDeadline(NOW);
        TaskChecklistItem completedItem = checklistItem(task, 21L, true);
        completedItem.setCompletedAt(NOW.minusSeconds(60));
        task.setChecklistItems(new ArrayList<>(List.of(completedItem)));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskChecklistItemRepository.findAllByTask_Id(11L)).thenReturn(List.of(completedItem));
        when(taskChecklistItemRepository.saveAll(any())).thenAnswer(invocation -> {
            List<TaskChecklistItem> items = invocation.getArgument(0);
            items.get(0).setId(22L);
            return items;
        });
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateTask(11L, editRequest(
                "Updated report", "Updated scope", 360,
                List.of(editItem(21L, "Completed review"), editItem(null, "Add final notes"))));

        assertEquals(NOW.minusSeconds(120).plusSeconds(360 * 60L), response.getDeadline());
        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertEquals(50, response.getProgress());
        assertEquals(NOW.minusSeconds(60), completedItem.getCompletedAt());
        assertEquals(true, completedItem.isCompleted());
        assertEquals(2, response.getChecklistItems().size());
    }

    @Test
    void completedChecklistItemsCannotBeRemovedDuringEdit() {
        Task task = task(11L, employee);
        TaskChecklistItem completedItem = checklistItem(task, 21L, true);
        task.setChecklistItems(new ArrayList<>(List.of(completedItem)));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskChecklistItemRepository.findAllByTask_Id(11L)).thenReturn(List.of(completedItem));

        assertThrows(TaskConflictException.class,
                () -> taskService.updateTask(11L, editRequest("Updated report", "Updated", 240, List.of())));
        verify(taskChecklistItemRepository, never()).deleteAll(any());
        verify(taskRepository, never()).save(any());
        verify(notificationService, never()).create(any(), any(), any());
    }

    @Test
    void editingTaskWithoutChecklistKeepsItsManualProgress() {
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setAcceptedAt(NOW.minusSeconds(120));
        task.setProgress(40);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskChecklistItemRepository.findAllByTask_Id(11L)).thenReturn(List.of());
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateTask(11L,
                editRequest("Updated report", "Updated", 240, List.of()));

        assertEquals(40, response.getProgress());
        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
    }

    @Test
    void completedAndDeclinedTasksAreReadOnlyForTaskCreator() {
        Task completedTask = task(11L, employee);
        completedTask.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(completedTask));

        assertThrows(TaskConflictException.class,
                () -> taskService.updateTask(11L, editRequest("Updated", "Updated", 240, null)));

        Task declinedTask = task(12L, employee);
        declinedTask.setStatus(TaskStatus.DECLINED);
        when(taskRepository.findById(12L)).thenReturn(Optional.of(declinedTask));

        assertThrows(TaskConflictException.class,
                () -> taskService.updateTask(12L, editRequest("Updated", "Updated", 240, null)));
        verify(taskRepository, never()).save(any());
        verify(notificationService, never()).create(any(), any(), any());
    }

    @Test
    void noOpEditDoesNotCreateTaskUpdatedNotification() {
        Task task = task(11L, employee);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.updateTask(11L,
                editRequest("Prepare monthly report", null, 120, null));

        verify(notificationService, never()).create(any(), any(), any());
        verify(taskActivityService, never()).record(any(), any(), any());
    }

    @Test
    void anotherAdminCannotEditTaskCreatedBySomeoneElse() {
        User otherAdmin = new User("other-admin", "encoded", Role.ADMIN);
        otherAdmin.setId(2L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other-admin", null, List.of()));
        when(userRepository.findByUsername("other-admin")).thenReturn(Optional.of(otherAdmin));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task(11L, employee)));

        assertThrows(TaskNotFoundException.class,
                () -> taskService.updateTask(11L, editRequest("Updated", "Updated", 240, null)));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void taskCreatorCanArchiveTaskOnlyOnce() {
        Task task = task(11L, employee);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.archiveTask(11L);

        assertEquals(true, task.isArchived());
        assertEquals(true, response.isArchived());
        verify(taskRepository).save(task);
        verify(taskActivityService).record(eq(task), eq(admin), eq(TaskActivityType.TASK_ARCHIVED));

        assertThrows(TaskConflictException.class, () -> taskService.archiveTask(11L));
        verify(taskRepository).save(task);
    }

    @Test
    void anotherAdminCannotArchiveTaskCreatedBySomeoneElse() {
        User otherAdmin = new User("other-admin", "encoded", Role.ADMIN);
        otherAdmin.setId(2L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other-admin", null, List.of()));
        when(userRepository.findByUsername("other-admin")).thenReturn(Optional.of(otherAdmin));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task(11L, employee)));

        assertThrows(TaskNotFoundException.class, () -> taskService.archiveTask(11L));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void archivedTaskBlocksAllTaskMutations() {
        Task task = task(11L, employee);
        task.setArchived(true);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        assertThrows(TaskConflictException.class,
                () -> taskService.updateTask(11L, editRequest("Updated", "Updated", 120, null)));

        authenticateEmployee(employee);
        assertThrows(TaskConflictException.class, () -> taskService.acceptTask(11L));
        assertThrows(TaskConflictException.class, () -> taskService.declineTask(11L));
        assertThrows(TaskConflictException.class,
                () -> taskService.updateProgress(11L, new TaskProgressRequest(10)));
        assertThrows(TaskConflictException.class,
                () -> taskService.updateChecklistItem(11L, 21L,
                        new TaskChecklistItemUpdateRequest(true)));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void acceptedTaskCannotBeAcceptedAgain() {
        authenticateEmployee(employee);
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        assertThrows(TaskConflictException.class, () -> taskService.acceptTask(11L));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void declinedTaskCannotBeDeclinedAgain() {
        authenticateEmployee(employee);
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.DECLINED);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        assertThrows(TaskConflictException.class, () -> taskService.declineTask(11L));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void requestChangesPreservesCompletedChecklistItemsAndAddsNewIncompleteItems() {
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setCompletedAt(NOW.minusSeconds(30));
        TaskChecklistItem completedItem = checklistItem(task, 21L, true);
        task.setChecklistItems(new ArrayList<>(List.of(completedItem)));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskRequestChangesRequest request = new TaskRequestChangesRequest();
        request.setFeedback("  Please revise the summary.  ");
        request.setAdditionalTimeMinutes(90);
        request.setChecklistItems(List.of(new TaskChecklistItemRequest("Add updated figures")));
        when(taskChecklistItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TaskResponse response = taskService.requestChanges(11L, request);

        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertNull(response.getCompletedAt());
        assertEquals(NOW.plusSeconds(90 * 60L), response.getDeadline());
        assertEquals(50, response.getProgress());
        assertEquals(2, response.getChecklistItems().size());
        assertEquals(completedItem.getDescription(), response.getChecklistItems().getFirst().getDescription());
        assertEquals(true, response.getChecklistItems().getFirst().isCompleted());
        assertEquals("Add updated figures", response.getChecklistItems().get(1).getDescription());
        assertEquals(false, response.getChecklistItems().get(1).isCompleted());
        verify(notificationService).create(eq(assignedEmployeeUser), eq(task),
                eq(NotificationType.TASK_CHANGES_REQUESTED));
        verify(taskActivityService).record(eq(task), eq(admin),
                eq(TaskActivityType.TASK_CHANGES_REQUESTED),
                eq("Changes requested: Please revise the summary."));
    }

    @Test
    void requestChangesResetsManualProgressAndPreservesAttachments() {
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setCompletedAt(NOW.minusSeconds(30));
        TaskAttachment attachment = new TaskAttachment();
        attachment.setId(31L);
        task.setAttachments(new ArrayList<>(List.of(attachment)));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        TaskRequestChangesRequest request = new TaskRequestChangesRequest();
        request.setFeedback("Please revise the summary.");
        request.setAdditionalTimeMinutes(45);

        TaskResponse response = taskService.requestChanges(11L, request);

        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertEquals(0, response.getProgress());
        assertNull(response.getCompletedAt());
        assertEquals(NOW.plusSeconds(45 * 60L), response.getDeadline());
        assertEquals(List.of(attachment), task.getAttachments());
    }

    @Test
    void requestChangesWithChecklistRequiresAtLeastOneNewItem() {
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setChecklistItems(new ArrayList<>(List.of(checklistItem(task, 21L, true))));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        TaskRequestChangesRequest request = new TaskRequestChangesRequest();
        request.setFeedback("Please revise the summary.");
        request.setAdditionalTimeMinutes(30);
        request.setChecklistItems(List.of());

        assertThrows(TaskConflictException.class, () -> taskService.requestChanges(11L, request));
        verify(taskChecklistItemRepository, never()).saveAll(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void requestChangesIsRejectedForTaskThatIsNotCompleted() {
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        TaskRequestChangesRequest request = new TaskRequestChangesRequest();
        request.setFeedback("Please revise the summary.");
        request.setAdditionalTimeMinutes(30);

        assertThrows(TaskConflictException.class, () -> taskService.requestChanges(11L, request));
        verify(taskRepository, never()).save(any());
        verify(notificationService, never()).create(any(), any(), any());
    }

    @Test
    void anotherAdminCannotRequestChangesForSomeoneElsesTask() {
        User otherAdmin = new User("other-admin", "encoded", Role.ADMIN);
        otherAdmin.setId(3L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other-admin", null, List.of()));
        when(userRepository.findByUsername("other-admin")).thenReturn(Optional.of(otherAdmin));
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        TaskRequestChangesRequest request = new TaskRequestChangesRequest();
        request.setFeedback("Please revise the summary.");
        request.setAdditionalTimeMinutes(30);

        assertThrows(TaskNotFoundException.class, () -> taskService.requestChanges(11L, request));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void progressUpdateKeepsTaskInProgress() {
        Task task = prepareProgressTask();
        when(taskRepository.save(task)).thenReturn(task);
        TaskResponse response = taskService.updateProgress(11L, new TaskProgressRequest(60));
        assertEquals(60, response.getProgress());
        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertNull(response.getCompletedAt());
    }

    @Test
    void manualProgressIsRejectedWhenTaskHasChecklistItems() {
        Task task = prepareProgressTask();
        task.setChecklistItems(new ArrayList<>(List.of(checklistItem(task, 21L, false))));

        assertThrows(TaskConflictException.class,
                () -> taskService.updateProgress(11L, new TaskProgressRequest(60)));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void checklistProgressIsCalculatedFromCompletedItems() {
        Task task = prepareProgressTask();
        TaskChecklistItem firstItem = checklistItem(task, 21L, false);
        TaskChecklistItem secondItem = checklistItem(task, 22L, false);
        List<TaskChecklistItem> items = List.of(firstItem, secondItem);
        task.setChecklistItems(new ArrayList<>(items));
        when(taskChecklistItemRepository.findByIdAndTask_Id(21L, 11L))
                .thenReturn(Optional.of(firstItem));
        when(taskChecklistItemRepository.findAllByTask_Id(11L)).thenReturn(items);
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateChecklistItem(
                11L, 21L, new TaskChecklistItemUpdateRequest(true));

        assertEquals(50, response.getProgress());
        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertEquals(NOW, firstItem.getCompletedAt());
    }

    @Test
    void completingAllChecklistItemsCompletesTaskUsingClock() {
        Task task = prepareProgressTask();
        TaskChecklistItem firstItem = checklistItem(task, 21L, true);
        firstItem.setCompletedAt(NOW.minusSeconds(30));
        TaskChecklistItem secondItem = checklistItem(task, 22L, false);
        List<TaskChecklistItem> items = List.of(firstItem, secondItem);
        task.setChecklistItems(new ArrayList<>(items));
        when(taskChecklistItemRepository.findByIdAndTask_Id(22L, 11L))
                .thenReturn(Optional.of(secondItem));
        when(taskChecklistItemRepository.findAllByTask_Id(11L)).thenReturn(items);
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.updateChecklistItem(
                11L, 22L, new TaskChecklistItemUpdateRequest(true));

        assertEquals(100, response.getProgress());
        assertEquals(TaskStatus.COMPLETED, response.getStatus());
        assertEquals(NOW, response.getCompletedAt());
        verify(taskActivityService).record(eq(task), any(User.class),
                eq(TaskActivityType.CHECKLIST_UPDATED));
        verify(taskActivityService).record(eq(task), any(User.class),
                eq(TaskActivityType.TASK_COMPLETED));
        verify(notificationService).create(eq(admin), eq(task),
                eq(NotificationType.TASK_COMPLETED));
    }

    @Test
    void anotherEmployeeCannotUpdateChecklistItem() {
        authenticateEmployee(employee);
        Employee otherEmployee = new Employee();
        otherEmployee.setId(8L);
        Task task = task(11L, otherEmployee);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));

        assertThrows(TaskNotFoundException.class, () -> taskService.updateChecklistItem(
                11L, 21L, new TaskChecklistItemUpdateRequest(true)));
        verify(taskChecklistItemRepository, never()).save(any());
    }

    @Test
    void progressCannotDecrease() {
        Task task = prepareProgressTask();
        assertThrows(TaskConflictException.class,
                () -> taskService.updateProgress(11L, new TaskProgressRequest(10)));
        assertEquals(20, task.getProgress());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void progress100CompletesTaskUsingClock() {
        Task task = prepareProgressTask();
        when(taskRepository.save(task)).thenReturn(task);
        TaskResponse response = taskService.updateProgress(11L, new TaskProgressRequest(100));
        assertEquals(100, response.getProgress());
        assertEquals(TaskStatus.COMPLETED, response.getStatus());
        assertEquals(NOW, response.getCompletedAt());
        verify(taskActivityService).record(eq(task), any(User.class),
                eq(TaskActivityType.PROGRESS_UPDATED));
        verify(taskActivityService).record(eq(task), any(User.class),
                eq(TaskActivityType.TASK_COMPLETED));
        verify(notificationService).create(eq(admin), eq(task),
                eq(NotificationType.TASK_COMPLETED));
    }

    @Test
    void completedTaskCannotBeUpdated() {
        Task task = prepareProgressTask();
        task.setStatus(TaskStatus.COMPLETED);
        assertThrows(TaskConflictException.class,
                () -> taskService.updateProgress(11L, new TaskProgressRequest(100)));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void pendingTaskCannotBeUpdated() {
        Task task = prepareProgressTask();
        task.setStatus(TaskStatus.PENDING);
        assertThrows(TaskConflictException.class,
                () -> taskService.updateProgress(11L, new TaskProgressRequest(30)));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void anotherEmployeesProgressCannotBeUpdated() {
        Task task = prepareProgressTask();
        Employee other = new Employee();
        other.setId(8L);
        task.setAssignedEmployee(other);
        assertThrows(TaskNotFoundException.class,
                () -> taskService.updateProgress(11L, new TaskProgressRequest(30)));
        verify(taskRepository, never()).save(any());
    }

    private Task prepareProgressTask() {
        authenticateEmployee(employee);
        Task task = task(11L, employee);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setProgress(20);
        task.setAcceptedAt(NOW.minusSeconds(60));
        task.setDeadline(NOW.plusSeconds(7140));
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        return task;
    }

    private void authenticateEmployee(Employee linkedEmployee) {
        User employeeUser = new User("employee", "encoded", Role.EMPLOYEE);
        employeeUser.setId(2L);
        employeeUser.setEmployee(linkedEmployee);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("employee", null, List.of()));
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employeeUser));
    }

    private Task task(Long id, Employee assignedEmployee) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Prepare monthly report");
        task.setAssignedEmployee(assignedEmployee);
        task.setCreatedByUser(admin);
        task.setTimeAllowedMinutes(120);
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);
        task.setCreatedAt(NOW.minusSeconds(60));
        return task;
    }

    private TaskChecklistItem checklistItem(Task task, Long id, boolean completed) {
        TaskChecklistItem item = new TaskChecklistItem();
        item.setId(id);
        item.setTask(task);
        item.setDescription("Checklist item " + id);
        item.setCompleted(completed);
        item.setCreatedAt(NOW.minusSeconds(60));
        return item;
    }

    private TaskCreateRequest validRequest() {
        return new TaskCreateRequest("Prepare monthly report", "September report", 7L, 120);
    }

    private TaskEditRequest editRequest(String title, String description, Integer timeAllowedMinutes,
                                        List<TaskChecklistItemEditRequest> checklistItems) {
        TaskEditRequest request = new TaskEditRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setTimeAllowedMinutes(timeAllowedMinutes);
        request.setChecklistItems(checklistItems);
        return request;
    }

    private TaskChecklistItemEditRequest editItem(Long id, String description) {
        TaskChecklistItemEditRequest request = new TaskChecklistItemEditRequest();
        request.setId(id);
        request.setDescription(description);
        return request;
    }
}
