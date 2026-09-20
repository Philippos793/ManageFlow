import { useRef, useState } from "react";
import Button from "./ui/Button.jsx";
import FeedbackMessage from "./ui/FeedbackMessage.jsx";
import taskService from "../services/taskService.js";
import { getFriendlyErrorMessage, getValidationErrors } from "../utils/apiErrors.js";
import useModalAccessibility from "../hooks/useModalAccessibility.js";

function TaskRequestChangesModal({ open, task, onClose, onRequested }) {
  const [form, setForm] = useState({
    feedback: "",
    additionalTimeDays: "0",
    additionalTimeHours: "0",
    additionalTimeMinutes: "0",
    checklistItems: [],
  });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const submitLockedRef = useRef(false);
  const dialogRef = useModalAccessibility(open, onClose, !saving);
  const hasChecklist = Array.isArray(task?.checklistItems) && task.checklistItems.length > 0;

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({
      ...current,
      [field]: undefined,
      additionalTime: undefined,
      checklistItems: undefined,
      form: undefined,
    }));
  }

  function addChecklistItem() {
    setForm((current) => ({
      ...current,
      checklistItems: [...current.checklistItems, ""],
    }));
    setErrors((current) => ({ ...current, checklistItems: undefined, form: undefined }));
  }

  function updateChecklistItem(index, value) {
    setForm((current) => ({
      ...current,
      checklistItems: current.checklistItems.map((item, itemIndex) => (
        itemIndex === index ? value : item
      )),
    }));
    setErrors((current) => ({ ...current, checklistItems: undefined, form: undefined }));
  }

  function removeChecklistItem(index) {
    setForm((current) => ({
      ...current,
      checklistItems: current.checklistItems.filter((_, itemIndex) => itemIndex !== index),
    }));
    setErrors((current) => ({ ...current, checklistItems: undefined, form: undefined }));
  }

  function getAdditionalTimeMinutes() {
    return (Number(form.additionalTimeDays) * 24 * 60)
      + (Number(form.additionalTimeHours) * 60)
      + Number(form.additionalTimeMinutes);
  }

  function validateForm() {
    const validationErrors = {};
    const days = Number(form.additionalTimeDays);
    const hours = Number(form.additionalTimeHours);
    const minutes = Number(form.additionalTimeMinutes);

    if (!form.feedback.trim()) {
      validationErrors.feedback = "Feedback is required.";
    }
    if (!Number.isInteger(days) || days < 0) {
      validationErrors.additionalTime = "Days must be 0 or greater.";
    } else if (!Number.isInteger(hours) || hours < 0 || hours > 23) {
      validationErrors.additionalTime = "Hours must be between 0 and 23.";
    } else if (!Number.isInteger(minutes) || minutes < 0 || minutes > 59) {
      validationErrors.additionalTime = "Minutes must be between 0 and 59.";
    } else if (getAdditionalTimeMinutes() <= 0) {
      validationErrors.additionalTime = "Additional time must be greater than 0.";
    }
    if (hasChecklist && form.checklistItems.length === 0) {
      validationErrors.checklistItems = "Add at least one checklist item for the requested changes.";
    } else if (form.checklistItems.some((item) => !item.trim())) {
      validationErrors.checklistItems = "Checklist items cannot be blank.";
    }

    setErrors(validationErrors);
    return Object.keys(validationErrors).length === 0;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitLockedRef.current || !task || !validateForm()) return;

    submitLockedRef.current = true;
    setSaving(true);
    setErrors({});
    try {
      const updatedTask = await taskService.requestChanges(task.id, {
        feedback: form.feedback.trim(),
        additionalTimeMinutes: getAdditionalTimeMinutes(),
        ...(hasChecklist
          ? { checklistItems: form.checklistItems.map((description) => ({ description: description.trim() })) }
          : {}),
      });
      await onRequested?.(updatedTask);
      onClose();
    } catch (requestError) {
      const validationErrors = getValidationErrors(requestError);
      if (validationErrors.additionalTimeMinutes) {
        validationErrors.additionalTime = validationErrors.additionalTimeMinutes;
      }
      setErrors(Object.keys(validationErrors).length > 0
        ? validationErrors
        : { form: getFriendlyErrorMessage(requestError, "Failed to request changes.") });
    } finally {
      submitLockedRef.current = false;
      setSaving(false);
    }
  }

  if (!open || !task) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-950/60 p-4 backdrop-blur-sm" role="presentation">
      <div ref={dialogRef} tabIndex="-1" role="dialog" aria-modal="true" aria-labelledby="request-changes-title" className="my-auto w-full max-w-2xl rounded-3xl bg-white shadow-2xl outline-none">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-5 sm:px-7">
          <div className="min-w-0">
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-600">Work management</p>
            <h2 id="request-changes-title" className="mt-2 break-words text-2xl font-bold text-slate-950">Request Changes</h2>
            <p className="mt-1 break-words text-sm text-slate-500">Explain the required changes and set a new deadline.</p>
          </div>
          <button type="button" aria-label="Close request changes" disabled={saving} onClick={onClose} className="flex size-9 shrink-0 items-center justify-center rounded-full border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50">X</button>
        </div>

        <form onSubmit={handleSubmit} className="max-h-[70vh] space-y-5 overflow-y-auto px-5 py-6 sm:px-7">
          <div>
            <label htmlFor="request-changes-feedback" className="mb-1.5 block text-sm font-semibold text-slate-700">Feedback</label>
            <textarea id="request-changes-feedback" rows="5" value={form.feedback} disabled={saving} onChange={(event) => updateField("feedback", event.target.value)} placeholder="Describe the changes that are needed." aria-invalid={Boolean(errors.feedback)} aria-describedby={errors.feedback ? "request-changes-feedback-error" : undefined} className="w-full resize-y rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
            {errors.feedback && <p id="request-changes-feedback-error" className="mt-1.5 text-sm font-medium text-red-600">{errors.feedback}</p>}
          </div>

          <fieldset>
            <legend className="mb-1.5 block text-sm font-semibold text-slate-700">Additional Time</legend>
            <div className="grid gap-3 sm:grid-cols-3">
              <div>
                <label htmlFor="request-changes-days" className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">Days</label>
                <input id="request-changes-days" type="number" min="0" step="1" value={form.additionalTimeDays} disabled={saving} onChange={(event) => updateField("additionalTimeDays", event.target.value)} aria-invalid={Boolean(errors.additionalTime)} aria-describedby={errors.additionalTime ? "request-changes-time-error" : undefined} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
              </div>
              <div>
                <label htmlFor="request-changes-hours" className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">Hours</label>
                <input id="request-changes-hours" type="number" min="0" max="23" step="1" value={form.additionalTimeHours} disabled={saving} onChange={(event) => updateField("additionalTimeHours", event.target.value)} aria-invalid={Boolean(errors.additionalTime)} aria-describedby={errors.additionalTime ? "request-changes-time-error" : undefined} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
              </div>
              <div>
                <label htmlFor="request-changes-minutes" className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">Minutes</label>
                <input id="request-changes-minutes" type="number" min="0" max="59" step="1" value={form.additionalTimeMinutes} disabled={saving} onChange={(event) => updateField("additionalTimeMinutes", event.target.value)} aria-invalid={Boolean(errors.additionalTime)} aria-describedby={errors.additionalTime ? "request-changes-time-error" : undefined} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
              </div>
            </div>
            {errors.additionalTime && <p id="request-changes-time-error" className="mt-1.5 text-sm font-medium text-red-600">{errors.additionalTime}</p>}
          </fieldset>

          {hasChecklist && (
            <fieldset className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4 sm:p-5">
              <legend className="px-1 text-sm font-bold text-slate-800">Checklist Corrections</legend>
              <p className="mt-1 text-sm text-slate-500">Add at least one new incomplete item for the requested changes.</p>
              <div className="mt-3 space-y-3">
                {form.checklistItems.map((item, index) => (
                  <div key={`new-${index}`} className="flex items-start gap-2">
                    <input aria-label={`New checklist item ${index + 1}`} value={item} disabled={saving} onChange={(event) => updateChecklistItem(index, event.target.value)} aria-invalid={Boolean(errors.checklistItems)} aria-describedby={errors.checklistItems ? "request-changes-checklist-error" : undefined} className="min-w-0 flex-1 rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                    <button type="button" aria-label={`Remove new checklist item ${index + 1}`} disabled={saving} onClick={() => removeChecklistItem(index)} className="rounded-xl border border-red-200 bg-white px-3 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50">Remove</button>
                  </div>
                ))}
                {errors.checklistItems && <p id="request-changes-checklist-error" className="text-sm font-medium text-red-600">{errors.checklistItems}</p>}
                <button type="button" disabled={saving} onClick={addChecklistItem} className="rounded-xl border border-blue-200 bg-white px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50">+ Add Checklist Item</button>
              </div>
            </fieldset>
          )}

          {errors.form && <FeedbackMessage>{errors.form}</FeedbackMessage>}
        </form>

        <div className="flex flex-col-reverse gap-3 border-t border-slate-200 bg-slate-50/60 px-5 py-4 sm:flex-row sm:justify-end sm:px-7">
          <Button variant="secondary" disabled={saving} onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving} onClick={handleSubmit}>{saving ? "Requesting..." : "Request Changes"}</Button>
        </div>
      </div>
    </div>
  );
}

export default TaskRequestChangesModal;
