import authService from "./authService";
import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/work-shifts`;

async function request(url, options = {}) {
  const token = authService.getToken();
  const headers = { ...options.headers };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(url, { ...options, headers });
  let data = null;
  const responseText = await response.text();

  if (responseText) {
    try {
      data = JSON.parse(responseText);
    } catch {
      data = responseText;
    }
  }

  if (!response.ok) {
    if (response.status === 401) {
      authService.handleUnauthorized();
    }

    throw new ApiError(response.status, data);
  }

  return data;
}

function startShift() {
  return request(`${API_URL}/start`, { method: "POST" });
}

function endShift() {
  return request(`${API_URL}/end`, { method: "POST" });
}

function getCurrentShift() {
  return request(`${API_URL}/current`);
}


function getEmployeeMonthlyDetails(employeeId, year, month) {
  const query = new URLSearchParams({ year, month });
  return request(`${API_URL}/report/${employeeId}?${query}`);
}
function getMonthlyReport(year, month) {
  const query = new URLSearchParams({ year, month });
  return request(`${API_URL}/report?${query}`);
}

export default { startShift, endShift, getCurrentShift, getMonthlyReport, getEmployeeMonthlyDetails };