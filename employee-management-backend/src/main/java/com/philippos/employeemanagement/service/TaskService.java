package com.philippos.employeemanagement.service;

import com.philippos.employeemanagement.dto.request.TaskCreateRequest;
import com.philippos.employeemanagement.dto.request.TaskEditRequest;
import com.philippos.employeemanagement.dto.request.TaskChecklistItemEditRequest;
import com.philippos.employeemanagement.dto.request.TaskChecklistItemUpdateRequest;
import com.philippos.employeemanagement.dto.request.TaskProgressRequest;
import com.philippos.employeemanagement.dto.request.TaskRequestChangesRequest;
import com.philippos.employeemanagement.dto.response.TaskResponse;
import com.philippos.employeemanagement.entity.Employee;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.NotificationType;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.Task;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskChecklistItemRepository taskChecklistItemRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final TaskChecklistItemMapper taskChecklistItemMapper;
    private final NotificationService notificationService;
    private final TaskActivityService taskActivityService;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository,
                       TaskChecklistItemRepository taskChecklistItemRepository,
                       EmployeeRepository employeeRepository,
                       UserRepository userRepository,
                       TaskMapper taskMapper,
                       TaskChecklistItemMapper taskChecklistItemMapper,
                       NotificationService notificationService,
                       TaskActivityService taskActivityService,
                       Clock clock) {
        this.taskRepository = taskRepository;
        this.taskChecklistItemRepository = taskChecklistItemRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
        this.taskChecklistItemMapper = taskChecklistItemMapper;
        this.notificationService = notificationService;
        this.taskActivityService = taskActivityService;
        this.clock = clock;
    }

    public TaskResponse createTask(TaskCreateRequest request) {
        User admin = getAuthenticatedAdmin();
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee with id " + request.getEmployeeId() + " not found"));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeConflictException(
                    "Task can only be assigned to an active employee");
        }

        User employeeUser = userRepository.findByEmployee_Id(employee.getId())
                .orElseThrow(() -> new EmployeeConflictException(
                        "Task cannot be assigned because the employee does not have a linked account"));
        if (employeeUser.getRole() != Role.EMPLOYEE) {
            throw new EmployeeConflictException(
                    "Task can only be assigned to an employee account");
        }

        Task task = taskMapper.toEntity(request, employee, admin, clock.instant());
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);
        task.setAcceptedAt(null);
        task.setDeadline(null);
        task.setCompletedAt(null);
        task.setArchived(false);
        Task savedTask = taskRepository.save(task);
        List<TaskChecklistItem> checklistItems = request.getChecklistItems() == null
                ? List.of()
                : request.getChecklistItems().stream()
                .map(itemRequest -> taskChecklistItemMapper.toEntity(
                        itemRequest, savedTask, clock.instant()))
                .toList();
        if (!checklistItems.isEmpty()) {
            savedTask.setChecklistItems(taskChecklistItemRepository.saveAll(checklistItems));
        }
        notificationService.create(employeeUser, savedTask, NotificationType.TASK_ASSIGNED);
        taskActivityService.record(savedTask, admin, TaskActivityType.TASK_CREATED);
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse updateTask(Long id, TaskEditRequest request) {
        User admin = getAuthenticatedAdmin();
        Task task = findTaskCreatedBy(id, admin);
        requireNotArchived(task);
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.DECLINED) {
            throw new TaskConflictException("Completed and declined tasks cannot be edited");
        }

        String title = request.getTitle().trim();
        boolean taskChanged = !task.getTitle().equals(title)
                || !Objects.equals(task.getDescription(), request.getDescription())
                || !Objects.equals(task.getTimeAllowedMinutes(), request.getTimeAllowedMinutes());
        boolean timeAllowedChanged = !Objects.equals(
                task.getTimeAllowedMinutes(), request.getTimeAllowedMinutes());

        task.setTitle(title);
        task.setDescription(request.getDescription());
        task.setTimeAllowedMinutes(request.getTimeAllowedMinutes());
        if (timeAllowedChanged && task.getStatus() == TaskStatus.IN_PROGRESS) {
            if (task.getAcceptedAt() == null) {
                throw new TaskConflictException("In-progress task is missing its acceptance time");
            }
            task.setDeadline(task.getAcceptedAt().plus(
                    request.getTimeAllowedMinutes(), ChronoUnit.MINUTES));
        }

        if (request.getChecklistItems() != null) {
            taskChanged |= updateChecklist(task, request.getChecklistItems());
        }

        Task savedTask = taskRepository.save(task);
        if (taskChanged) {
            User employeeUser = userRepository.findByEmployee_Id(
                            savedTask.getAssignedEmployee().getId())
                    .orElseThrow(() -> new EmployeeConflictException(
                            "Task cannot be updated because the assigned employee does not have a linked account"));
            notificationService.create(employeeUser, savedTask, NotificationType.TASK_UPDATED);
            taskActivityService.record(savedTask, admin, TaskActivityType.TASK_UPDATED);
        }
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse archiveTask(Long id) {
        User admin = getAuthenticatedAdmin();
        Task task = findTaskCreatedBy(id, admin);
        if (task.isArchived()) {
            throw new TaskConflictException("Task is already archived");
        }

        task.setArchived(true);
        Task savedTask = taskRepository.save(task);
        taskActivityService.record(savedTask, admin, TaskActivityType.TASK_ARCHIVED);
        return taskMapper.toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks() {
        User user = getAuthenticatedUser();
        List<Task> tasks;
        if (user.getRole() == Role.ADMIN) {
            tasks = taskRepository.findAllByArchivedFalse();
        } else {
            tasks = taskRepository.findAllByAssignedEmployee_IdAndArchivedFalse(
                    requireLinkedEmployee(user).getId());
        }
        return tasks.stream().map(taskMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id) {
        User user = getAuthenticatedUser();
        return taskMapper.toResponse(findAccessibleTask(id, user));
    }

    public TaskResponse acceptTask(Long id) {
        User user = getAuthenticatedEmployeeUser();
        Task task = findAccessibleTask(id, user);
        requireNotArchived(task);
        requirePending(task);

        var acceptedAt = clock.instant();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setAcceptedAt(acceptedAt);
        task.setDeadline(acceptedAt.plus(
                task.getTimeAllowedMinutes(), ChronoUnit.MINUTES));
        Task savedTask = taskRepository.save(task);
        notificationService.create(task.getCreatedByUser(), savedTask, NotificationType.TASK_ACCEPTED);
        taskActivityService.record(savedTask, user, TaskActivityType.TASK_ACCEPTED);
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse declineTask(Long id) {
        User user = getAuthenticatedEmployeeUser();
        Task task = findAccessibleTask(id, user);
        requireNotArchived(task);
        requirePending(task);

        task.setStatus(TaskStatus.DECLINED);
        task.setAcceptedAt(null);
        task.setDeadline(null);
        Task savedTask = taskRepository.save(task);
        notificationService.create(task.getCreatedByUser(), savedTask, NotificationType.TASK_DECLINED);
        taskActivityService.record(savedTask, user, TaskActivityType.TASK_DECLINED);
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse requestChanges(Long id, TaskRequestChangesRequest request) {
        User admin = getAuthenticatedAdmin();
        Task task = findTaskCreatedBy(id, admin);
        requireNotArchived(task);
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new TaskConflictException("Changes can only be requested for completed tasks");
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setCompletedAt(null);
        task.setDeadline(clock.instant().plus(request.getAdditionalTimeMinutes(), ChronoUnit.MINUTES));
        if (task.getChecklistItems().isEmpty()) {
            task.setProgress(0);
        } else {
            List<com.philippos.employeemanagement.dto.request.TaskChecklistItemRequest>
                    requestedChecklistItems = request.getChecklistItems();
            if (requestedChecklistItems == null || requestedChecklistItems.isEmpty()) {
                throw new TaskConflictException(
                        "At least one new incomplete checklist item is required when requesting changes");
            }
            List<TaskChecklistItem> newChecklistItems = requestedChecklistItems.stream()
                    .map(itemRequest -> taskChecklistItemMapper.toEntity(itemRequest, task, clock.instant()))
                    .toList();
            List<TaskChecklistItem> savedNewChecklistItems =
                    taskChecklistItemRepository.saveAll(newChecklistItems);
            List<TaskChecklistItem> resultingChecklistItems = new ArrayList<>(task.getChecklistItems());
            resultingChecklistItems.addAll(savedNewChecklistItems);
            task.setChecklistItems(resultingChecklistItems);
            recalculateChecklistProgress(task, resultingChecklistItems);
        }
        Task savedTask = taskRepository.save(task);
        User employeeUser = userRepository.findByEmployee_Id(savedTask.getAssignedEmployee().getId())
                .orElseThrow(() -> new EmployeeConflictException(
                        "Task changes cannot be requested because the assigned employee does not have a linked account"));
        notificationService.create(employeeUser, savedTask, NotificationType.TASK_CHANGES_REQUESTED);
        taskActivityService.record(savedTask, admin, TaskActivityType.TASK_CHANGES_REQUESTED,
                "Changes requested: " + request.getFeedback().trim());
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse updateProgress(Long id, TaskProgressRequest request) {
        User user = getAuthenticatedEmployeeUser();
        Task task = findAccessibleTask(id, user);
        requireNotArchived(task);
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new TaskConflictException("Only in-progress tasks can have their progress updated");
        }
        if (!task.getChecklistItems().isEmpty()) {
            throw new TaskConflictException(
                    "Task progress is calculated from its checklist items");
        }
        if (request.getProgress() < task.getProgress()) {
            throw new TaskConflictException("Task progress cannot be decreased");
        }

        task.setProgress(request.getProgress());
        if (request.getProgress() == 100) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(clock.instant());
        }
        Task savedTask = taskRepository.save(task);
        taskActivityService.record(savedTask, user, TaskActivityType.PROGRESS_UPDATED);
        if (savedTask.getStatus() == TaskStatus.COMPLETED) {
            notificationService.create(task.getCreatedByUser(), savedTask,
                    NotificationType.TASK_COMPLETED);
            taskActivityService.record(savedTask, user, TaskActivityType.TASK_COMPLETED);
        }
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse updateChecklistItem(Long taskId, Long itemId,
                                            TaskChecklistItemUpdateRequest request) {
        User user = getAuthenticatedEmployeeUser();
        Task task = findAccessibleTask(taskId, user);
        requireNotArchived(task);
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new TaskConflictException(
                    "Checklist items can only be updated for in-progress tasks");
        }

        TaskChecklistItem item = taskChecklistItemRepository.findByIdAndTask_Id(itemId, taskId)
                .orElseThrow(() -> new TaskNotFoundException(
                        "Checklist item with id " + itemId + " not found"));
        item.setCompleted(request.getCompleted());
        item.setCompletedAt(request.getCompleted() ? clock.instant() : null);
        taskChecklistItemRepository.save(item);

        List<TaskChecklistItem> checklistItems = taskChecklistItemRepository.findAllByTask_Id(taskId);
        task.setChecklistItems(checklistItems);
        long completedItems = checklistItems.stream()
                .filter(TaskChecklistItem::isCompleted)
                .count();
        int progress = (int) (completedItems * 100 / checklistItems.size());
        task.setProgress(progress);
        if (completedItems == checklistItems.size()) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(clock.instant());
        }
        Task savedTask = taskRepository.save(task);
        taskActivityService.record(savedTask, user, TaskActivityType.CHECKLIST_UPDATED);
        if (savedTask.getStatus() == TaskStatus.COMPLETED) {
            notificationService.create(task.getCreatedByUser(), savedTask,
                    NotificationType.TASK_COMPLETED);
            taskActivityService.record(savedTask, user, TaskActivityType.TASK_COMPLETED);
        }
        return taskMapper.toResponse(savedTask);
    }

    private User getAuthenticatedAdmin() {
        User user = getAuthenticatedUser();
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Authenticated administrator is required");
        }
        return user;
    }

    private User getAuthenticatedEmployeeUser() {
        User user = getAuthenticatedUser();
        if (user.getRole() != Role.EMPLOYEE) {
            throw new IllegalStateException("Authenticated employee is required");
        }
        requireLinkedEmployee(user);
        return user;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user was not found"));
    }

    private Employee requireLinkedEmployee(User user) {
        if (user.getEmployee() == null) {
            throw new EmployeeNotFoundException(
                    "Authenticated employee does not have a linked employee record");
        }
        return user.getEmployee();
    }

    private Task findAccessibleTask(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> taskNotFound(id));
        if (user.getRole() == Role.EMPLOYEE
                && !task.getAssignedEmployee().getId()
                .equals(requireLinkedEmployee(user).getId())) {
            throw taskNotFound(id);
        }
        return task;
    }

    private Task findTaskCreatedBy(Long id, User admin) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> taskNotFound(id));
        if (!task.getCreatedByUser().getId().equals(admin.getId())) {
            throw taskNotFound(id);
        }
        return task;
    }

    private boolean updateChecklist(Task task, List<TaskChecklistItemEditRequest> requests) {
        List<TaskChecklistItem> existingItems = taskChecklistItemRepository.findAllByTask_Id(task.getId());
        boolean checklistChanged = !existingItems.isEmpty() || !requests.isEmpty();
        Map<Long, TaskChecklistItem> itemsById = new HashMap<>();
        for (TaskChecklistItem item : existingItems) {
            itemsById.put(item.getId(), item);
        }

        Set<Long> requestedExistingIds = new HashSet<>();
        for (TaskChecklistItemEditRequest request : requests) {
            if (request.getId() == null) continue;
            if (!itemsById.containsKey(request.getId())) {
                throw new TaskNotFoundException("Checklist item with id " + request.getId() + " not found");
            }
            if (!requestedExistingIds.add(request.getId())) {
                throw new TaskConflictException("Checklist item ids must not be duplicated");
            }
        }

        List<TaskChecklistItem> itemsToDelete = existingItems.stream()
                .filter(item -> !requestedExistingIds.contains(item.getId()))
                .toList();
        if (itemsToDelete.stream().anyMatch(TaskChecklistItem::isCompleted)) {
            throw new TaskConflictException("Completed checklist items cannot be removed");
        }

        boolean descriptionChanged = false;
        for (TaskChecklistItemEditRequest request : requests) {
            if (request.getId() != null) {
                TaskChecklistItem item = itemsById.get(request.getId());
                String description = request.getDescription().trim();
                if (!item.getDescription().equals(description)) {
                    item.setDescription(description);
                    descriptionChanged = true;
                }
            }
        }
        if (!itemsToDelete.isEmpty()) {
            taskChecklistItemRepository.deleteAll(itemsToDelete);
        }

        List<TaskChecklistItem> newItems = requests.stream()
                .filter(request -> request.getId() == null)
                .map(request -> new com.philippos.employeemanagement.dto.request.TaskChecklistItemRequest(
                        request.getDescription()))
                .map(request -> taskChecklistItemMapper.toEntity(request, task, clock.instant()))
                .toList();
        List<TaskChecklistItem> savedNewItems = newItems.isEmpty()
                ? List.of()
                : taskChecklistItemRepository.saveAll(newItems);

        List<TaskChecklistItem> resultingItems = new ArrayList<>();
        existingItems.stream()
                .filter(item -> !itemsToDelete.contains(item))
                .forEach(resultingItems::add);
        resultingItems.addAll(savedNewItems);
        task.setChecklistItems(resultingItems);
        if (checklistChanged) {
            recalculateChecklistProgress(task, resultingItems);
        }
        return descriptionChanged || !itemsToDelete.isEmpty() || !savedNewItems.isEmpty();
    }

    private void recalculateChecklistProgress(Task task, List<TaskChecklistItem> checklistItems) {
        if (checklistItems.isEmpty()) {
            task.setProgress(0);
            return;
        }

        long completedItems = checklistItems.stream()
                .filter(TaskChecklistItem::isCompleted)
                .count();
        task.setProgress((int) (completedItems * 100 / checklistItems.size()));
        if (completedItems == checklistItems.size()) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(clock.instant());
        }
    }

    private void requirePending(Task task) {
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new TaskConflictException(
                    "Only pending tasks can be accepted or declined");
        }
    }

    private TaskNotFoundException taskNotFound(Long id) {
        return new TaskNotFoundException("Task with id " + id + " not found");
    }

    private void requireNotArchived(Task task) {
        if (task.isArchived()) {
            throw new TaskConflictException("Archived tasks cannot be modified");
        }
    }
}
