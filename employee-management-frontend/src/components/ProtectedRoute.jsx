import { Navigate } from "react-router-dom";
import authService from "../services/authService";

function ProtectedRoute({ children, allowedRoles }) {
  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (
    allowedRoles &&
    !allowedRoles.includes(authService.getRole())
  ) {
    return <Navigate to="/employees" replace />;
  }

  return children;
}

export default ProtectedRoute;
