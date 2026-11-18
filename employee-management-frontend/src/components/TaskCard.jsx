import Button from "./ui/Button.jsx";
import {
  formatDateTime,
  formatRemainingTime,
  formatTimeAllowed,
  taskStatusLabel,
  taskStatusStyles,
} from "../utils/taskFormatting.js";

function TaskCard({
  task,
  currentTime,
  isAdmin,
  isEmployee,
  currentUserId,
  onViewDetails,
  onEdit,
  onArchive,
  onRequestChanges,
  onTaskAction,
}) {
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
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-blue-200 hover:shadow-md sm:p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <h2 className="truncate text-lg font-bold text-slate-950" title={task.title}>{task.title}</h2>
          {task.employeeName && <p className="mt-1 text-sm text-slate-500">Assigned to {task.employeeName}</p>}
        </div>
        <span data-testid={`task-${task.id}-status`} className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold ${taskStatusStyles(task.status)}`}>
          {taskStatusLabel(task.status)}
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
          <dd className="mt-1 text-sm font-semibold text-slate-700">{formatDateTime(task.deadline)}</dd>
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
        <Button variant="secondary" onClick={() => onViewDetails(task)}>View Details</Button>
        {taskCanBeEdited && (
          <Button variant="secondary" onClick={() => onEdit(task)}>Edit Task</Button>
        )}
        {taskCanBeArchived && (
          <Button variant="secondary" className="border-red-200 text-red-700 hover:bg-red-50" onClick={() => onArchive(task)}>Archive Task</Button>
        )}
        {taskCanRequestChanges && (
          <Button variant="secondary" onClick={() => onRequestChanges(task)}>Request Changes</Button>
        )}
      </div>

      {isEmployee && task.status === "PENDING" && (
        <div className="mt-5 flex flex-col gap-3 border-t border-slate-100 pt-5 sm:flex-row">
          <Button className="w-full sm:w-auto" onClick={() => onTaskAction(task, "accept")}>Accept</Button>
          <Button variant="secondary" className="w-full border-red-200 text-red-700 hover:bg-red-50 sm:w-auto" onClick={() => onTaskAction(task, "decline")}>Decline</Button>
        </div>
      )}
    </article>
  );
}

export default TaskCard;
