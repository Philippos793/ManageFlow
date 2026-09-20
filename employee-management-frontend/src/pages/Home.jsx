import { Link } from "react-router-dom";
import authService from "../services/authService";
import ShiftControls from "../components/ShiftControls.jsx";
import AdminDashboardSummary from "../components/AdminDashboardSummary.jsx";

function Home() {
  const isLoggedIn = authService.isAuthenticated();
  const username = authService.getUsername();
  const role = authService.getRole();

  return (
    <div className="space-y-8">
      <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
      <div className="grid items-center gap-10 px-6 py-12 sm:px-10 lg:grid-cols-[1.2fr_0.8fr] lg:px-16 lg:py-20">
        <div>
          {isLoggedIn && username && (
            <p className="mb-4 text-lg font-semibold text-slate-800">
              Welcome, {username}
            </p>
          )}
          <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-sm font-semibold text-blue-700">
            Employee operations, simplified
          </span>
          <h1 className="mt-6 max-w-3xl text-4xl font-bold tracking-tight text-slate-950 sm:text-5xl">
            A clearer way to manage your people.
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-slate-600">
            Keep employee records organized, searchable, and easy to update from one secure employee management workspace.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to={isLoggedIn ? "/employees" : "/login"} className="rounded-xl bg-blue-600 px-5 py-3 font-semibold text-white shadow-sm transition hover:bg-blue-700">
              {isLoggedIn ? "View Employees" : "Sign In"}
            </Link>
            {isLoggedIn && role === "ADMIN" && (
              <Link to="/add-employee" className="rounded-xl border border-slate-300 bg-white px-5 py-3 font-semibold text-slate-700 transition hover:bg-slate-50">
                Add Employee
              </Link>
            )}
          </div>
          {isLoggedIn && role === "EMPLOYEE" && <ShiftControls />}
        </div>

        <div className="rounded-3xl bg-gradient-to-br from-blue-600 to-indigo-700 p-8 text-white shadow-xl shadow-blue-200">
          <div className="grid grid-cols-2 gap-4">
            <div className="rounded-2xl bg-white/10 p-5 backdrop-blur">
              <p className="text-sm text-blue-100">Workspace</p>
              <p className="mt-2 text-2xl font-bold">Secure</p>
            </div>
            <div className="rounded-2xl bg-white/10 p-5 backdrop-blur">
              <p className="text-sm text-blue-100">Access</p>
              <p className="mt-2 text-2xl font-bold">Role-based</p>
            </div>
            <div className="col-span-2 rounded-2xl bg-white p-6 text-slate-900">
              <p className="text-sm font-medium text-slate-500">Employee Management</p>
              <p className="mt-2 text-xl font-bold">One reliable source for employee data</p>
            </div>
          </div>
        </div>
      </div>
      </section>
      {isLoggedIn && role === "ADMIN" && <AdminDashboardSummary />}
    </div>
  );
}

export default Home;
