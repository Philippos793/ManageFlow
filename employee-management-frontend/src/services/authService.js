import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/auth`;

async function login(username, password) {
  const response = await fetch(`${API_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  let data;

  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    if (response.status === 401) {
      handleUnauthorized();
    }

    throw new ApiError(response.status, data);
  }

  // Store authentication information
  localStorage.setItem("token", data.token);
  localStorage.setItem("userId", String(data.userId));
  localStorage.setItem("username", data.username);
  localStorage.setItem("role", data.role);

  return data;
}

function logout() {
  // Remove all authentication information
  localStorage.removeItem("token");
  localStorage.removeItem("userId");
  localStorage.removeItem("username");
  localStorage.removeItem("role");
}

function handleUnauthorized() {
  logout();

  if (window.location.pathname !== "/login") {
    window.location.replace("/login");
  }
}
function getToken() {
  return localStorage.getItem("token");
}

function getUsername() {
  return localStorage.getItem("username");
}

function getUserId() {
  const userId = Number(localStorage.getItem("userId"));
  return Number.isSafeInteger(userId) && userId > 0 ? userId : null;
}

function getRole() {
  return localStorage.getItem("role");
}

function isAuthenticated() {
  return getToken() !== null;
}

const authService = {
  login,
  logout,
  handleUnauthorized,
  getToken,
  getUserId,
  getUsername,
  getRole,
  isAuthenticated,
};

export default authService;


