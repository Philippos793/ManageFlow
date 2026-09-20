import { useEffect, useRef, useState } from "react";
import registrationRequestService from "../services/registrationRequestService.js";
import departmentService from "../services/departmentService.js";
import { getFriendlyErrorMessage, getValidationErrors } from "../utils/apiErrors.js";
import useModalAccessibility from "../hooks/useModalAccessibility.js";

function formatSubmittedDate(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unknown date";
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function RegistrationNotifications({
  embedded = false,
  open: externalOpen = false,
  onOpenChange,
  onRequestCountChange,
}) {
  const [requests, setRequests] = useState([]);
  const [open, setOpen] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [departments, setDepartments] = useState([]);
  const [selectedDepartmentId, setSelectedDepartmentId] = useState("");
  const [departmentQuery, setDepartmentQuery] = useState("");
  const [departmentOptionsOpen, setDepartmentOptionsOpen] = useState(false);
  const [departmentsLoading, setDepartmentsLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState("");
  const [actionError, setActionError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [createDepartmentOpen, setCreateDepartmentOpen] = useState(false);
  const [newDepartment, setNewDepartment] = useState({ name: "", description: "" });
  const [createDepartmentLoading, setCreateDepartmentLoading] = useState(false);
  const [createDepartmentErrors, setCreateDepartmentErrors] = useState({});
  const isOpen = embedded ? externalOpen : open;
  const reviewActionLockedRef = useRef(false);
  const createDepartmentLockedRef = useRef(false);
  const reviewDialogRef = useModalAccessibility(
    Boolean(selectedRequest && !createDepartmentOpen),
    () => {
      if (!actionLoading) setSelectedRequest(null);
    }
  );
  const createDialogRef = useModalAccessibility(createDepartmentOpen, () => {
    if (!createDepartmentLoading) setCreateDepartmentOpen(false);
  });

  useEffect(() => {
    let mounted = true;

    async function loadPendingRequests(showLoading = false) {
      if (showLoading && mounted) setLoading(true);
      try {
        const data = await registrationRequestService.getPending();
        if (mounted) {
          setRequests(Array.isArray(data) ? data : []);
          setError("");
        }
      } catch (requestError) {
        if (mounted) {
          setError(requestError?.data?.message || getFriendlyErrorMessage(
            requestError,
            "Failed to load registration requests."
          ));
        }
      } finally {
        if (mounted && showLoading) setLoading(false);
      }
    }

    loadPendingRequests(true);
    const intervalId = window.setInterval(loadPendingRequests, 10000);

    return () => {
      mounted = false;
      window.clearInterval(intervalId);
    };
  }, []);

  useEffect(() => {
    onRequestCountChange?.(requests.length);
  }, [onRequestCountChange, requests.length]);

  useEffect(() => {
    if (!selectedRequest) return undefined;
    let mounted = true;

    departmentService.getAllDepartments()
      .then((data) => {
        if (mounted) setDepartments(Array.isArray(data) ? data : []);
      })
      .catch((requestError) => {
        if (mounted) setActionError(requestError?.data?.message || getFriendlyErrorMessage(
          requestError, "Failed to load departments."
        ));
      })
      .finally(() => {
        if (mounted) setDepartmentsLoading(false);
      });

    return () => { mounted = false; };
  }, [selectedRequest]);

  useEffect(() => {
    if (!successMessage) return undefined;
    const timeoutId = window.setTimeout(() => setSuccessMessage(""), 4000);
    return () => window.clearTimeout(timeoutId);
  }, [successMessage]);

  function reviewRequest(request) {
    setDepartmentsLoading(true);
    setSelectedDepartmentId("");
    setDepartmentQuery("");
    setDepartmentOptionsOpen(false);
    setSelectedRequest(request);
    if (embedded) {
      onOpenChange?.(false);
    } else {
      setOpen(false);
    }
    setActionError("");
    setSuccessMessage("");
  }

  async function refreshPendingRequests() {
    try {
      const data = await registrationRequestService.getPending();
      setRequests(Array.isArray(data) ? data : []);
      setError("");
    } catch (requestError) {
      setError(requestError?.data?.message || getFriendlyErrorMessage(
        requestError, "Failed to refresh registration requests."
      ));
    }
  }

  async function handleAccept() {
    if (!selectedDepartmentId || reviewActionLockedRef.current) return;
    reviewActionLockedRef.current = true;
    setActionLoading("accept");
    setActionError("");
    try {
      await registrationRequestService.approve(
        selectedRequest.id, Number(selectedDepartmentId)
      );
      setSelectedRequest(null);
      setSuccessMessage("Registration request accepted successfully.");
      await refreshPendingRequests();
    } catch (requestError) {
      setActionError(requestError?.data?.message || getFriendlyErrorMessage(
        requestError, "Failed to accept registration request."
      ));
    } finally {
      reviewActionLockedRef.current = false;
      setActionLoading("");
    }
  }

  async function handleReject() {
    if (reviewActionLockedRef.current) return;
    reviewActionLockedRef.current = true;
    setActionLoading("reject");
    setActionError("");
    try {
      await registrationRequestService.reject(selectedRequest.id);
      setSelectedRequest(null);
      setSuccessMessage("Registration request rejected successfully.");
      await refreshPendingRequests();
    } catch (requestError) {
      setActionError(requestError?.data?.message || getFriendlyErrorMessage(
        requestError, "Failed to reject registration request."
      ));
    } finally {
      reviewActionLockedRef.current = false;
      setActionLoading("");
    }
  }

  function selectDepartment(department) {
    setSelectedDepartmentId(String(department.id));
    setDepartmentQuery(department.name);
    setDepartmentOptionsOpen(false);
    setActionError("");
  }

  function openCreateDepartment() {
    const normalizedQuery = departmentQuery.trim().toLowerCase();
    const existingDepartment = departments.find(
      (department) => department.name?.trim().toLowerCase() === normalizedQuery
    );
    if (existingDepartment) {
      selectDepartment(existingDepartment);
      return;
    }

    setNewDepartment({ name: departmentQuery.trim(), description: "" });
    setCreateDepartmentErrors({});
    setDepartmentOptionsOpen(false);
    setCreateDepartmentOpen(true);
  }

  async function handleCreateDepartment(event) {
    event.preventDefault();
    if (createDepartmentLockedRef.current) return;

    createDepartmentLockedRef.current = true;
    setCreateDepartmentLoading(true);
    setCreateDepartmentErrors({});
    try {
      const createdDepartment = await departmentService.createDepartment(newDepartment);
      const refreshedDepartments = await departmentService.getAllDepartments();
      const departmentList = Array.isArray(refreshedDepartments) ? refreshedDepartments : [];
      const createdId = createdDepartment?.id ?? departmentList.find(
        (department) => department.name?.toLowerCase() === newDepartment.name.trim().toLowerCase()
      )?.id;

      setDepartments(departmentList);
      if (createdId != null) setSelectedDepartmentId(String(createdId));
      setDepartmentQuery(createdDepartment?.name || newDepartment.name.trim());
      setCreateDepartmentOpen(false);
      setNewDepartment({ name: "", description: "" });
      setSuccessMessage("Department created successfully.");
    } catch (requestError) {
      const validationErrors = getValidationErrors(requestError);
      setCreateDepartmentErrors(
        Object.keys(validationErrors).length > 0
          ? validationErrors
          : {
              form: requestError?.data?.message || getFriendlyErrorMessage(
                requestError, "Failed to create department."
              ),
            }
      );
    } finally {
      createDepartmentLockedRef.current = false;
      setCreateDepartmentLoading(false);
    }
  }
  const normalizedDepartmentQuery = departmentQuery.trim().toLowerCase();
  const filteredDepartments = departments.filter((department) =>
    department.name?.toLowerCase().includes(normalizedDepartmentQuery)
  );
  const departmentExists = departments.some(
    (department) => department.name?.trim().toLowerCase() === normalizedDepartmentQuery
  );
  return (
    <div className={embedded ? "" : "relative"}>
      {successMessage && (
        <div role="status" className="fixed right-4 top-4 z-[60] rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-800 shadow-lg">
          {successMessage}
        </div>
      )}
      {!embedded && (
        <button
        type="button"
        aria-label="Registration requests"
        aria-expanded={isOpen}
        onClick={() => setOpen((current) => !current)}
        className="relative flex size-10 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 focus:outline-none focus:ring-4 focus:ring-blue-100"
      >
        <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="size-5">
          <path strokeLinecap="round" strokeLinejoin="round" d="M14.9 18a3 3 0 0 1-5.8 0m9.4-2.5H5.5c1.4-1.5 2-3.4 2-5.5a4.5 4.5 0 0 1 9 0c0 2.1.6 4 2 5.5Z" />
        </svg>
        {requests.length > 0 && (
          <span className="absolute -right-1 -top-1 flex min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-[11px] font-bold leading-5 text-white shadow-sm">
            {requests.length > 99 ? "99+" : requests.length}
          </span>
        )}
        </button>
      )}

      {isOpen && (
        <div className={embedded ? "" : "absolute right-0 z-40 mt-2 w-[min(24rem,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl shadow-slate-900/10"}>
          {!embedded && (
            <div className="border-b border-slate-200 px-4 py-3">
            <h2 className="font-bold text-slate-950">Registration requests</h2>
            <p className="mt-0.5 text-xs text-slate-500">Pending employee account requests</p>
            </div>
          )}
          <div className="max-h-96 overflow-y-auto p-2">
            {loading ? (
              <p className="px-3 py-5 text-center text-sm text-slate-500">Loading requests...</p>
            ) : error ? (
              <p className="px-3 py-5 text-center text-sm font-medium text-red-600">{error}</p>
            ) : requests.length === 0 ? (
              <p className="px-3 py-5 text-center text-sm text-slate-500">No pending requests.</p>
            ) : requests.map((request) => (
              <article key={request.id} className="rounded-xl p-3 transition-colors hover:bg-slate-50">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-bold text-slate-900">{request.firstName} {request.lastName}</p>
                    <p className="mt-1 truncate text-xs text-slate-600">{request.email}</p>
                    <p className="mt-0.5 truncate text-xs text-slate-500">@{request.username}</p>
                    <p className="mt-1.5 text-xs text-slate-400">{formatSubmittedDate(request.createdAt)}</p>
                  </div>
                  <button type="button" onClick={() => reviewRequest(request)} className="shrink-0 rounded-lg border border-blue-200 px-2.5 py-1.5 text-xs font-semibold text-blue-700 transition-colors hover:bg-blue-50">
                    Review
                  </button>
                </div>
              </article>
            ))}
          </div>
        </div>
      )}

      {selectedRequest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm" role="presentation">
          <div ref={reviewDialogRef} tabIndex="-1" role="dialog" aria-modal="true" aria-labelledby="registration-details-title" className="w-full max-w-md rounded-2xl border border-white/70 bg-white shadow-2xl outline-none">
            <div className="flex items-start justify-between border-b border-slate-200 px-5 py-4">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-blue-600">Pending request</p>
                <h2 id="registration-details-title" className="mt-1 text-xl font-bold text-slate-950">Registration details</h2>
              </div>
              <button type="button" aria-label="Close registration details" onClick={() => setSelectedRequest(null)} className="flex size-9 items-center justify-center rounded-full border border-slate-200 text-slate-500 hover:bg-slate-50 hover:text-slate-900">X</button>
            </div>
            <dl className="grid gap-4 px-5 py-5 sm:grid-cols-2">
              <div className="sm:col-span-2"><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Full name</dt><dd className="mt-1 font-semibold text-slate-900">{selectedRequest.firstName} {selectedRequest.lastName}</dd></div>
              <div className="sm:col-span-2"><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Email</dt><dd className="mt-1 break-all text-sm text-slate-700">{selectedRequest.email}</dd></div>
              <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Username</dt><dd className="mt-1 text-sm text-slate-700">{selectedRequest.username}</dd></div>
              <div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">Submitted</dt><dd className="mt-1 text-sm text-slate-700">{formatSubmittedDate(selectedRequest.createdAt)}</dd></div>
              <div className="relative sm:col-span-2">
                <label htmlFor="registration-department" className="text-xs font-semibold uppercase tracking-wide text-slate-400">Department</label>
                <input
                  id="registration-department"
                  role="combobox"
                  aria-autocomplete="list"
                  aria-controls="department-options"
                  aria-expanded={departmentOptionsOpen}
                  autoComplete="off"
                  placeholder={departmentsLoading ? "Loading departments..." : "Search or create a department"}
                  value={departmentQuery}
                  onFocus={() => setDepartmentOptionsOpen(true)}
                  onChange={(event) => {
                    setDepartmentQuery(event.target.value);
                    setSelectedDepartmentId("");
                    setDepartmentOptionsOpen(true);
                    setActionError("");
                  }}
                  disabled={departmentsLoading || Boolean(actionLoading)}
                  className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
                />
                {departmentOptionsOpen && !departmentsLoading && (
                  <div id="department-options" role="listbox" className="absolute z-10 mt-1 max-h-44 w-full overflow-y-auto rounded-xl border border-slate-200 bg-white p-1 shadow-lg">
                    {filteredDepartments.map((department) => (
                      <button key={department.id} type="button" role="option" aria-selected={String(department.id) === selectedDepartmentId} onClick={() => selectDepartment(department)} className="block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-700 hover:bg-blue-50 hover:text-blue-700">
                        {department.name}
                      </button>
                    ))}
                    {normalizedDepartmentQuery && !departmentExists && (
                      <button type="button" onClick={openCreateDepartment} className="block w-full rounded-lg px-3 py-2 text-left text-sm font-semibold text-blue-700 hover:bg-blue-50">
                        Create &quot;{departmentQuery.trim()}&quot;
                      </button>
                    )}
                    {filteredDepartments.length === 0 && (!normalizedDepartmentQuery || departmentExists) && (
                      <p className="px-3 py-2 text-sm text-slate-500">No departments found.</p>
                    )}
                  </div>
                )}
              </div>
              {actionError && <div role="alert" className="sm:col-span-2 rounded-lg bg-red-50 px-3 py-2 text-sm font-medium text-red-700">{actionError}</div>}            </dl>
            <div className="flex flex-col-reverse gap-2 border-t border-slate-200 px-5 py-4 sm:flex-row sm:justify-end">
              <button type="button" disabled={Boolean(actionLoading)} onClick={() => setSelectedRequest(null)} className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">Close</button>
              <button type="button" disabled={Boolean(actionLoading)} onClick={handleReject} className="rounded-lg border border-red-200 px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50">{actionLoading === "reject" ? "Rejecting..." : "Reject"}</button>
              <button type="button" disabled={!selectedDepartmentId || Boolean(actionLoading) || departmentsLoading} onClick={handleAccept} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-300">{actionLoading === "accept" ? "Accepting..." : "Accept"}</button>
            </div>
          </div>
        </div>
      )}
      {selectedRequest && createDepartmentOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/60 p-4" role="presentation">
          <div ref={createDialogRef} tabIndex="-1" role="dialog" aria-modal="true" aria-labelledby="create-department-title" className="w-full max-w-sm rounded-2xl bg-white shadow-2xl outline-none">
            <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
              <h3 id="create-department-title" className="text-lg font-bold text-slate-950">Create new department</h3>
              <button type="button" aria-label="Close create department" disabled={createDepartmentLoading} onClick={() => setCreateDepartmentOpen(false)} className="flex size-8 items-center justify-center rounded-full border border-slate-200 text-slate-500 hover:bg-slate-50 disabled:opacity-50">X</button>
            </div>
            <form onSubmit={handleCreateDepartment} className="space-y-4 p-5">
              <div>
                <label htmlFor="new-department-name" className="mb-1.5 block text-sm font-semibold text-slate-700">Department Name</label>
                <input id="new-department-name" required value={newDepartment.name} onChange={(event) => { setNewDepartment((current) => ({ ...current, name: event.target.value })); setCreateDepartmentErrors((current) => ({ ...current, name: undefined })); }} disabled={createDepartmentLoading} className="w-full rounded-xl border border-slate-300 px-3 py-2.5 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />
                {createDepartmentErrors.name && <p className="mt-1.5 text-sm font-medium text-red-600">{createDepartmentErrors.name}</p>}
              </div>
              <div>
                <label htmlFor="new-department-description" className="mb-1.5 block text-sm font-semibold text-slate-700">Description</label>
                <textarea id="new-department-description" rows="3" value={newDepartment.description} onChange={(event) => setNewDepartment((current) => ({ ...current, description: event.target.value }))} disabled={createDepartmentLoading} className="w-full resize-none rounded-xl border border-slate-300 px-3 py-2.5 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />
              </div>
              {createDepartmentErrors.form && <div role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm font-medium text-red-700">{createDepartmentErrors.form}</div>}
              <div className="flex justify-end gap-2 pt-1">
                <button type="button" disabled={createDepartmentLoading} onClick={() => setCreateDepartmentOpen(false)} className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50">Cancel</button>
                <button type="submit" disabled={createDepartmentLoading} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-400">{createDepartmentLoading ? "Creating..." : "Create Department"}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default RegistrationNotifications;
