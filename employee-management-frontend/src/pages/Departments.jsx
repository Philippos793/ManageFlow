import { useCallback, useEffect, useRef, useState } from "react";
import authService from "../services/authService";
import departmentService from "../services/departmentService";
import { getFriendlyErrorMessage, getValidationErrors } from "../utils/apiErrors.js";
import Button from "../components/ui/Button.jsx";
import ConfirmDialog from "../components/ui/ConfirmDialog.jsx";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import LoadingSpinner from "../components/ui/LoadingSpinner.jsx";
import PageTitle from "../components/ui/PageTitle.jsx";

const emptyForm = { name: "", description: "" };

function Departments() {
  const isAdmin = authService.getRole() === "ADMIN";
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [formMode, setFormMode] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [departmentToDelete, setDepartmentToDelete] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loadFailed, setLoadFailed] = useState(false);
  const submitLockedRef = useRef(false);
  const deleteLockedRef = useRef(false);

  const loadDepartments = useCallback(() => {
    setLoading(true);
    setLoadFailed(false);
    setError("");
    departmentService
      .getAllDepartments()
      .then((data) => {
        setDepartments(data);
        setLoadFailed(false);
      })
      .catch((requestError) => {
        setDepartments([]);
        setLoadFailed(true);
        setError(errorMessage(requestError, "Failed to load departments."));
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    let mounted = true;
    departmentService.getAllDepartments()
      .then((data) => {
        if (mounted) setDepartments(data);
      })
      .catch((requestError) => {
        if (!mounted) return;
        setDepartments([]);
        setLoadFailed(true);
        setError(errorMessage(requestError, "Failed to load departments."));
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => { mounted = false; };
  }, []);

  function startCreate() {
    setFormMode("create");
    setEditingId(null);
    setForm(emptyForm);
    setFieldErrors({});
    setError("");
    setSuccess("");
  }

  function startEdit(department) {
    setFormMode("edit");
    setEditingId(department.id);
    setForm({ name: department.name, description: department.description || "" });
    setFieldErrors({});
    setError("");
    setSuccess("");
  }

  function closeForm() {
    setFormMode(null);
    setEditingId(null);
    setForm(emptyForm);
    setFieldErrors({});
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitLockedRef.current) return;
    submitLockedRef.current = true;
    setSaving(true);
    setError("");
    setSuccess("");
    setFieldErrors({});

    try {
      const savedDepartment = formMode === "edit"
        ? await departmentService.updateDepartment(editingId, form)
        : await departmentService.createDepartment(form);

      setDepartments((current) => formMode === "edit"
        ? current.map((department) => department.id === savedDepartment.id ? savedDepartment : department)
        : [...current, savedDepartment]);
      setSuccess(`Department ${formMode === "edit" ? "updated" : "created"} successfully.`);
      closeForm();
    } catch (requestError) {
      setFieldErrors(getValidationErrors(requestError));
      setError(errorMessage(requestError, "Failed to save department."));
    } finally {
      submitLockedRef.current = false;
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!departmentToDelete || deleteLockedRef.current) return;
    deleteLockedRef.current = true;
    setDeleting(true);
    setError("");
    setSuccess("");

    try {
      await departmentService.deleteDepartment(departmentToDelete.id);
      setDepartments((current) => current.filter(({ id }) => id !== departmentToDelete.id));
      setDepartmentToDelete(null);
      setSuccess("Department deleted successfully.");
    } catch (requestError) {
      setDepartmentToDelete(null);
      setError(errorMessage(requestError, "Failed to delete department."));
    } finally {
      deleteLockedRef.current = false;
      setDeleting(false);
    }
  }

  if (loading) {
    return <div className="rounded-2xl border border-slate-200 bg-white"><LoadingSpinner label="Loading departments..." /></div>;
  }

  return (
    <section>
      <PageTitle
        eyebrow="Organization"
        title="Departments"
        description="View the departments available across the organization."
        action={isAdmin ? <Button onClick={startCreate}>Add Department</Button> : null}
      />

      <div className="mt-6 space-y-3">
        <FeedbackMessage type="success">{success}</FeedbackMessage>
        <FeedbackMessage>{error}</FeedbackMessage>
        {loadFailed && <Button variant="secondary" onClick={loadDepartments}>Retry</Button>}
      </div>

      {isAdmin && formMode && (
        <form onSubmit={handleSubmit} className="mt-8 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
          <h2 className="text-lg font-bold text-slate-950">{formMode === "edit" ? "Edit Department" : "New Department"}</h2>
          <div className="mt-5 grid gap-5 sm:grid-cols-2">
            <div>
              <label htmlFor="department-name" className="mb-2 block text-sm font-semibold text-slate-700">Name</label>
              <input id="department-name" required value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />
              {fieldErrors.name && <p className="mt-2 text-sm text-red-600">{fieldErrors.name}</p>}
            </div>
            <div>
              <label htmlFor="department-description" className="mb-2 block text-sm font-semibold text-slate-700">Description</label>
              <input id="department-description" value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />
            </div>
          </div>
          <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <Button variant="secondary" disabled={saving} onClick={closeForm}>Cancel</Button>
            <Button type="submit" disabled={saving}>{saving ? "Saving department..." : "Save Department"}</Button>
          </div>
        </form>
      )}

      <div className="mt-8 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50"><tr>
              <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">Name</th>
              <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">Description</th>
              {isAdmin && <th className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">Actions</th>}
            </tr></thead>
            <tbody className="divide-y divide-slate-100">
              {departments.map((department, index) => (
                <tr key={department.id} className={index % 2 ? "bg-slate-50/50" : "bg-white"}>
                  <td className="px-5 py-4 font-semibold text-slate-900">{department.name}</td>
                  <td className="px-5 py-4 text-sm text-slate-600">{department.description || "No description"}</td>
                  {isAdmin && <td className="whitespace-nowrap px-5 py-4">
                    <button type="button" onClick={() => startEdit(department)} className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100">Edit</button>
                    <button type="button" onClick={() => setDepartmentToDelete(department)} className="rounded-lg px-3 py-2 text-sm font-semibold text-red-600 hover:bg-red-50">Delete</button>
                  </td>}
                </tr>
              ))}
              {departments.length === 0 && !loadFailed && <tr><td colSpan={isAdmin ? 3 : 2} className="px-6 py-16 text-center text-sm text-slate-500">No departments yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmDialog
        open={Boolean(departmentToDelete)}
        title="Delete Department?"
        message="This action cannot be undone. Departments with employees cannot be deleted."
        loading={deleting}
        onCancel={() => setDepartmentToDelete(null)}
        onConfirm={confirmDelete}
      />
    </section>
  );
}

function errorMessage(error, fallback) {
  if ([400, 404, 409].includes(error?.status) && error?.data?.message) {
    return error.data.message;
  }

  return getFriendlyErrorMessage(error, fallback);
}

export default Departments;
