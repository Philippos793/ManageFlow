import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import authService from "../services/authService";
import NotificationsBell from "./NotificationsBell.jsx";
import ConfirmDialog from "./ui/ConfirmDialog.jsx";

const linkClassName = ({ isActive }) =>
  `rounded-lg px-3 py-2 text-center text-sm font-medium transition-colors md:text-left ${
    isActive
      ? "bg-blue-50 text-blue-700"
      : "text-slate-600 hover:bg-slate-100 hover:text-slate-950"
  }`;

function Navbar() {
  const navigate = useNavigate();
  const isLoggedIn = authService.isAuthenticated();
  const isAdmin = authService.getRole() === "ADMIN";
  const username = authService.getUsername();
  const [menuOpen, setMenuOpen] = useState(false);
  const [logoutConfirmationOpen, setLogoutConfirmationOpen] = useState(false);

  function handleLogout() {
    setLogoutConfirmationOpen(false);
    authService.logout();
    navigate("/login");
  }

  return (
    <header className="border-b border-slate-200 bg-white shadow-sm">
      <nav className="mx-auto flex w-full max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
        <NavLink to="/" className="flex items-center gap-3 text-slate-950">
          <span className="flex size-10 items-center justify-center rounded-xl bg-blue-600 text-lg font-bold text-white shadow-sm">
            MF
          </span>
          <span>
            <span className="block text-base font-bold leading-tight">ManageFlow</span>
            <span className="block text-xs text-slate-500">Employee Management</span>
          </span>
        </NavLink>

        <button
          type="button"
          aria-label="Toggle navigation"
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((current) => !current)}
          className="flex size-10 items-center justify-center rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50 md:hidden"
        >
          <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" className="size-5">
            <path strokeLinecap="round" strokeWidth="2" d={menuOpen ? "M6 6l12 12M18 6L6 18" : "M4 7h16M4 12h16M4 17h16"} />
          </svg>
        </button>

        <div onClick={(event) => { if (event.target.closest("a")) setMenuOpen(false); }} className={`${menuOpen ? "flex" : "hidden"} w-full flex-col items-stretch gap-1 border-t border-slate-200 pt-4 md:flex md:w-auto md:flex-row md:flex-wrap md:items-center md:justify-end md:border-0 md:pt-0`}>
          <NavLink to="/" className={linkClassName}>Home</NavLink>

          {isLoggedIn ? (
            <>
              <NavLink to="/employees" className={linkClassName}>Employees</NavLink>
              <NavLink to="/tasks" className={linkClassName}>Tasks</NavLink>
              <NavLink to="/departments" className={linkClassName}>Departments</NavLink>
              {isAdmin && <NavLink to="/work-hours" className={linkClassName}>Work Hours</NavLink>}
              {isAdmin && <NavLink to="/add-employee" className={linkClassName}>Add Employee</NavLink>}
              <NotificationsBell isAdmin={isAdmin} />
              <div className="mx-2 my-2 flex min-w-0 flex-col items-start border-t border-slate-100 pt-3 leading-tight md:my-0 md:items-end md:border-0 md:pt-0">
                <span className="text-xs font-normal text-slate-400">Signed in as</span>
                <span className="max-w-32 truncate text-sm font-semibold text-slate-700" title={username}>
                  {username}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setLogoutConfirmationOpen(true)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:border-slate-400 hover:bg-slate-50 md:ml-1"
              >
                Logout
              </button>
            </>
          ) : (
            <NavLink to="/login" className={linkClassName}>Login</NavLink>
          )}
        </div>
      </nav>
      <ConfirmDialog
        open={logoutConfirmationOpen}
        title="Log out?"
        message="Are you sure you want to log out?"
        confirmLabel="Log Out"
        cancelLabel="Cancel"
        onConfirm={handleLogout}
        onCancel={() => setLogoutConfirmationOpen(false)}
      />
    </header>
  );
}

export default Navbar;
