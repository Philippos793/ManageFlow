import { useCallback, useEffect, useRef, useState } from "react";
import notificationService from "../services/notificationService.js";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";
import FeedbackMessage from "./ui/FeedbackMessage.jsx";
import LoadingSpinner from "./ui/LoadingSpinner.jsx";

function formatCreatedAt(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unknown date";

  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function notificationTypeLabel(type) {
  return type
    ?.replace(/^TASK_/, "")
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase()) || "Notification";
}

function uniqueNotifications(data) {
  if (!Array.isArray(data)) return [];

  const notificationsById = new Map();
  data.forEach((notification) => {
    if (notification?.id != null) {
      notificationsById.set(notification.id, notification);
    }
  });
  return [...notificationsById.values()];
}

function TaskNotifications({
  embedded = false,
  open: externalOpen = false,
  onUnreadCountChange,
}) {
  const [notifications, setNotifications] = useState([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [backgroundError, setBackgroundError] = useState("");
  const [markingAsRead, setMarkingAsRead] = useState("");
  const [actionError, setActionError] = useState("");
  const notificationRequestInFlightRef = useRef(false);
  const isOpen = embedded ? externalOpen : open;
  const unreadCount = notifications.filter((notification) => !notification.readAt).length;

  useEffect(() => {
    onUnreadCountChange?.(unreadCount);
  }, [onUnreadCountChange, unreadCount]);

  const loadNotifications = useCallback(async (background = false) => {
    if (notificationRequestInFlightRef.current) return;

    notificationRequestInFlightRef.current = true;
    if (!background) {
      setLoading(true);
      setError("");
      setBackgroundError("");
    }

    try {
      const data = await notificationService.getNotifications();
      setNotifications(uniqueNotifications(data));
      setBackgroundError("");
      setError("");
    } catch (requestError) {
      const message = getFriendlyErrorMessage(
        requestError,
        "Failed to load notifications."
      );
      if (background) {
        setBackgroundError(message);
      } else {
        setError(message);
      }
    } finally {
      notificationRequestInFlightRef.current = false;
      if (!background) setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(() => loadNotifications());
  }, [loadNotifications]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      void loadNotifications(true);
    }, 10000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [loadNotifications]);

  async function handleMarkAsRead(notification) {
    if (notification.readAt || markingAsRead === notification.id) return;

    setMarkingAsRead(notification.id);
    setActionError("");

    try {
      const updatedNotification = await notificationService.markAsRead(notification.id);
      setNotifications((currentNotifications) => currentNotifications.map((current) =>
        current.id === notification.id
          ? { ...current, ...updatedNotification, readAt: updatedNotification?.readAt || new Date().toISOString() }
          : current
      ));
    } catch (requestError) {
      setActionError(getFriendlyErrorMessage(
        requestError,
        "Failed to mark the notification as read."
      ));
    } finally {
      setMarkingAsRead("");
    }
  }

  return (
    <div className={embedded ? "" : "relative"}>
      {!embedded && (
        <button
        type="button"
        aria-label="Task notifications"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        className="relative flex size-10 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 focus:outline-none focus:ring-4 focus:ring-blue-100"
      >
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="size-5">
          <path strokeLinecap="round" strokeLinejoin="round" d="M14.9 18a3 3 0 0 1-5.8 0m9.4-2.5H5.5c1.4-1.5 2-3.4 2-5.5a4.5 4.5 0 0 1 9 0c0 2.1.6 4 2 5.5Z" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 flex min-w-5 items-center justify-center rounded-full bg-blue-600 px-1 text-[11px] font-bold leading-5 text-white shadow-sm">
            {unreadCount > 99
              ? "99+"
              : unreadCount}
          </span>
        )}
        </button>
      )}

      {isOpen && (
        <section aria-labelledby="task-notifications-title" className={embedded ? "" : "absolute right-0 z-40 mt-2 w-[min(24rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl shadow-slate-900/10"}>
          {!embedded && (
            <div className="border-b border-slate-200 px-4 py-3">
            <h2 id="task-notifications-title" className="font-bold text-slate-950">Task notifications</h2>
            <p className="mt-0.5 text-xs text-slate-500">Updates about your assigned tasks.</p>
            </div>
          )}

          <div className="max-h-96 overflow-y-auto p-2">
            {loading ? (
              <LoadingSpinner compact label="Loading notifications..." />
            ) : error ? (
              <div className="space-y-3 px-2 py-3">
                <FeedbackMessage>{error}</FeedbackMessage>
                <button
                  type="button"
                  onClick={() => loadNotifications()}
                  className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                >
                  Try again
                </button>
              </div>
            ) : notifications.length === 0 ? (
              <p className="px-3 py-5 text-center text-sm text-slate-500">You have no notifications.</p>
            ) : (
              <div className="space-y-3 p-2">
                <FeedbackMessage>{backgroundError}</FeedbackMessage>
                <FeedbackMessage>{actionError}</FeedbackMessage>
                {notifications.map((notification) => {
            const isUnread = !notification.readAt;
            const isMarkingAsRead = markingAsRead === notification.id;

            return (
              <article
                key={notification.id}
                className={`rounded-xl border px-4 py-4 ${
                  isUnread
                    ? "border-blue-200 bg-blue-50/60"
                    : "border-slate-200 bg-white"
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold uppercase tracking-wide text-slate-500">
                        {notificationTypeLabel(notification.type)}
                      </span>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                        isUnread
                          ? "bg-blue-100 text-blue-700"
                          : "bg-slate-100 text-slate-600"
                      }`}>
                        {isUnread ? "Unread" : "Read"}
                      </span>
                    </div>
                    <p className="mt-2 text-sm font-semibold text-slate-900">
                      {notification.message}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      {formatCreatedAt(notification.createdAt)}
                    </p>
                  </div>

                  {isUnread && (
                    <button
                      type="button"
                      onClick={() => handleMarkAsRead(notification)}
                      disabled={isMarkingAsRead}
                      className="shrink-0 rounded-lg border border-blue-300 px-3 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-100 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {isMarkingAsRead ? "Marking..." : "Mark as read"}
                    </button>
                  )}
                </div>
              </article>
            );
                })}
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
}

export default TaskNotifications;
