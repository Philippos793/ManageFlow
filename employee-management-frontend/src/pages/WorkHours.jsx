import { useEffect, useState } from "react";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import WorkHoursDetailsModal from "../components/WorkHoursDetailsModal.jsx";
import authService from "../services/authService.js";
import LoadingSpinner from "../components/ui/LoadingSpinner.jsx";
import PageTitle from "../components/ui/PageTitle.jsx";
import workShiftService from "../services/workShiftService.js";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";

const months = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

function WorkHours() {
  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [report, setReport] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const isAdmin = authService.getRole() === "ADMIN";
  const years = Array.from({ length: 7 }, (_, index) => today.getFullYear() - 5 + index);

  useEffect(() => {
    let isMounted = true;
    let requestInProgress = false;

    async function loadReport() {
      if (requestInProgress) return;
      requestInProgress = true;

      try {
        const data = await workShiftService.getMonthlyReport(year, month);
        if (isMounted) {
          setReport(data);
          setError("");
        }
      } catch (requestError) {
        if (isMounted) {
          setReport([]);
          setError(
            requestError?.data?.message ||
              getFriendlyErrorMessage(requestError, "Failed to load work-hours report.")
          );
        }
      } finally {
        if (isMounted) setLoading(false);
        requestInProgress = false;
      }
    }

    loadReport();
    const intervalId = window.setInterval(loadReport, 10000);

    return () => {
      isMounted = false;
      window.clearInterval(intervalId);
    };
  }, [year, month]);

  return (
    <section>
      <PageTitle
        eyebrow="Reporting"
        title="Work Hours"
        description="Review completed employee shifts by month."
      />

      <div className="mt-8 grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-2 sm:p-6">
        <div>
          <label htmlFor="report-month" className="mb-2 block text-sm font-semibold text-slate-700">
            Month
          </label>
          <select
            id="report-month"
            value={month}
            onChange={(event) => {
              setLoading(true);
              setError("");
              setMonth(Number(event.target.value));
            }}
            className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          >
            {months.map((name, index) => (
              <option key={name} value={index + 1}>{name}</option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="report-year" className="mb-2 block text-sm font-semibold text-slate-700">
            Year
          </label>
          <select
            id="report-year"
            value={year}
            onChange={(event) => {
              setLoading(true);
              setError("");
              setYear(Number(event.target.value));
            }}
            className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
          >
            {years.map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
        </div>
      </div>

      <div className="mt-6">
        <FeedbackMessage>{error}</FeedbackMessage>
      </div>

      <div className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {loading ? (
          <LoadingSpinner label="Loading work hours..." />
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">Employee</th>
                  <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">Completed Shifts</th>
                  <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">Total Worked Time</th>
                  {isAdmin && <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">Actions</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {report.map((row, index) => (
                  <tr key={row.employeeId} className={index % 2 ? "bg-slate-50/50" : "bg-white"}>
                    <td className="px-5 py-4 font-semibold text-slate-900">{row.fullName}</td>
                    <td className="px-5 py-4 text-sm text-slate-600">{row.completedShifts}</td>
                    <td className="px-5 py-4 text-sm font-medium text-slate-700">{row.totalWorkedHours}</td>
                    {isAdmin && (<td className="px-5 py-4">
                      <button type="button" onClick={() => setSelectedEmployee(row)} className="rounded-lg px-3 py-2 text-sm font-semibold text-blue-600 transition hover:bg-blue-50">
                        View Details
                      </button>
                    </td>)}
                  </tr>
                ))}
                {report.length === 0 && !error && (
                  <tr>
                    <td colSpan={isAdmin ? 4 : 3} className="px-6 py-16 text-center text-sm text-slate-500">
                      No work-hour data for this month.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {isAdmin && selectedEmployee && (
        <WorkHoursDetailsModal
          employee={selectedEmployee}
          year={year}
          month={month}
          onClose={() => setSelectedEmployee(null)}
        />
      )}
    </section>
  );
}

export default WorkHours;