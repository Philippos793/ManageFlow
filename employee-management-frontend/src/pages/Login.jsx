import { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Button from "../components/ui/Button.jsx";
import FeedbackMessage from "../components/ui/FeedbackMessage.jsx";
import PasswordInput from "../components/ui/PasswordInput.jsx";
import authService from "../services/authService";
import { getFriendlyErrorMessage } from "../utils/apiErrors.js";

function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const submitLockedRef = useRef(false);

  function handleSubmit(event) {

    event.preventDefault();
    if (submitLockedRef.current) return;
    submitLockedRef.current = true;

    setError("");
    setLoading(true);

    authService
      .login(username, password)
      .then(() => {
        navigate("/employees", {
          state: { showWelcome: true },
        });
      })
      .catch((loginError) => {
        setError(
          loginError?.status === 401
            ? "Invalid username or password."
            : getFriendlyErrorMessage(
                loginError,
                "Unable to sign in. Please try again."
              )
        );
      })
      .finally(() => {
        submitLockedRef.current = false;
        setLoading(false);
      });
  }

  return (
    <section className="flex min-h-[calc(100vh-11rem)] items-center justify-center py-6">
      <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl shadow-slate-200/60 sm:p-8">
        <div className="text-center">
          <span className="mx-auto flex size-14 items-center justify-center rounded-2xl bg-blue-600 text-xl font-bold text-white shadow-lg shadow-blue-200">MF</span>
          <h1 className="mt-6 text-2xl font-bold text-slate-950">Welcome to ManageFlow</h1>
          <p className="mt-2 text-sm text-slate-500">Sign in to your employee management workspace</p>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
          <div>
            <label htmlFor="username" className="mb-2 block text-sm font-semibold text-slate-700">Username</label>
            <input id="username" type="text" autoComplete="username" required placeholder="Enter your username" value={username} onChange={(event) => setUsername(event.target.value)} className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />
          </div>
          <PasswordInput id="password" label="Password" autoComplete="current-password" placeholder="Enter your password" value={password} onChange={(event) => setPassword(event.target.value)} />

          <FeedbackMessage>{error}</FeedbackMessage>

          <Button type="submit" disabled={loading} className="w-full">
            {loading ? "Logging in..." : "Sign In"}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-500">
          Need an account?{" "}
          <Link to="/register" className="font-semibold text-blue-600 hover:text-blue-700">Request an account</Link>
        </p>
      </div>
    </section>
  );
}

export default Login;



