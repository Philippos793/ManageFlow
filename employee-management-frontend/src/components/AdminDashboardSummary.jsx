import { useEffect, useState } from "react";
import dashboardService from "../services/dashboardService.js";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";
import FeedbackMessage from "./ui/FeedbackMessage.jsx";
import LoadingSpinner from "./ui/LoadingSpinner.jsx";

function AdminDashboardSummary() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;
    let requestInProgress = false;

    async function loadSummary() {
      if (requestInProgress) return;
      requestInProgress = true;
      try {
        const data = await dashboardService.getAdminSummary();
        if (isMounted) {
          setSummary(data);
          setError("");
        }
      } catch (requestError) {
        if (isMounted) {
          setError(requestError?.data?.message || getFriendlyErrorMessage(
            requestError, "Failed to load dashboard statistics."
          ));
        }
      } finally {
        if (isMounted) setLoading(false);
        requestInProgress = false;
      }
    }

    loadSummary();
    const intervalId = window.setInterval(loadSummary, 10000);
    return () => {
      isMounted = false;
      window.clearInterval(intervalId);
    };
  }, []);

  const cards = summary ? [
    { label: "Active Employees", value: summary.totalEmployees, color: "text-blue-700", accent: "bg-blue-50", dot: "bg-blue-500" },
    { label: "Departments", value: summary.totalDepartments, color: "text-indigo-700", accent: "bg-indigo-50", dot: "bg-indigo-500" },
    { label: "Active Shifts", value: summary.activeShifts, color: "text-emerald-700", accent: "bg-emerald-50", dot: "bg-emerald-500", active: summary.activeShifts > 0 },
    { label: "Hours This Month", value: summary.hoursThisMonth, color: "text-amber-700", accent: "bg-amber-50", dot: "bg-amber-500", wideValue: true },
  ] : [];

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8">
      <div>
        <p className="text-sm font-semibold uppercase tracking-wider text-blue-600">Admin overview</p>
        <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-950">Dashboard Summary</h2>
        <p className="mt-2 text-sm text-slate-500">Live organization statistics, refreshed every 10 seconds.</p>
      </div>

      <div className="mt-5"><FeedbackMessage>{error}</FeedbackMessage></div>

      {loading ? (
        <LoadingSpinner label="Loading dashboard statistics..." />
      ) : summary ? (
        <div className="mt-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {cards.map((card) => (
            <div
              key={card.label}
              className={`group min-w-0 rounded-2xl border bg-white p-5 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:shadow-md sm:p-6 ${card.active ? "border-emerald-300 ring-4 ring-emerald-50" : "border-slate-200"}`}
            >
              <div className="flex items-center justify-between gap-3">
                <div className={`inline-flex rounded-lg px-3 py-1.5 ${card.accent}`}>
                  <span className={`text-xs font-bold uppercase tracking-[0.12em] ${card.color}`}>{card.label}</span>
                </div>
                <span className={`size-2.5 shrink-0 rounded-full ${card.dot} ${card.active ? "animate-pulse ring-4 ring-emerald-100" : "opacity-70"}`} />
              </div>
              <p className={`mt-6 min-w-0 font-bold leading-none tracking-tight text-slate-950 ${card.wideValue ? "break-words text-3xl sm:text-4xl" : "text-4xl sm:text-5xl"}`}>
                {card.value}
              </p>
            </div>
          ))}
        </div>
      ) : null}
    </section>
  );
}

export default AdminDashboardSummary;
