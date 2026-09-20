import { useEffect, useRef, useState } from "react";
import Button from "./ui/Button.jsx";
import FeedbackMessage from "./ui/FeedbackMessage.jsx";
import LoadingSpinner from "./ui/LoadingSpinner.jsx";
import employeeService from "../services/employeeService.js";
import taskService from "../services/taskService.js";
import { getFriendlyErrorMessage, getValidationErrors } from "../utils/apiErrors.js";
import useModalAccessibility from "../hooks/useModalAccessibility.js";

const initialForm = {
  employeeId: "",
  title: "",
  description: "",
  timeAllowedDays: "0",
  timeAllowedHours: "0",
  timeAllowedMinutes: "0",
  checklistItems: [],
};

function TaskCreateModal({ open, onClose, onCreated }) {
  const [form, setForm] = useState(initialForm);
  const [employees, setEmployees] = useState([]);
  const [loadingEmployees, setLoadingEmployees] = useState(true);
  const [employeeError, setEmployeeError] = useState("");
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const submitLockedRef = useRef(false);
  const dialogRef = useModalAccessibility(open, onClose, !saving);

  useEffect(() => {
    if (!open) return undefined;

    let mounted = true;
    employeeService.getAllEmployees()
      .then((data) => {
        if (mounted) setEmployees(Array.isArray(data) ? data : []);
      })
      .catch((requestError) => {
        if (mounted) {
          setEmployees([]);
          setEmployeeError(getFriendlyErrorMessage(
            requestError,
            "Failed to load employees."
          ));
        }
      })
      .finally(() => {
        if (mounted) setLoadingEmployees(false);
      });

    return () => { mounted = false; };
  }, [open]);

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({
      ...current,
      [field]: undefined,
      timeAllowed: undefined,
      form: undefined,
    }));
  }

  function addChecklistItem() {
    setForm((current) => ({
      ...current,
      checklistItems: [...current.checklistItems, ""],
    }));
    setErrors((current) => ({ ...current, checklistItems: undefined }));
  }

  function updateChecklistItem(index, description) {
    setForm((current) => ({
      ...current,
      checklistItems: current.checklistItems.map((item, itemIndex) => (
        itemIndex === index ? description : item
      )),
    }));
    setErrors((current) => ({ ...current, checklistItems: undefined, form: undefined }));
  }

  function removeChecklistItem(index) {
    setForm((current) => ({
      ...current,
      checklistItems: current.checklistItems.filter((_, itemIndex) => itemIndex !== index),
    }));
  }

  function validateForm() {
    const validationErrors = {};
    const days = Number(form.timeAllowedDays);
    const hours = Number(form.timeAllowedHours);
    const minutes = Number(form.timeAllowedMinutes);
    const totalMinutes = (days * 24 * 60) + (hours * 60) + minutes;

    if (!form.employeeId) validationErrors.employeeId = "Please select an employee.";
    if (!form.title.trim()) validationErrors.title = "Title is required.";
    else if (form.title.trim().length > 200) {
      validationErrors.title = "Title must not exceed 200 characters.";
    }
    if (!Number.isInteger(days) || days < 0) {
      validationErrors.timeAllowed = "Days must be 0 or greater.";
    } else if (!Number.isInteger(hours) || hours < 0 || hours > 23) {
      validationErrors.timeAllowed = "Hours must be between 0 and 23.";
    } else if (!Number.isInteger(minutes) || minutes < 0 || minutes > 59) {
      validationErrors.timeAllowed = "Minutes must be between 0 and 59.";
    } else if (totalMinutes <= 0) {
      validationErrors.timeAllowed = "Time allowed must be greater than 0.";
    }
    if (form.checklistItems.some((item) => !item.trim())) {
      validationErrors.checklistItems = "Checklist items cannot be blank.";
    }

    setErrors(validationErrors);
    return Object.keys(validationErrors).length === 0;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitLockedRef.current || !validateForm()) return;

    submitLockedRef.current = true;
    setSaving(true);
    setErrors({});
    try {
      const createdTask = await taskService.createTask({
        employeeId: Number(form.employeeId),
        title: form.title.trim(),
        description: form.description.trim() || null,
        timeAllowedMinutes: (Number(form.timeAllowedDays) * 24 * 60)
          + (Number(form.timeAllowedHours) * 60)
          + Number(form.timeAllowedMinutes),
        checklistItems: form.checklistItems.map((description) => ({ description: description.trim() })),
      });
      await onCreated?.(createdTask);
      onClose();
    } catch (requestError) {
      const validationErrors = getValidationErrors(requestError);
      if (validationErrors.timeAllowedMinutes) {
        validationErrors.timeAllowed = validationErrors.timeAllowedMinutes;
      }
      setErrors(Object.keys(validationErrors).length > 0
        ? validationErrors
        : { form: getFriendlyErrorMessage(requestError, "Failed to assign task.") });
    } finally {
      submitLockedRef.current = false;
      setSaving(false);
    }
  }

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-950/60 p-4 backdrop-blur-sm" role="presentation">
      <div ref={dialogRef} tabIndex="-1" role="dialog" aria-modal="true" aria-labelledby="assign-task-title" className="my-auto w-full max-w-2xl rounded-3xl bg-white shadow-2xl outline-none">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-5 sm:px-7">
          <div className="min-w-0">
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">Work management</p>
            <h2 id="assign-task-title" className="mt-2 break-words text-2xl font-bold text-slate-950">Assign Task</h2>
            <p className="mt-1 break-words text-sm text-slate-500">Assign work, time allowed, and optional checklist items.</p>
          </div>
          <button type="button" aria-label="Close assign task" disabled={saving} onClick={onClose} className="flex size-9 shrink-0 items-center justify-center rounded-full border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50">X</button>
        </div>

        <form onSubmit={handleSubmit} className="max-h-[70vh] space-y-5 overflow-y-auto px-5 py-6 sm:px-7">
          {loadingEmployees ? <LoadingSpinner compact label="Loading employees..." /> : (
            <div>
              <label htmlFor="task-employee" className="mb-1.5 block text-sm font-semibold text-slate-700">Employee</label>
              <select id="task-employee" value={form.employeeId} disabled={saving || Boolean(employeeError)} onChange={(event) => updateField("employeeId", event.target.value)} aria-invalid={Boolean(errors.employeeId)} aria-describedby={errors.employeeId ? "task-employee-error" : undefined} className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100">
                <option value="">Select an active employee</option>
                {employees.map((employee) => (
                  <option key={employee.id} value={employee.id}>
                    {employee.firstName} {employee.lastName} — {employee.department}
                  </option>
                ))}
              </select>
              {employeeError && <FeedbackMessage>{employeeError}</FeedbackMessage>}
              {errors.employeeId && <p id="task-employee-error" className="mt-1.5 text-sm font-medium text-red-600">{errors.employeeId}</p>}
            </div>
          )}

          <div>
            <label htmlFor="task-title" className="mb-1.5 block text-sm font-semibold text-slate-700">Title</label>
            <input id="task-title" value={form.title} maxLength="200" disabled={saving} onChange={(event) => updateField("title", event.target.value)} placeholder="e.g. Prepare monthly report" aria-invalid={Boolean(errors.title)} aria-describedby={errors.title ? "task-title-error" : undefined} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
            {errors.title && <p id="task-title-error" className="mt-1.5 text-sm font-medium text-red-600">{errors.title}</p>}
          </div>

          <div>
            <label htmlFor="task-description" className="mb-1.5 block text-sm font-semibold text-slate-700">Description</label>
            <textarea id="task-description" rows="4" value={form.description} disabled={saving} onChange={(event) => updateField("description", event.target.value)} placeholder="Add instructions or context for this task." className="w-full resize-y rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
          </div>

          <fieldset>
            <legend className="mb-1.5 block text-sm font-semibold text-slate-700">Time Allowed</legend>
            <div className="grid gap-3 sm:grid-cols-3">
              <div>
                <label htmlFor="task-time-allowed-days" className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">Days</label>
                <input id="task-time-allowed-days" type="number" min="0" step="1" value={form.timeAllowedDays} disabled={saving} onChange={(event) => updateField("timeAllowedDays", event.target.value)} aria-invalid={Boolean(errors.timeAllowed)} aria-describedby={errors.timeAllowed ? "task-time-allowed-error" : undefined} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
              </div>
              <div>
                <label htmlFor="task-time-allowed-hours" className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">Hours</label>
                <input id="task-time-allowed-hours" type="number" min="0" max="23" step="1" value={form.timeAllowedHours} disabled={saving} onChange={(event) => updateField("timeAllowedHours", event.target.value)} aria-invalid={Boolean(errors.timeAllowed)} aria-describedby={errors.timeAllowed ? "task-time-allowed-error" : undefined} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
              </div>
              <div>
                <label htmlFor="task-time-allowed-minutes" className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">Minutes</label>
                <input id="task-time-allowed-minutes" type="number" min="0" max="59" step="1" value={form.timeAllowedMinutes} disabled={saving} onChange={(event) => updateField("timeAllowedMinutes", event.target.value)} aria-invalid={Boolean(errors.timeAllowed)} aria-describedby={errors.timeAllowed ? "task-time-allowed-error" : undefined} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
              </div>
            </div>
            {errors.timeAllowed && <p id="task-time-allowed-error" className="mt-1.5 text-sm font-medium text-red-600">{errors.timeAllowed}</p>}
          </fieldset>

          <fieldset className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4 sm:p-5">
            <legend className="px-1 text-sm font-bold text-slate-800">Checklist Items <span className="font-normal text-slate-500">(optional)</span></legend>
            <div className="mt-3 space-y-3">
              {form.checklistItems.map((item, index) => (
                <div key={`checklist-item-${index}`} className="flex items-start gap-2">
                  <input aria-label={`Checklist item ${index + 1}`} value={item} disabled={saving} onChange={(event) => updateChecklistItem(index, event.target.value)} placeholder={`Checklist item ${index + 1}`} aria-invalid={Boolean(errors.checklistItems)} aria-describedby={errors.checklistItems ? "task-checklist-items-error" : undefined} className="min-w-0 flex-1 rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                  <button type="button" aria-label={`Remove checklist item ${index + 1}`} disabled={saving} onClick={() => removeChecklistItem(index)} className="rounded-xl border border-red-200 bg-white px-3 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50">Remove</button>
                </div>
              ))}
              {errors.checklistItems && <p id="task-checklist-items-error" className="text-sm font-medium text-red-600">{errors.checklistItems}</p>}
              <button type="button" disabled={saving} onClick={addChecklistItem} className="rounded-xl border border-blue-200 bg-white px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50">+ Add Checklist Item</button>
            </div>
          </fieldset>

          {errors.form && <FeedbackMessage>{errors.form}</FeedbackMessage>}
        </form>

        <div className="flex flex-col-reverse gap-3 border-t border-slate-200 bg-slate-50/60 px-5 py-4 sm:flex-row sm:justify-end sm:px-7">
          <Button variant="secondary" disabled={saving} onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving || loadingEmployees || Boolean(employeeError)} onClick={handleSubmit}>{saving ? "Assigning..." : "Assign Task"}</Button>
        </div>
      </div>
    </div>
  );
}

export default TaskCreateModal;
