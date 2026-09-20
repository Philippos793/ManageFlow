import { useCallback, useEffect, useRef, useState } from "react";
import Button from "./ui/Button.jsx";
import FeedbackMessage from "./ui/FeedbackMessage.jsx";
import LoadingSpinner from "./ui/LoadingSpinner.jsx";
import workShiftService from "../services/workShiftService.js";
import employeeService from "../services/employeeService.js";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";
import useModalAccessibility from "../hooks/useModalAccessibility.js";

const weekdays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function formatMinutes(minutes) {
  if (!minutes) return "0h 0m";
  return `${Math.floor(minutes / 60)}h ${String(minutes % 60).padStart(2, "0")}m`;
}

function WorkHoursDetailsModal({ employee, year, month, onClose }) {
  const dialogRef = useModalAccessibility(true, onClose);
  const [details, setDetails] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [payrollVisible, setPayrollVisible] = useState(false);
  const [hourlyRate, setHourlyRate] = useState("");
  const [rateError, setRateError] = useState("");
  const [estimatedPay, setEstimatedPay] = useState(null);
  const [savingRate, setSavingRate] = useState(false);
  const [rateSuccess, setRateSuccess] = useState("");
  const saveRateLockedRef = useRef(false);

  const loadDetails = useCallback(() => {
    setLoading(true);
    setError("");
    return workShiftService
      .getEmployeeMonthlyDetails(employee.employeeId, year, month)
      .then((data) => {
        setDetails(data);
        setHourlyRate(data.hourlyRate?.toString() || "");
      })
      .catch((requestError) => {
        setDetails(null);
        setError(requestError?.data?.message || getFriendlyErrorMessage(
          requestError, "Failed to load employee work-hour details."
        ));
      })
      .finally(() => setLoading(false));
  }, [employee.employeeId, year, month]);

  useEffect(() => {
    let isMounted = true;
    workShiftService.getEmployeeMonthlyDetails(employee.employeeId, year, month)
      .then((data) => {
        if (!isMounted) return;
        setDetails(data);
        setHourlyRate(data.hourlyRate?.toString() || "");
      })
      .catch((requestError) => {
        if (!isMounted) return;
        setDetails(null);
        setError(requestError?.data?.message || getFriendlyErrorMessage(
          requestError, "Failed to load employee work-hour details."
        ));
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });
    return () => { isMounted = false; };
  }, [employee.employeeId, year, month]);


  function showPayroll() {
    setPayrollVisible(true);
    setHourlyRate(details.hourlyRate?.toString() || "");
    setRateError("");
    setEstimatedPay(null);
  }


  async function saveRate() {
    if (saveRateLockedRef.current) return;
    if (hourlyRate.trim() === "") {
      setRateError("Hourly rate is required.");
      setRateSuccess("");
      return;
    }

    const parsedRate = Number(hourlyRate);
    if (!Number.isFinite(parsedRate) || parsedRate < 0) {
      setRateError("Enter a valid non-negative hourly rate.");
      setRateSuccess("");
      return;
    }

    saveRateLockedRef.current = true;
    setSavingRate(true);
    setRateError("");
    setRateSuccess("");

    try {
      const response = await employeeService.updateHourlyRate(
        employee.employeeId, parsedRate
      );
      setHourlyRate(response.hourlyRate.toString());
      setDetails((current) => ({ ...current, hourlyRate: response.hourlyRate }));
      setRateSuccess("Hourly rate saved successfully.");
    } catch (requestError) {
      setRateError(requestError?.data?.message || getFriendlyErrorMessage(
        requestError, "Failed to save hourly rate."
      ));
    } finally {
      saveRateLockedRef.current = false;
      setSavingRate(false);
    }
  }
  function calculatePay() {
    if (hourlyRate.trim() === "") {
      setRateError("Hourly rate is required.");
      setEstimatedPay(null);
      return;
    }

    const parsedRate = Number(hourlyRate);
    if (!Number.isFinite(parsedRate) || parsedRate < 0) {
      setRateError("Enter a valid non-negative hourly rate.");
      setEstimatedPay(null);
      return;
    }

    setRateError("");
    setEstimatedPay(((details.totalWorkedMinutes / 60) * parsedRate).toFixed(2));
  }
  const leadingDays = (new Date(year, month - 1, 1).getDay() + 6) % 7;
  const monthLabel = new Intl.DateTimeFormat("en", { month: "long" })
    .format(new Date(year, month - 1, 1));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-950/60 p-3 backdrop-blur-sm sm:p-6" role="presentation">
      <div ref={dialogRef} tabIndex="-1" className="my-auto max-h-[100dvh] w-full max-w-6xl overflow-y-auto border border-white/70 bg-white shadow-2xl shadow-slate-950/20 outline-none sm:max-h-[94vh] sm:rounded-3xl" role="dialog" aria-modal="true" aria-labelledby="work-hours-details-title">
        <div className="sticky top-0 z-10 flex items-start justify-between gap-4 border-b border-slate-200 bg-white/95 px-5 py-5 backdrop-blur sm:px-8 sm:py-6">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">Monthly details</p>
            <h2 id="work-hours-details-title" className="mt-2 text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{details?.fullName || employee.fullName}</h2>
            <p className="mt-1.5 text-sm font-medium text-slate-500">{monthLabel} {year}</p>
          </div>
          <button type="button" onClick={onClose} aria-label="Close" className="flex size-10 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-white text-base font-semibold text-slate-500 shadow-sm transition hover:border-slate-300 hover:bg-slate-50 hover:text-slate-900">X</button>
        </div>

        <div className="px-4 py-6 sm:px-8 sm:py-8">
          {loading ? <LoadingSpinner label="Loading monthly details..." /> : error ? (
            <div className="space-y-4 text-center">
              <FeedbackMessage>{error}</FeedbackMessage>
              <Button variant="secondary" onClick={loadDetails}>Retry</Button>
            </div>
          ) : (
            <>
              <div className="rounded-2xl border border-slate-200 bg-slate-50/70 p-2 sm:p-5">
                <div className="grid grid-cols-7 gap-1 sm:gap-3">
                  {weekdays.map((day) => (
                    <div key={day} className="pb-1 text-center text-[10px] font-bold uppercase text-slate-500 sm:text-xs sm:tracking-wider">
                      {day}
                    </div>
                  ))}
                  {Array.from({ length: leadingDays }, (_, index) => (
                    <div key={`empty-${index}`} className="min-h-16 rounded-lg border border-dashed border-slate-200/80 bg-white/30 sm:min-h-24 sm:rounded-xl" />
                  ))}
                {details.dailyWorkedTimes.map((day) => {
                  const hasTime = day.workedMinutes > 0;
                  return (
                    <div key={day.day} className={`min-h-20 rounded-lg border p-1.5 transition sm:min-h-28 sm:rounded-xl sm:p-3.5 ${hasTime ? "border-emerald-200 bg-emerald-50/70 shadow-sm shadow-emerald-100/60" : "border-slate-200 bg-white"}`}>
                      <p className={`flex size-6 items-center justify-center rounded-full text-xs font-bold sm:size-7 sm:text-sm ${hasTime ? "bg-emerald-100 text-emerald-800" : "bg-slate-100 text-slate-600"}`}>{day.day}</p>
                      <p className={`mt-3 break-words text-[10px] font-bold leading-tight sm:mt-5 sm:text-sm ${hasTime ? "text-emerald-700" : "font-medium text-slate-400"}`}>
                        {formatMinutes(day.workedMinutes)}
                      </p>
                    </div>
                  );
                })}
              </div>

                            </div>

              <div className="mt-8 flex flex-col gap-4 border-t border-slate-200 pt-6 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm font-semibold text-slate-900">Payroll summary</p>
                  <p className="mt-1 text-sm text-slate-500">Prepare an estimate from this month&apos;s completed hours.</p>
                </div>
                <Button onClick={showPayroll} className="w-full sm:w-auto">Prepare Payroll</Button>
              </div>

              {payrollVisible && (
                <div className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                  <div className="border-b border-slate-200 bg-slate-50/80 px-5 py-4 sm:px-6">
                    <h3 className="text-lg font-bold text-slate-950">Payroll Estimate</h3>
                    <p className="mt-1 text-sm text-slate-500">Review hours, set the rate, and calculate estimated pay.</p>
                  </div>

                  <div className="grid gap-4 p-4 sm:p-6 lg:grid-cols-3">
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
                      <p className="text-xs font-bold uppercase tracking-wider text-slate-500">Total Hours</p>
                      <p className="mt-3 text-2xl font-bold tracking-tight text-slate-950">
                        {formatMinutes(details.totalWorkedMinutes)}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">Completed shifts this month</p>
                    </div>

                    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                      <label htmlFor="hourly-rate" className="block text-xs font-bold uppercase tracking-wider text-slate-500">
                        Hourly Rate
                      </label>
                      <input
                        id="hourly-rate"
                        type="number"
                        min="0"
                        step="0.01"
                        value={hourlyRate}
                        onChange={(event) => {
                          setHourlyRate(event.target.value);
                          setRateError("");
                          setEstimatedPay(null);
                          setRateSuccess("");
                        }}
                        className={`mt-3 w-full rounded-xl border bg-white px-4 py-3 text-lg font-semibold text-slate-900 outline-none focus:ring-4 ${rateError ? "border-red-300 focus:border-red-500 focus:ring-red-100" : "border-slate-300 focus:border-blue-500 focus:ring-blue-100"}`}
                        placeholder="Enter hourly rate"
                      />
                      {rateError && <p className="mt-2 text-sm font-medium text-red-600">{rateError}</p>}
                      {rateSuccess && <p className="mt-2 text-sm font-medium text-emerald-600">{rateSuccess}</p>}
                    </div>

                    <div className="rounded-2xl border border-emerald-200 bg-gradient-to-br from-emerald-50 to-green-100/70 p-5 shadow-sm shadow-emerald-100">
                      <p className="text-xs font-bold uppercase tracking-wider text-emerald-700">Estimated Pay</p>
                      <p className="mt-3 text-3xl font-bold tracking-tight text-emerald-800">
                        {estimatedPay === null ? "Not calculated" : estimatedPay}
                      </p>
                      <p className="mt-1 text-xs text-emerald-700/80">Based on total hours and hourly rate</p>
                    </div>
                  </div>

                  <div className="flex flex-col-reverse gap-3 border-t border-slate-200 bg-slate-50/60 px-4 py-4 sm:flex-row sm:justify-end sm:px-6">
                    <Button variant="secondary" disabled={savingRate} onClick={calculatePay} className="w-full sm:w-auto">Calculate</Button>
                    <Button disabled={savingRate} onClick={saveRate} className="w-full sm:w-auto">{savingRate ? "Saving..." : "Save Rate"}</Button>
                  </div>
                </div>
              )}            </>
          )}
        </div>

        <div className="sticky bottom-0 flex justify-end border-t border-slate-200 bg-white/95 px-5 py-4 backdrop-blur sm:px-8">
          <Button variant="secondary" onClick={onClose}>Close</Button>
        </div>
      </div>
    </div>
  );
}

export default WorkHoursDetailsModal;
