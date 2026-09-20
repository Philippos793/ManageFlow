import { useEffect, useState } from "react";
import Button from "./ui/Button.jsx";
import departmentService from "../services/departmentService.js";

function EmployeeForm({
  values,
  errors,
  onChange,
  onSubmit,
  onCancel,
  submitLabel,
  submitting,
}) {
  const [departments, setDepartments] = useState([]);
  const [departmentsLoading, setDepartmentsLoading] = useState(true);
  const [departmentsError, setDepartmentsError] = useState("");

  async function loadDepartments() {
    setDepartmentsLoading(true);
    setDepartmentsError("");
    try {
      setDepartments(await departmentService.getAllDepartments());
    } catch {
      setDepartmentsError("Unable to load departments. Please try again.");
    } finally {
      setDepartmentsLoading(false);
    }
  }

  useEffect(() => {
    let cancelled = false;

    departmentService
      .getAllDepartments()
      .then((loadedDepartments) => {
        if (!cancelled) {
          setDepartments(loadedDepartments);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDepartmentsError("Unable to load departments. Please try again.");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setDepartmentsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const fields = [
    { name: "firstName", label: "First Name", placeholder: "e.g. Maria", type: "text", autoComplete: "given-name" },
    { name: "lastName", label: "Last Name", placeholder: "e.g. Nikolaou", type: "text", autoComplete: "family-name" },
    { name: "email", label: "Email Address", placeholder: "e.g. maria@company.com", type: "email", autoComplete: "email" },
  ];

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      <div className="grid gap-5 sm:grid-cols-2">
        {fields.map((field) => (
          <div key={field.name}>
            <label htmlFor={field.name} className="mb-2 block text-sm font-semibold text-slate-700">
              {field.label}
            </label>
            <input
              id={field.name}
              name={field.name}
              type={field.type}
              autoComplete={field.autoComplete}
              required
              placeholder={field.placeholder}
              value={values[field.name]}
              onChange={onChange}
              aria-invalid={Boolean(errors[field.name])}
              aria-describedby={errors[field.name] ? `${field.name}-error` : undefined}
              className={`w-full rounded-xl border bg-white px-4 py-3 text-slate-900 outline-none transition placeholder:text-slate-400 focus:ring-4 ${
                errors[field.name]
                  ? "border-red-400 focus:border-red-500 focus:ring-red-100"
                  : "border-slate-300 focus:border-blue-500 focus:ring-blue-100"
              }`}
            />
            {errors[field.name] && (
              <p id={`${field.name}-error`} className="mt-2 text-sm font-medium text-red-600">
                {errors[field.name]}
              </p>
            )}
          </div>
        ))}
        <div>
          <label htmlFor="department" className="mb-2 block text-sm font-semibold text-slate-700">
            Department
          </label>
          <select
            id="department"
            name="department"
            required
            value={values.department}
            onChange={onChange}
            disabled={submitting || departmentsLoading || Boolean(departmentsError) || departments.length === 0}
            aria-invalid={Boolean(errors.department)}
            aria-describedby={errors.department ? "department-error" : undefined}
            className={`w-full rounded-xl border bg-white px-4 py-3 text-slate-900 outline-none transition focus:ring-4 disabled:cursor-not-allowed disabled:bg-slate-100 ${
              errors.department
                ? "border-red-400 focus:border-red-500 focus:ring-red-100"
                : "border-slate-300 focus:border-blue-500 focus:ring-blue-100"
            }`}
          >
            <option value="">
              {departmentsLoading ? "Loading departments..." : "Select a department"}
            </option>
            {departments.map((department) => (
              <option key={department.id} value={department.name}>
                {department.name}
              </option>
            ))}
          </select>
          {errors.department && (
            <p id="department-error" className="mt-2 text-sm font-medium text-red-600">
              {errors.department}
            </p>
          )}
          {departmentsError && (
            <div className="mt-2 flex items-center gap-3">
              <p className="text-sm font-medium text-red-600">{departmentsError}</p>
              <button
                type="button"
                onClick={loadDepartments}
                className="text-sm font-semibold text-blue-700 hover:text-blue-800"
              >
                Try again
              </button>
            </div>
          )}
          {!departmentsLoading && !departmentsError && departments.length === 0 && (
            <p className="mt-2 text-sm text-slate-600">
              No departments are available. Create one in Department Management first.
            </p>
          )}
        </div>
      </div>

      <div className="flex flex-col-reverse gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-end">
        <Button variant="secondary" disabled={submitting} onClick={onCancel}>
          Cancel
        </Button>
        <Button
          type="submit"
          disabled={submitting || departmentsLoading || Boolean(departmentsError) || departments.length === 0}
        >
          {submitting ? "Saving employee..." : submitLabel}
        </Button>
      </div>
    </form>
  );
}

export default EmployeeForm;

