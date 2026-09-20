import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import Button from "../components/ui/Button.jsx";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import LoadingSpinner from "../components/ui/LoadingSpinner.jsx";
import PasswordInput from "../components/ui/PasswordInput.jsx";
import employeeInvitationService from "../services/employeeInvitationService.js";
import { getFriendlyErrorMessage, getValidationErrors } from "../utils/apiErrors.js";

function invitationTokenFromUrl() {
  return new URLSearchParams(window.location.hash.slice(1)).get("token") || "";
}

function AcceptInvitation() {
  const token = invitationTokenFromUrl();
  const [invitation, setInvitation] = useState(null);
  const [checking, setChecking] = useState(true);
  const [form, setForm] = useState({ username: "", password: "", confirmPassword: "" });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);
  const submitLockedRef = useRef(false);

  useEffect(() => {
    let mounted = true;
    if (!token) return undefined;

    employeeInvitationService.validate(token)
      .then((data) => {
        if (mounted) setInvitation(data);
      })
      .catch((requestError) => {
        if (mounted) {
          setErrors({
            form: requestError?.data?.message ||
              getFriendlyErrorMessage(requestError, "Unable to validate this invitation."),
          });
        }
      })
      .finally(() => {
        if (mounted) setChecking(false);
      });
    return () => { mounted = false; };
  }, [token]);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: undefined, form: undefined }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitLockedRef.current) return;
    setErrors({});

    if (form.password !== form.confirmPassword) {
      setErrors({ confirmPassword: "Passwords do not match." });
      return;
    }

    submitLockedRef.current = true;
    setSubmitting(true);
    try {
      await employeeInvitationService.accept(token, form.username, form.password);
      setCompleted(true);
    } catch (requestError) {
      const validationErrors = getValidationErrors(requestError);
      setErrors(Object.keys(validationErrors).length > 0
        ? validationErrors
        : {
            form: requestError?.data?.message ||
              getFriendlyErrorMessage(requestError, "Unable to create your account."),
          });
    } finally {
      submitLockedRef.current = false;
      setSubmitting(false);
    }
  }

  if (!token) {
    return <InvitationCard><FeedbackMessage>Invitation link is missing or invalid.</FeedbackMessage></InvitationCard>;
  }

  if (checking) {
    return <InvitationCard><LoadingSpinner label="Checking invitation..." /></InvitationCard>;
  }

  if (completed) {
    return (
      <InvitationCard>
        <FeedbackMessage type="success">Your account has been created successfully.</FeedbackMessage>
        <Link to="/login" className="mt-5 inline-flex w-full justify-center rounded-xl bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700">
          Continue to Login
        </Link>
      </InvitationCard>
    );
  }

  if (!invitation) {
    return (
      <InvitationCard>
        <FeedbackMessage>{errors.form || "Invitation is invalid or no longer available."}</FeedbackMessage>
      </InvitationCard>
    );
  }

  return (
    <InvitationCard>
      <div className="text-center">
        <span className="mx-auto flex size-14 items-center justify-center rounded-2xl bg-blue-600 text-xl font-bold text-white">MF</span>
        <h1 className="mt-5 text-2xl font-bold text-slate-950">Set up your account</h1>
        <p className="mt-2 text-sm text-slate-500">
          Welcome, {invitation.employeeName}. Choose your login details.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="mt-7 space-y-5">
        <Field label="Username" name="username" value={form.username} error={errors.username} onChange={handleChange} autoComplete="username" />
        <PasswordInput id="password" name="password" label="Password" value={form.password} error={errors.password} onChange={handleChange} autoComplete="new-password" />
        <PasswordInput id="confirmPassword" name="confirmPassword" label="Confirm Password" value={form.confirmPassword} error={errors.confirmPassword} onChange={handleChange} autoComplete="new-password" />
        <FeedbackMessage>{errors.form}</FeedbackMessage>
        <Button type="submit" disabled={submitting} className="w-full">
          {submitting ? "Creating account..." : "Create Account"}
        </Button>
      </form>
    </InvitationCard>
  );
}

function InvitationCard({ children }) {
  return (
    <section className="flex min-h-[calc(100vh-11rem)] items-center justify-center py-6">
      <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl shadow-slate-200/60 sm:p-8">
        {children}
      </div>
    </section>
  );
}

function Field({ label, name, type = "text", value, error, onChange, autoComplete }) {
  return (
    <div>
      <label htmlFor={name} className="mb-2 block text-sm font-semibold text-slate-700">{label}</label>
      <input id={name} name={name} type={type} required value={value} onChange={onChange} autoComplete={autoComplete} aria-invalid={Boolean(error)} aria-describedby={error ? `${name}-error` : undefined} className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />
      {error && <p id={`${name}-error`} className="mt-1.5 text-sm font-medium text-red-600">{error}</p>}
    </div>
  );
}

export default AcceptInvitation;
