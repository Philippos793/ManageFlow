import authService from "./authService";
import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/registration-requests`;

async function request(url, options = {}, authenticated = true) {
  const token = authenticated ? authService.getToken() : null;
  const headers = { ...options.headers };
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(url, { ...options, headers });
  let data;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    if (response.status === 401) authService.handleUnauthorized();
    throw new ApiError(response.status, data);
  }
  return data;
}

function submit(registration) {
  return request(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(registration),
  }, false);
}
function getPending() {
  return request(`${API_URL}/pending`);
}

function approve(id, departmentId) {
  return request(`${API_URL}/${id}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ departmentId }),
  });
}

function reject(id) {
  return request(`${API_URL}/${id}/reject`, { method: "POST" });
}

export default { submit, getPending, approve, reject };
