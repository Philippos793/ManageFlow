import { useCallback, useEffect, useRef, useState } from "react";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import LoadingSpinner from "../components/ui/LoadingSpinner.jsx";
import PageTitle from "../components/ui/PageTitle.jsx";
import Button from "../components/ui/Button.jsx";
import ConfirmDialog from "../components/ui/ConfirmDialog.jsx";
import TaskCreateModal from "../components/TaskCreateModal.jsx";
import TaskEditModal from "../components/TaskEditModal.jsx";
import TaskRequestChangesModal from "../components/TaskRequestChangesModal.jsx";
import TaskDetailsModal from "../components/TaskDetailsModal.jsx";
import taskService from "../services/taskService.js";
import authService from "../services/authService.js";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";

const TASK_FILTERS = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Pending" },
  { value: "IN_PROGRESS", label: "In Progress" },
  { value: "OVERDUE", label: "Overdue" },
  { value: "COMPLETED", label: "Completed" },
  { value: "DECLINED", label: "Declined" },
];

function formatTimeAllowed(minutes) {
  if (!Number.isFinite(minutes)) return "Not available";
  const days = Math.floor(minutes / (24 * 60));
  const hours = Math.floor((minutes % (24 * 60)) / 60);
  const remainingMinutes = minutes % 60;
  return days > 0
    ? `${days}d ${hours}h ${remainingMinutes}m`
    : `${hours}h ${remainingMinutes}m`;
}

function formatDeadline(value) {
  if (!value) return "Not set";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Not available";
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatRemainingTime(deadline, currentTime) {
  const deadlineDate = new Date(deadline);
  if (Number.isNaN(deadlineDate.getTime())) return "Not available";

  const millisecondsRemaining = deadlineDate.getTime() - currentTime;
  if (millisecondsRemaining <= 0) return "Overdue";

  const remainingMinutes = Math.max(1, Math.ceil(millisecondsRemaining / 60000));
  const days = Math.floor(remainingMinutes / (24 * 60));
  const hours = Math.floor((remainingMinutes % (24 * 60)) / 60);
  const minutes = remainingMinutes % 60;

  if (days > 0) return `${days}d ${hours}h ${minutes}m remaining`;
  if (hours > 0) return `${hours}h ${minutes}m remaining`;
  return `${minutes}m remaining`;
}

function isOverdueTask(task, currentTime) {
  if (task.status !== "IN_PROGRESS" || !task.deadline) return false;
  const deadline = new Date(task.deadline);
  return !Number.isNaN(deadline.getTime()) && deadline.getTime() <= currentTime;
}

function matchesFilter(task, filter, currentTime) {
  if (filter === "ALL") return true;
  if (filter === "OVERDUE") return isOverdueTask(task, currentTime);
  if (filter === "IN_PROGRESS") {
    return task.status === "IN_PROGRESS" && !isOverdueTask(task, currentTime);
  }
  return task.status === filter;
}

function statusStyles(status) {
  switch (status) {
    case "IN_PROGRESS":
      return "bg-blue-100 text-blue-700";
    case "COMPLETED":
      return "bg-emerald-100 text-emerald-700";
    case "DECLINED":
      return "bg-red-100 text-red-700";
    default:
      return "bg-amber-100 text-amber-700";
  }
}

function statusLabel(status) {
  return status?.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) || "Unknown";
}

