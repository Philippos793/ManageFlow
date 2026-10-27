export function formatTimeAllowed(minutes) {
  if (!Number.isFinite(minutes)) return "Not available";
  const days = Math.floor(minutes / (24 * 60));
  const hours = Math.floor((minutes % (24 * 60)) / 60);
  const remainingMinutes = minutes % 60;
  return days > 0
    ? `${days}d ${hours}h ${remainingMinutes}m`
    : `${hours}h ${remainingMinutes}m`;
}

export function formatDateTime(value) {
  if (!value) return "Not set";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Not available";
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function formatRemainingTime(deadline, currentTime) {
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

export function isOverdueTask(task, currentTime) {
  if (task.status !== "IN_PROGRESS" || !task.deadline) return false;
  const deadline = new Date(task.deadline);
  return !Number.isNaN(deadline.getTime()) && deadline.getTime() <= currentTime;
}

export function matchesTaskFilter(task, filter, currentTime) {
  if (filter === "ALL") return true;
  if (filter === "OVERDUE") return isOverdueTask(task, currentTime);
  if (filter === "IN_PROGRESS") {
    return task.status === "IN_PROGRESS" && !isOverdueTask(task, currentTime);
  }
  return task.status === filter;
}

export function taskStatusStyles(status) {
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

export function taskStatusLabel(status) {
  return status?.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) || "Unknown";
}
