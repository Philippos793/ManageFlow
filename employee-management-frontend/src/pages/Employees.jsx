import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import ConfirmDialog from "../components/ui/ConfirmDialog.jsx";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import LoadingSpinner from "../components/ui/LoadingSpinner.jsx";
import PageTitle from "../components/ui/PageTitle.jsx";
import employeeService from "../services/employeeService";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";
import authService from "../services/authService";
import Button from "../components/ui/Button.jsx";

function Employees() {
  const location = useLocation();
  const navigate = useNavigate();
  const role = authService.getRole();
  const isAuthenticated = authService.isAuthenticated();
  const username = authService.getUsername();
  const canEdit = role === "ADMIN";
  const canDelete = role === "ADMIN";
  const [employees, setEmployees] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [employeeToDelete, setEmployeeToDelete] = useState(null);
  const [error, setError] = useState("");
  const [loadFailed, setLoadFailed] = useState(false);
  const [success, setSuccess] = useState(location.state?.success || "");
  const [showWelcome, setShowWelcome] = useState(
    Boolean(location.state?.showWelcome && isAuthenticated && username)
  );
  const deleteLockedRef = useRef(false);

  useEffect(() => {
    if (location.state?.success || location.state?.showWelcome) {
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [
    location.pathname,
    location.state?.showWelcome,
    location.state?.success,
    navigate,
  ]);

  useEffect(() => {
    if (!showWelcome) return undefined;

    const timer = window.setTimeout(() => setShowWelcome(false), 4000);
    return () => window.clearTimeout(timer);
  }, [showWelcome]);

  const loadEmployees = useCallback(() => {
    setLoading(true);
    setLoadFailed(false);
    setError("");
    employeeService
      .getAllEmployees()
      .then((data) => {
        setEmployees(data);
        setLoadFailed(false);
      })
      .catch((fetchError) => {
        setEmployees([]);
        setLoadFailed(true);
        setError(
          getFriendlyErrorMessage(fetchError, "Failed to load employees.")
        );
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    let mounted = true;
    employeeService.getAllEmployees()
      .then((data) => {
        if (mounted) setEmployees(data);
      })
      .catch((fetchError) => {
        if (!mounted) return;
        setEmployees([]);
        setLoadFailed(true);
        setError(getFriendlyErrorMessage(fetchError, "Failed to load employees."));
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => { mounted = false; };
  }, []);

  const filteredEmployees = useMemo(() => {
    const search = searchTerm.trim().toLowerCase();
    if (!search) return employees;

    return employees.filter((employee) =>
      `${employee.firstName} ${employee.lastName} ${employee.email} ${employee.department}`
        .toLowerCase()
        .includes(search)
    );
  }, [employees, searchTerm]);

  function confirmDelete() {
    if (!employeeToDelete || deleteLockedRef.current) return;

    deleteLockedRef.current = true;
    setError("");
    setSuccess("");
    setDeleting(true);

    employeeService
      .deleteEmployee(employeeToDelete.id)
      .then(() => {
        setEmployees((currentEmployees) =>
          currentEmployees.filter(
            (employee) => employee.id !== employeeToDelete.id
          )
        );
        setEmployeeToDelete(null);
        setSuccess("Employee deactivated successfully.");
      })
      .catch((deleteError) => {
        setError(
          getFriendlyErrorMessage(deleteError, "Failed to deactivate employee.")
        );
      })
      .finally(() => {
        deleteLockedRef.current = false;
        setDeleting(false);
      });
  }

  if (loading) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white">
        <LoadingSpinner label="Loading employees..." />
      </div>
    );
  }

  return (
    <section>
      {showWelcome && (
        <div className="fixed right-4 top-4 z-50 w-[calc(100%-2rem)] max-w-sm shadow-lg sm:right-6 sm:top-6">
          <FeedbackMessage type="success">
            Welcome, {username}
          </FeedbackMessage>
        </div>
      )}
      <PageTitle
        eyebrow="People directory"
        title="Employees"
        description="View and manage employee information from one place."
        action={
          canEdit ? (
            <Link
              to="/add-employee"
              className="inline-flex items-center justify-center rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
            >
              Add Employee
            </Link>
          ) : null
        }
      />

      <div className="mt-6 space-y-3">
        <FeedbackMessage type="success">{success}</FeedbackMessage>
        <FeedbackMessage>{error}</FeedbackMessage>
        {loadFailed && <Button variant="secondary" onClick={loadEmployees}>Retry</Button>}
      </div>

      <div className="mt-8 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-200 p-4 sm:p-5">
          <label htmlFor="employee-search" className="sr-only">Search employees</label>
          <input id="employee-search" type="search" placeholder="Search by name, email, or department..." value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-100 sm:max-w-md" />
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                {["Employee", "Email", "Department", "Actions"].map((heading) => (
                  <th key={heading} scope="col" className="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredEmployees.map((employee, index) => (
                <tr key={employee.id} className={`transition hover:bg-blue-50/60 ${index % 2 === 1 ? "bg-slate-50/50" : "bg-white"}`}>
                  <td className="whitespace-nowrap px-5 py-4">
                    <div className="flex items-center gap-3">
                      <span className="flex size-10 items-center justify-center rounded-full bg-blue-100 text-sm font-bold text-blue-700">{employee.firstName.charAt(0)}{employee.lastName.charAt(0)}</span>
                      <div>
                        <p className="font-semibold text-slate-900">{employee.firstName} {employee.lastName}</p>
                        <p className="text-xs text-slate-500">ID #{employee.id}</p>
                      </div>
                    </div>
                  </td>
                  <td className="whitespace-nowrap px-5 py-4 text-sm text-slate-600">{employee.email}</td>
                  <td className="whitespace-nowrap px-5 py-4"><span className="inline-flex rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">{employee.department}</span></td>
                  <td className="whitespace-nowrap px-5 py-4">
                    <div className="flex items-center gap-2">

                      {/* All authenticated users can view employee details */}
                      <Link
                        to={`/employees/${employee.id}`}
                        className="rounded-lg px-3 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-100"
                      >
                        View
                      </Link>

                      {/* Only ADMIN can edit employees */}
                      {canEdit && (
                        <Link
                          to={`/edit-employee/${employee.id}`}
                          className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100"
                        >
                          Edit
                        </Link>
                      )}

                      {/* Only ADMIN can deactivate employees */}
                      {canDelete && (
                        <button
                          type="button"
                          onClick={() => setEmployeeToDelete(employee)}
                          className="rounded-lg px-3 py-2 text-sm font-semibold text-red-600 transition hover:bg-red-50"
                        >
                          Deactivate
                        </button>
                      )}

                    </div>
                  </td>
                </tr>
              ))}

              {filteredEmployees.length === 0 && !loadFailed && (
                <tr>
                  <td colSpan="4" className="px-6 py-16 text-center">
                    <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-slate-100 text-xl">{searchTerm ? "?" : "0"}</div>
                    <p className="mt-4 font-semibold text-slate-900">{searchTerm ? "No matching employees" : "No employees yet"}</p>
                    <p className="mt-1 text-sm text-slate-500">{searchTerm ? "Try a different search term." : canEdit ? "Add your first employee to get started." : "No active employees are currently available."}</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {!loadFailed && <div className="border-t border-slate-200 bg-slate-50 px-5 py-3 text-sm text-slate-500">{filteredEmployees.length} of {employees.length} employees</div>}
      </div>

      <ConfirmDialog
        open={Boolean(employeeToDelete)}
        title="Deactivate Employee?"
        message="This employee will become inactive and will no longer be able to sign in. Work shift history will be preserved."
        confirmLabel="Deactivate"
        loadingLabel="Deactivating..."
        loading={deleting}
        onCancel={() => setEmployeeToDelete(null)}
        onConfirm={confirmDelete}
      />
    </section>
  );
}

export default Employees;