function Tasks() {
  const [tasks, setTasks] = useState([]);
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [currentTime, setCurrentTime] = useState(() => Date.now());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [loadFailed, setLoadFailed] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState(null);
  const [requestChangesTask, setRequestChangesTask] = useState(null);
  const [detailsTask, setDetailsTask] = useState(null);
  const [pendingArchiveTask, setPendingArchiveTask] = useState(null);
  const [successMessage, setSuccessMessage] = useState("");
  const [pendingTaskAction, setPendingTaskAction] = useState(null);
  const [taskActionLoading, setTaskActionLoading] = useState(false);
  const [archiveLoading, setArchiveLoading] = useState(false);
  const taskActionLockedRef = useRef(false);
  const archiveLockedRef = useRef(false);
  const tasksRequestInFlightRef = useRef(null);
  const isAdmin = authService.getRole() === "ADMIN";
  const isEmployee = authService.getRole() === "EMPLOYEE";
  const currentUserId = authService.getUserId();

  const loadTasks = useCallback((background = false) => {
    if (tasksRequestInFlightRef.current) return tasksRequestInFlightRef.current;

    if (!background) {
      setLoading(true);
      setLoadFailed(false);
      setError("");
    }

    let request;
    const clearRequestLock = () => {
      if (tasksRequestInFlightRef.current === request) {
        tasksRequestInFlightRef.current = null;
      }
    };

    request = taskService.getAllTasks()
      .then((data) => {
        clearRequestLock();
        setTasks(Array.isArray(data) ? data : []);
        setCurrentTime(Date.now());
      })
      .catch((requestError) => {
        clearRequestLock();
        const message = getFriendlyErrorMessage(requestError, "Failed to load tasks.");
        if (background) {
          setError(message);
          return;
        }
        setTasks([]);
        setLoadFailed(true);
        setError(message);
      })
      .finally(() => {
        clearRequestLock();
        if (!background) setLoading(false);
      });

    tasksRequestInFlightRef.current = request;
    return request;
  }, []);

  const handleTaskCreated = useCallback(async () => {
    setSuccessMessage("Task assigned successfully.");
    await loadTasks();
  }, [loadTasks]);

  const handleTaskUpdated = useCallback(async (updatedTask) => {
    setSuccessMessage("Task updated successfully.");
    setTasks((currentTasks) => currentTasks.map((task) => (
      task.id === updatedTask.id ? updatedTask : task
    )));
    await loadTasks(true);
  }, [loadTasks]);

  const handleChangesRequested = useCallback(async (updatedTask) => {
    setSuccessMessage("Changes requested successfully.");
    setTasks((currentTasks) => currentTasks.map((task) => (
      task.id === updatedTask.id ? updatedTask : task
    )));
    await loadTasks(true);
  }, [loadTasks]);

  const handleTaskDetailsUpdated = useCallback(async (updatedTask) => {
    if (updatedTask) {
      setDetailsTask(updatedTask);
      setTasks((currentTasks) => currentTasks.map((task) => (
        task.id === updatedTask.id ? updatedTask : task
      )));
    }
    await loadTasks(true);
  }, [loadTasks]);

  function openArchiveTask(task) {
    if (archiveLoading) return;
    setPendingArchiveTask(task);
  }

  async function handleArchiveTask() {
    if (!pendingArchiveTask || archiveLockedRef.current) return;

    archiveLockedRef.current = true;
    setArchiveLoading(true);
    setError("");
    try {
      await taskService.archiveTask(pendingArchiveTask.id);
      setPendingArchiveTask(null);
      setTasks((currentTasks) => currentTasks.filter(
        (task) => task.id !== pendingArchiveTask.id
      ));
      setSuccessMessage("Task archived successfully.");
      await loadTasks(true);
    } catch (requestError) {
      setPendingArchiveTask(null);
      setError(getFriendlyErrorMessage(requestError, "Failed to archive task."));
    } finally {
      archiveLockedRef.current = false;
      setArchiveLoading(false);
    }
  }

  function openTaskAction(task, action) {
    if (taskActionLoading) return;
    setPendingTaskAction({ task, action });
  }

  async function handleTaskAction() {
    if (!pendingTaskAction || taskActionLockedRef.current) return;

    taskActionLockedRef.current = true;
    setTaskActionLoading(true);
    setError("");
    try {
      if (pendingTaskAction.action === "accept") {
        await taskService.acceptTask(pendingTaskAction.task.id);
        setSuccessMessage("Task accepted successfully.");
      } else {
        await taskService.declineTask(pendingTaskAction.task.id);
        setSuccessMessage("Task declined successfully.");
      }
      setPendingTaskAction(null);
      await loadTasks();
    } catch (requestError) {
      setPendingTaskAction(null);
      setError(getFriendlyErrorMessage(requestError, "Failed to update task status."));
    } finally {
      taskActionLockedRef.current = false;
      setTaskActionLoading(false);
    }
  }

  useEffect(() => {
    if (!successMessage) return undefined;
    const timeoutId = window.setTimeout(() => setSuccessMessage(""), 4000);
    return () => window.clearTimeout(timeoutId);
  }, [successMessage]);

  useEffect(() => {
    const hasActiveDeadline = tasks.some(
      (task) => task.status === "IN_PROGRESS" && task.deadline
    );
    if (!hasActiveDeadline) return undefined;

    const intervalId = window.setInterval(() => setCurrentTime(Date.now()), 60000);
    return () => window.clearInterval(intervalId);
  }, [tasks]);

  useEffect(() => {
    void Promise.resolve().then(() => loadTasks());
  }, [loadTasks]);

  const filteredTasks = tasks.filter((task) => matchesFilter(task, activeFilter, currentTime));
  const activeFilterLabel = TASK_FILTERS.find((filter) => filter.value === activeFilter)?.label ?? "All";

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      void loadTasks(true);
    }, 10000);

    return () => window.clearInterval(intervalId);
  }, [loadTasks]);

  if (loading) {
    return <div className="rounded-2xl border border-slate-200 bg-white"><LoadingSpinner label="Loading tasks..." /></div>;
  }

  return (
    <section>
      <PageTitle
        eyebrow="Work management"
        title="Tasks"
        description="Track assigned work, deadlines, and progress."
        action={isAdmin ? <Button onClick={() => setCreateModalOpen(true)}>Assign Task</Button> : null}
      />

      <div className="mt-6 space-y-3">
        <FeedbackMessage type="success">{successMessage}</FeedbackMessage>
        <FeedbackMessage>{error}</FeedbackMessage>
        {loadFailed && <Button variant="secondary" onClick={() => loadTasks()}>Retry</Button>}
      </div>

      <div className="mt-6 flex flex-wrap gap-2" role="group" aria-label="Task filters">
        {TASK_FILTERS.map((filter) => (
          <button
            key={filter.value}
            type="button"
            aria-pressed={activeFilter === filter.value}
            onClick={() => setActiveFilter(filter.value)}
            className={`rounded-full border px-3.5 py-2 text-sm font-semibold transition ${activeFilter === filter.value
              ? "border-blue-600 bg-blue-600 text-white shadow-sm"
              : "border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"}`}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {!loadFailed && tasks.length === 0 && (
        <div className="mt-8 rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center shadow-sm">
          <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-slate-100 text-xl">0</div>
          <p className="mt-4 font-semibold text-slate-900">No tasks yet</p>
          <p className="mt-1 text-sm text-slate-500">Tasks assigned to you will appear here.</p>
        </div>
      )}

      {!loadFailed && tasks.length > 0 && filteredTasks.length === 0 && (
        <div className="mt-8 rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center shadow-sm">
          <p className="font-semibold text-slate-900">No {activeFilterLabel.toLowerCase()} tasks</p>
          <p className="mt-1 text-sm text-slate-500">Try another filter to view available tasks.</p>
        </div>
      )}

      {!loadFailed && filteredTasks.length > 0 && (
        <div className="mt-8 grid gap-5 lg:grid-cols-2">
          {filteredTasks.map((task) => {
            const progress = Number.isFinite(task.progress) ? task.progress : 0;
            const checklistItems = Array.isArray(task.checklistItems) ? task.checklistItems : [];
            const completedChecklistItems = checklistItems.filter((item) => item.completed).length;
            const isTaskCreator = isAdmin
              && Number(task.createdByUserId) === currentUserId;
            const taskCanBeEdited = isTaskCreator
              && (task.status === "PENDING" || task.status === "IN_PROGRESS");
            const taskCanBeArchived = isTaskCreator && !task.archived;
            const taskCanRequestChanges = isTaskCreator && task.status === "COMPLETED";
            const remainingTime = task.status === "IN_PROGRESS" && task.deadline
              ? formatRemainingTime(task.deadline, currentTime)
              : null;
            const isOverdue = remainingTime === "Overdue";
            return (
              <article key={task.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-blue-200 hover:shadow-md sm:p-6">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <h2 className="truncate text-lg font-bold text-slate-950" title={task.title}>{task.title}</h2>
                    {task.employeeName && <p className="mt-1 text-sm text-slate-500">Assigned to {task.employeeName}</p>}
                  </div>
                  <span data-testid={`task-${task.id}-status`} className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold ${statusStyles(task.status)}`}>
                    {statusLabel(task.status)}
                  </span>
                </div>

                {task.description && <p className="mt-4 line-clamp-3 break-words text-sm leading-6 text-slate-600">{task.description}</p>}

                <div className="mt-5">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-semibold text-slate-700">Progress</span>
                    <span className="font-bold text-slate-900">{progress}%</span>
                  </div>
                  <div
                    role="progressbar"
                    aria-label={`${task.title} progress`}
                    aria-valuemin="0"
                    aria-valuemax="100"
                    aria-valuenow={progress}
                    className="mt-2 h-2.5 overflow-hidden rounded-full bg-slate-100"
                  >
                    <div className={`h-full rounded-full ${progress === 100 ? "bg-emerald-500" : "bg-blue-600"}`} style={{ width: `${progress}%` }} />
                  </div>
                </div>

                <dl className="mt-5 grid gap-4 border-t border-slate-100 pt-5 sm:grid-cols-2">
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Time allowed</dt>
                    <dd className="mt-1 text-sm font-semibold text-slate-700">{formatTimeAllowed(task.timeAllowedMinutes)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Deadline</dt>
                    <dd className="mt-1 text-sm font-semibold text-slate-700">{formatDeadline(task.deadline)}</dd>
                  </div>
                  {remainingTime && (
                    <div>
                      <dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">{isOverdue ? "Deadline status" : "Remaining time"}</dt>
                      <dd className={`mt-1 text-sm font-semibold ${isOverdue ? "text-red-600" : "text-slate-700"}`}>
                        {remainingTime}
                      </dd>
                    </div>
                  )}
                </dl>

                {checklistItems.length > 0 && (
                  <p className="mt-5 border-t border-slate-100 pt-5 text-sm font-medium text-slate-500">
                    {completedChecklistItems} of {checklistItems.length} checklist items completed
                  </p>
                )}

                <div className="mt-5 flex flex-col gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:flex-wrap">
                  <Button variant="secondary" onClick={() => setDetailsTask(task)}>View Details</Button>
                  {(taskCanBeEdited || taskCanBeArchived || taskCanRequestChanges) && (
                    <>
                    {taskCanBeEdited && (
                      <Button variant="secondary" onClick={() => setEditingTask(task)}>Edit Task</Button>
                    )}
                    {taskCanBeArchived && (
                      <Button variant="secondary" className="border-red-200 text-red-700 hover:bg-red-50" onClick={() => openArchiveTask(task)}>Archive Task</Button>
                    )}
                    {taskCanRequestChanges && (
                      <Button variant="secondary" onClick={() => setRequestChangesTask(task)}>Request Changes</Button>
                    )}
                    </>
                  )}
                </div>

                {isEmployee && task.status === "PENDING" && (
                  <div className="mt-5 flex flex-col gap-3 border-t border-slate-100 pt-5 sm:flex-row">
                    <Button className="w-full sm:w-auto" onClick={() => openTaskAction(task, "accept")}>Accept</Button>
                    <Button variant="secondary" className="w-full border-red-200 text-red-700 hover:bg-red-50 sm:w-auto" onClick={() => openTaskAction(task, "decline")}>Decline</Button>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}

      {isAdmin && createModalOpen && (
        <TaskCreateModal
          open
          onClose={() => setCreateModalOpen(false)}
          onCreated={handleTaskCreated}
        />
      )}

      {isAdmin && editingTask && (
        <TaskEditModal
          open
          task={editingTask}
          onClose={() => setEditingTask(null)}
          onUpdated={handleTaskUpdated}
        />
      )}

      {isAdmin && requestChangesTask && (
        <TaskRequestChangesModal
          open
          task={requestChangesTask}
          onClose={() => setRequestChangesTask(null)}
          onRequested={handleChangesRequested}
        />
      )}

      {detailsTask && (
        <TaskDetailsModal open task={detailsTask} onClose={() => setDetailsTask(null)} onTaskUpdated={handleTaskDetailsUpdated} isAdmin={isAdmin} isEmployee={isEmployee} currentTime={currentTime} />
      )}

      <ConfirmDialog
        open={Boolean(pendingArchiveTask)}
        title="Archive Task?"
        message="Are you sure you want to archive this task?"
        confirmLabel="Archive"
        loadingLabel="Archiving..."
        confirmVariant="danger"
        loading={archiveLoading}
        onConfirm={handleArchiveTask}
        onCancel={() => setPendingArchiveTask(null)}
      />

      <ConfirmDialog
        open={Boolean(pendingTaskAction)}
        title={pendingTaskAction?.action === "accept" ? "Accept Task?" : "Decline Task?"}
        message={pendingTaskAction?.action === "accept"
          ? "Are you ready to accept this task? Its deadline will start now."
          : "Are you sure you want to decline this task?"}
        confirmLabel={pendingTaskAction?.action === "accept" ? "Accept Task" : "Decline Task"}
        loadingLabel={pendingTaskAction?.action === "accept" ? "Accepting..." : "Declining..."}
        confirmVariant={pendingTaskAction?.action === "accept" ? "primary" : "danger"}
        loading={taskActionLoading}
        onConfirm={handleTaskAction}
        onCancel={() => setPendingTaskAction(null)}
      />
    </section>
  );
}

export default Tasks;
