import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import Button from "../components/ui/Button.jsx";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import PasswordInput from "../components/ui/PasswordInput.jsx";
import registrationRequestService from "../services/registrationRequestService.js";
import { getFriendlyErrorMessage, getValidationErrors } from "../utils/apiErrors.js";

const initialForm = {
  firstName: "",
  lastName: "",
  email: "",
  username: "",
  password: "",
  confirmPassword: "",
};

function Registration() {
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const submitLockedRef = useRef(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: undefined }));
    setMessage("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitLockedRef.current) return;
    setErrors({});
    setMessage("");

    if (form.password !== form.confirmPassword) {
      setErrors({ confirmPassword: "Passwords do not match." });
      return;
    }

    submitLockedRef.current = true;
    setLoading(true);
    try {
      await registrationRequestService.submit({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        username: form.username,
        password: form.password,
      });
      setForm(initialForm);
      setMessage("Your account request has been submitted and is waiting for admin approval.");
    } catch (requestError) {
      const validationErrors = getValidationErrors(requestError);
      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
      } else {
        setErrors({
          form:
            requestError?.data?.message ||
            getFriendlyErrorMessage(requestError, "Unable to submit your account request. Please try again."),
        });
      }
    } finally {
      submitLockedRef.current = false;
      setLoading(false);
    }
  }


  return (
    <section className="flex min-h-[calc(100vh-11rem)] items-center justify-center py-6">
      <div className="w-full max-w-2xl rounded-3xl border border-slate-200 bg-white p-6 shadow-xl shadow-slate-200/60 sm:p-8">
        <div className="text-center">
          <span className="mx-auto flex size-14 items-center justify-center rounded-2xl bg-blue-600 text-xl font-bold text-white shadow-lg shadow-blue-200">MF</span>
          <h1 className="mt-6 text-2xl font-bold text-slate-950">Request a ManageFlow account</h1>
          <p className="mt-2 text-sm text-slate-500">Submit your details for administrator approval.</p>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
          <div className="grid gap-5 sm:grid-cols-2">
            <FormField label="First Name" name="firstName" value={form.firstName} error={errors.firstName} onChange={handleChange} autoComplete="given-name" placeholder="Enter your first name" />
            <FormField label="Last Name" name="lastName" value={form.lastName} error={errors.lastName} onChange={handleChange} autoComplete="family-name" placeholder="Enter your last name" />
          </div>
          <FormField label="Email" name="email" type="email" value={form.email} error={errors.email} onChange={handleChange} autoComplete="email" placeholder="Enter your email" />
          <FormField label="Username" name="username" value={form.username} error={errors.username} onChange={handleChange} autoComplete="username" placeholder="Choose a username" />
          <div className="grid gap-5 sm:grid-cols-2">
            <PasswordInput id="password" name="password" label="Password" value={form.password} error={errors.password} onChange={handleChange} autoComplete="new-password" placeholder="Choose a password" />
            <PasswordInput id="confirmPassword" name="confirmPassword" label="Confirm Password" value={form.confirmPassword} error={errors.confirmPassword} onChange={handleChange} autoComplete="new-password" placeholder="Confirm your password" />
          </div>

          <FeedbackMessage>{errors.form}</FeedbackMessage>
          <FeedbackMessage type="success">{message}</FeedbackMessage>

          <Button type="submit" disabled={loading} className="w-full">
            {loading ? "Submitting request..." : "Request Account"}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-500">
          Already have an account?{" "}
          <Link to="/login" className="font-semibold text-blue-600 hover:text-blue-700">Sign in</Link>
        </p>
      </div>
    </section>
  );
}

function FormField({ label, name, type = "text", value, error, onChange, autoComplete, placeholder }) {
  const inputClassName = "w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-100";

  return (
    <div>
      <label htmlFor={name} className="mb-2 block text-sm font-semibold text-slate-700">{label}</label>
      <input id={name} name={name} type={type} required value={value} onChange={onChange} autoComplete={autoComplete} placeholder={placeholder} aria-invalid={Boolean(error)} aria-describedby={error ? `${name}-error` : undefined} className={inputClassName} />
      {error && <p id={`${name}-error`} className="mt-1.5 text-sm font-medium text-red-600">{error}</p>}
    </div>
  );
}

export default Registration;
