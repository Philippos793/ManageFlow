import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import LoadingSpinner from "../components/ui/LoadingSpinner.jsx";
import employeeService from "../services/employeeService";
import authService from "../services/authService";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";

function EmployeeDetails() {
  const { id } = useParams();
  const isAdmin = authService.getRole() === "ADMIN";
  const [employee, setEmployee] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [resending, setResending] = useState(false);
  const [invitationMessage, setInvitationMessage] = useState("");

  useEffect(() => {
    employeeService
      .getEmployeeById(id)
      .then(setEmployee)
      .catch((requestError) => {
        setError(
          getFriendlyErrorMessage(
            requestError,
            "Failed to load employee details."
          )
        );
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return <LoadingSpinner label="Loading employee..." />;
  }

  if (!employee) {
    return (
      <div className="mx-auto max-w-xl space-y-5 rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <FeedbackMessage>{error}</FeedbackMessage>
        <Link to="/employees" className="inline-flex rounded-xl bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700">
          Back to Employees
        </Link>
      </div>
    );
  }

  const details = [
    ["First Name", employee.firstName],
    ["Last Name", employee.lastName],
    ["Email", employee.email],
    ["Department", employee.department],
  ];

  async function resendInvitation() {
    if (resending) return;
    setResending(true);
    setInvitationMessage("");
    try {
      await employeeService.resendInvitation(employee.id);
      setInvitationMessage("Account invitation sent successfully.");
    } catch (requestError) {
      setInvitationMessage(
        requestError?.data?.message ||
          getFriendlyErrorMessage(requestError, "Failed to send account invitation.")
      );
    } finally {
      setResending(false);
    }
  }

  return (
    <section className="mx-auto max-w-3xl">
      <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-6 py-8 text-white sm:px-8">
          <span className="flex size-14 items-center justify-center rounded-2xl bg-white/15 text-xl font-bold">
            {employee.firstName.charAt(0)}
            {employee.lastName.charAt(0)}
          </span>
          <h1 className="mt-4 text-3xl font-bold">
            {employee.firstName} {employee.lastName}
          </h1>
          <p className="mt-1 text-blue-100">{employee.department}</p>
        </div>

        <dl className="divide-y divide-slate-100 px-6 sm:px-8">
          {details.map(([label, value]) => (
            <div key={label} className="grid gap-1 py-5 sm:grid-cols-[11rem_1fr] sm:items-center">
              <dt className="text-sm font-semibold text-slate-500">{label}</dt>
              <dd className="font-medium text-slate-900">{value}</dd>
            </div>
          ))}
        </dl>

        {isAdmin && invitationMessage && (
          <div className="px-6 pb-5 sm:px-8">
            <FeedbackMessage type={invitationMessage.includes("successfully") ? "success" : "error"}>
              {invitationMessage}
            </FeedbackMessage>
          </div>
        )}

        <div className="flex flex-col-reverse gap-3 border-t border-slate-200 bg-slate-50 px-6 py-5 sm:flex-row sm:justify-end sm:px-8">
          <Link to="/employees" className="rounded-xl border border-slate-300 bg-white px-5 py-3 text-center text-sm font-semibold text-slate-700 hover:bg-slate-50">
            Back
          </Link>
          {isAdmin && (
            <button type="button" disabled={resending} onClick={resendInvitation} className="rounded-xl border border-blue-200 bg-white px-5 py-3 text-center text-sm font-semibold text-blue-700 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-60">
              {resending ? "Sending invitation..." : "Resend Invitation"}
            </button>
          )}
          {isAdmin && (
            <Link to={`/edit-employee/${employee.id}`} className="rounded-xl bg-blue-600 px-5 py-3 text-center text-sm font-semibold text-white hover:bg-blue-700">
              Edit Employee
            </Link>
          )}
        </div>
      </div>
    </section>
  );
}

export default EmployeeDetails;

