import { Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import AddEmployee from "./pages/AddEmployee.jsx";
import Departments from "./pages/Departments.jsx";
import EditEmployee from "./pages/EditEmployee.jsx";
import EmployeeDetails from "./pages/EmployeeDetails.jsx";
import Employees from "./pages/Employees.jsx";
import Home from "./pages/Home.jsx";
import Login from "./pages/Login.jsx";
import Registration from "./pages/Registration.jsx";
import WorkHours from "./pages/WorkHours.jsx";
import AcceptInvitation from "./pages/AcceptInvitation.jsx";
import Tasks from "./pages/Tasks.jsx";

function App() {
  return (
    <div className="min-h-screen">
      <Navbar />

      <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Registration />} />
          <Route path="/accept-invitation" element={<AcceptInvitation />} />

          <Route
            path="/departments"
            element={
              <ProtectedRoute>
                <Departments />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employees"
            element={
              <ProtectedRoute>
                <Employees />
              </ProtectedRoute>
            }
          />
          <Route
            path="/tasks"
            element={
              <ProtectedRoute>
                <Tasks />
              </ProtectedRoute>
            }
          />
          <Route
            path="/work-hours"
            element={
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <WorkHours />
              </ProtectedRoute>
            }
          />

          <Route
            path="/add-employee"
            element={
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <AddEmployee />
              </ProtectedRoute>
            }
          />

          <Route
            path="/edit-employee/:id"
            element={
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <EditEmployee />
              </ProtectedRoute>
            }
          />

          <Route
            path="/employees/:id"
            element={
              <ProtectedRoute>
                <EmployeeDetails />
              </ProtectedRoute>
            }
          />
        </Routes>
      </main>
    </div>
  );
}

export default App;


