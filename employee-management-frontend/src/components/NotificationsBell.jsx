import { useCallback, useState } from "react";
import RegistrationNotifications from "./RegistrationNotifications.jsx";
import TaskNotifications from "./TaskNotifications.jsx";

function NotificationsBell({ isAdmin }) {
  const [open, setOpen] = useState(false);
  const [activeTab, setActiveTab] = useState(isAdmin ? "requests" : "tasks");
  const [requestCount, setRequestCount] = useState(0);
  const [taskUnreadCount, setTaskUnreadCount] = useState(0);
  const totalUnreadCount = requestCount + taskUnreadCount;

  const handleRequestCountChange = useCallback((count) => {
    setRequestCount(count);
  }, []);

  const handleTaskUnreadCountChange = useCallback((count) => {
    setTaskUnreadCount(count);
  }, []);

  function selectTab(tab) {
    setActiveTab(tab);
  }

  return (
    <div className="relative">
      <button
        type="button"
        aria-label="Notifications"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        className="relative flex size-10 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 focus:outline-none focus:ring-4 focus:ring-blue-100"
      >
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="size-5">
          <path strokeLinecap="round" strokeLinejoin="round" d="M14.9 18a3 3 0 0 1-5.8 0m9.4-2.5H5.5c1.4-1.5 2-3.4 2-5.5a4.5 4.5 0 0 1 9 0c0 2.1.6 4 2 5.5Z" />
        </svg>
        {totalUnreadCount > 0 && (
          <span className="absolute -right-1 -top-1 flex min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-[11px] font-bold leading-5 text-white shadow-sm">
            {totalUnreadCount > 99 ? "99+" : totalUnreadCount}
          </span>
        )}
      </button>

      <div className={`${open ? "absolute" : "hidden"} right-0 z-40 mt-2 w-[min(24rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl shadow-slate-900/10`}>
        <div className="border-b border-slate-200 px-4 py-3">
          <h2 className="font-bold text-slate-950">Notifications</h2>
          <p className="mt-0.5 text-xs text-slate-500">Stay up to date with your work.</p>
        </div>

        {isAdmin && (
          <div className="flex border-b border-slate-200 px-2 pt-2" role="tablist" aria-label="Notification categories">
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === "requests"}
              onClick={() => selectTab("requests")}
              className={`flex-1 rounded-t-lg px-3 py-2 text-sm font-semibold ${activeTab === "requests" ? "bg-blue-50 text-blue-700" : "text-slate-500 hover:bg-slate-50"}`}
            >
              Requests
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === "tasks"}
              onClick={() => selectTab("tasks")}
              className={`flex-1 rounded-t-lg px-3 py-2 text-sm font-semibold ${activeTab === "tasks" ? "bg-blue-50 text-blue-700" : "text-slate-500 hover:bg-slate-50"}`}
            >
              Tasks
            </button>
          </div>
        )}

        {isAdmin && (
          <RegistrationNotifications
            embedded
            open={activeTab === "requests"}
            onRequestCountChange={handleRequestCountChange}
          />
        )}
        <TaskNotifications
          embedded
          open={activeTab === "tasks"}
          onUnreadCountChange={handleTaskUnreadCountChange}
        />
      </div>
    </div>
  );
}

export default NotificationsBell;
