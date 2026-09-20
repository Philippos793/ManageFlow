import { useCallback, useEffect, useRef, useState } from "react";
import taskService from "../services/taskService.js";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";
import useModalAccessibility from "../hooks/useModalAccessibility.js";
import TaskAttachments from "./TaskAttachments.jsx";
import Button from "./ui/Button.jsx";
import FeedbackMessage from "./ui/FeedbackMessage.jsx";
import LoadingSpinner from "./ui/LoadingSpinner.jsx";

function formatTimeAllowed(minutes) {
  if (!Number.isFinite(minutes)) return "Not available";
  const days = Math.floor(minutes / (24 * 60));
  const hours = Math.floor((minutes % (24 * 60)) / 60);
  const remainingMinutes = minutes % 60;
  return days > 0 ? `${days}d ${hours}h ${remainingMinutes}m` : `${hours}h ${remainingMinutes}m`;
}

function formatDateTime(value) {
  if (!value) return "Not set";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Not available";
  return new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" }).format(date);
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

function statusLabel(status) {
  return status?.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) || "Unknown";
}

function TaskDetailsModal({ open, task, onClose, onTaskUpdated, isAdmin, isEmployee, currentTime }) {
  const dialogRef = useModalAccessibility(open, onClose);
  const [activities, setActivities] = useState([]);
  const [activitiesLoading, setActivitiesLoading] = useState(false);
  const [activitiesError, setActivitiesError] = useState("");
  const [actionError, setActionError] = useState("");
  const [updatingChecklistItemId, setUpdatingChecklistItemId] = useState(null);
  const [progressDraft, setProgressDraft] = useState(() => (
    String(Number.isFinite(task?.progress) ? task.progress : 0)
  ));
  const [progressError, setProgressError] = useState("");
  const [updatingProgress, setUpdatingProgress] = useState(false);
  const checklistUpdateLockedRef = useRef(false);
  const progressUpdateLockedRef = useRef(false);

  const loadActivities = useCallback(async () => {
    if (!task) return;
    setActivitiesLoading(true);
    setActivitiesError("");
    try {
      const data = await taskService.getTaskActivities(task.id);
      setActivities(Array.isArray(data) ? data : []);
    } catch (requestError) {
      setActivities([]);
      setActivitiesError(getFriendlyErrorMessage(requestError, "Failed to load activity history."));
    } finally {
      setActivitiesLoading(false);
    }
  }, [task]);

  useEffect(() => {
    if (open) void Promise.resolve().then(loadActivities);
  }, [open, loadActivities]);

  async function handleChecklistItemChange(item) {
    if (!task || checklistUpdateLockedRef.current) return;

    checklistUpdateLockedRef.current = true;
    setUpdatingChecklistItemId(item.id);
    setActionError("");
    try {
      const updatedTask = await taskService.updateChecklistItem(task.id, item.id, !item.completed);
      await onTaskUpdated?.(updatedTask);
    } catch (requestError) {
      setActionError(getFriendlyErrorMessage(requestError, "Failed to update checklist item."));
    } finally {
      checklistUpdateLockedRef.current = false;
      setUpdatingChecklistItemId(null);
    }
  }

  async function handleProgressUpdate() {
    if (!task || progressUpdateLockedRef.current) return;

    const progress = Number(progressDraft);
    if (!Number.isInteger(progress) || progress < task.progress || progress > 100) {
      setProgressError(progress < task.progress
        ? "Progress cannot be decreased."
        : "Progress must be a whole number between the current value and 100.");
      return;
    }

    progressUpdateLockedRef.current = true;
    setUpdatingProgress(true);
    setActionError("");
    try {
      const updatedTask = await taskService.updateProgress(task.id, progress);
      await onTaskUpdated?.(updatedTask);
    } catch (requestError) {
      setActionError(getFriendlyErrorMessage(requestError, "Failed to update task progress."));
    } finally {
      progressUpdateLockedRef.current = false;
      setUpdatingProgress(false);
    }
  }

  if (!open || !task) return null;

  const checklistItems = Array.isArray(task.checklistItems) ? task.checklistItems : [];
  const completedChecklistItems = checklistItems.filter((item) => item.completed).length;
  const remainingTime = task.status === "IN_PROGRESS" && task.deadline
    ? formatRemainingTime(task.deadline, currentTime)
    : null;
  const isOverdue = remainingTime === "Overdue";
  const checklistEditable = isEmployee && task.status === "IN_PROGRESS";
  const canUpdateManualProgress = isEmployee
    && task.status === "IN_PROGRESS"
    && checklistItems.length === 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-950/60 p-4 backdrop-blur-sm" role="presentation">
      <div ref={dialogRef} tabIndex="-1" role="dialog" aria-modal="true" aria-labelledby="task-details-title" className="my-auto flex max-h-[90vh] w-full max-w-3xl flex-col rounded-3xl bg-white shadow-2xl outline-none">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-5 sm:px-7">
          <div className="min-w-0"><p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">Work management</p><h2 id="task-details-title" className="mt-2 break-words text-2xl font-bold text-slate-950">Task Details</h2><p className="mt-1 break-words text-sm text-slate-500">Review task information, shared files, and activity history.</p></div>
          <button type="button" aria-label="Close task details" onClick={onClose} className="flex size-9 shrink-0 items-center justify-center rounded-full border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-900">X</button>
        </div>

        <div className="overflow-y-auto px-5 py-6 sm:px-7">
          <section aria-labelledby="task-details-overview" className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4 sm:p-5">
            <div className="flex flex-wrap items-start justify-between gap-3"><div className="min-w-0"><h3 id="task-details-overview" className="break-words text-xl font-bold text-slate-950">{task.title}</h3><p className="mt-2 break-words text-sm leading-6 text-slate-600">{task.description || "No description provided."}</p></div><span className="rounded-full bg-slate-200 px-3 py-1 text-xs font-semibold text-slate-700">{statusLabel(task.status)}</span></div>
            <dl className="mt-5 grid gap-4 border-t border-slate-200 pt-5 sm:grid-cols-2">
              <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Assigned employee</dt><dd className="mt-1 text-sm font-semibold text-slate-700">{task.employeeName || "Not available"}</dd></div>
              <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Progress</dt><dd className="mt-1 text-sm font-semibold text-slate-700">{Number.isFinite(task.progress) ? task.progress : 0}%</dd></div>
              <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Time allowed</dt><dd className="mt-1 text-sm font-semibold text-slate-700">{formatTimeAllowed(task.timeAllowedMinutes)}</dd></div>
              <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Deadline</dt><dd className="mt-1 text-sm font-semibold text-slate-700">{formatDateTime(task.deadline)}</dd></div>
              {remainingTime && <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">{isOverdue ? "Deadline status" : "Remaining time"}</dt><dd className={`mt-1 text-sm font-semibold ${isOverdue ? "text-red-600" : "text-slate-700"}`}>{remainingTime}</dd></div>}
            </dl>
          </section>

          <FeedbackMessage>{actionError}</FeedbackMessage>

          <section className="mt-6" aria-labelledby="task-details-checklist">
            <div className="flex items-center justify-between gap-3"><h3 id="task-details-checklist" className="text-base font-bold text-slate-900">Checklist</h3>{checklistItems.length > 0 && <span className="text-xs font-semibold text-slate-500">{completedChecklistItems} of {checklistItems.length} completed</span>}</div>
            {checklistItems.length === 0 ? <p className="mt-3 rounded-xl border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">No checklist items.</p> : <ul className="mt-3 space-y-2">{checklistItems.map((item) => { const isUpdating = updatingChecklistItemId === item.id; return <li key={item.id} className={`flex items-center gap-3 rounded-xl border px-3 py-2.5 ${item.completed ? "border-emerald-100 bg-emerald-50/70" : "border-slate-200 bg-white"}`}><input type="checkbox" checked={Boolean(item.completed)} disabled={!checklistEditable || updatingChecklistItemId !== null} onChange={() => handleChecklistItemChange(item)} aria-label={item.description} className="size-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 disabled:cursor-not-allowed" /><span className={`min-w-0 flex-1 text-sm ${item.completed ? "font-medium text-emerald-800 line-through" : "text-slate-700"}`}>{item.description}</span>{isUpdating && <span className="text-xs font-semibold text-slate-500">Updating...</span>}</li>; })}</ul>}
          </section>

          {canUpdateManualProgress && (
            <section className="mt-6 border-t border-slate-100 pt-5" aria-labelledby="task-details-progress">
              <h3 id="task-details-progress" className="text-base font-bold text-slate-900">Update Progress</h3>
              <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end">
                <div className="flex-1">
                  <label htmlFor="task-details-progress-input" className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Progress percentage</label>
                  <input id="task-details-progress-input" type="number" min={task.progress} max="100" step="1" value={progressDraft} disabled={updatingProgress} onChange={(event) => { setProgressDraft(event.target.value); setProgressError(""); }} className="mt-1.5 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm font-semibold text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                  {progressError && <p className="mt-1.5 text-sm font-medium text-red-600">{progressError}</p>}
                </div>
                <Button disabled={updatingProgress} onClick={handleProgressUpdate} className="w-full shrink-0 sm:w-auto">{updatingProgress ? "Updating..." : "Update Progress"}</Button>
              </div>
            </section>
          )}

          <TaskAttachments taskId={task.id} taskStatus={task.status} isAdmin={isAdmin} isEmployee={isEmployee} />

          <section className="mt-6 border-t border-slate-100 pt-5" aria-labelledby="task-activity-history">
            <h3 id="task-activity-history" className="text-base font-bold text-slate-900">Activity History</h3>
            {activitiesLoading ? <LoadingSpinner compact label="Loading activity history..." /> : activitiesError ? <div className="mt-3 space-y-3"><FeedbackMessage>{activitiesError}</FeedbackMessage><Button variant="secondary" onClick={loadActivities}>Try again</Button></div> : activities.length === 0 ? <p className="mt-3 rounded-xl border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">No activity recorded yet.</p> : <ol aria-label="Activity timeline" className="mt-4 space-y-3 border-l-2 border-slate-200 pl-4">{activities.map((activity, index) => <li key={`${activity.createdAt}-${activity.type}-${index}`} className="relative rounded-xl border border-slate-200 bg-white p-3"><span className="absolute -left-[1.35rem] top-5 size-2.5 rounded-full bg-blue-500 ring-4 ring-white" /><p className="text-sm font-semibold text-slate-800">{activity.message}</p><p className="mt-1 text-xs text-slate-500">{activity.actorUsername} · {formatDateTime(activity.createdAt)}</p></li>)}</ol>}
          </section>
        </div>

        <div className="border-t border-slate-200 px-5 py-4 sm:px-7"><Button variant="secondary" onClick={onClose}>Close</Button></div>
      </div>
    </div>
  );
}

export default TaskDetailsModal;
