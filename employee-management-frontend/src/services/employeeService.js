import authService from "./authService";
import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/employees`;

async function parseResponseBody(response) {
  const responseText = await response.text();

  if (!responseText) {
    return null;
  }

  try {
    return JSON.parse(responseText);
  } catch {
    return responseText;
  }
}

async function request(url, options = {}) {
  const token = authService.getToken();
  const headers = { ...options.headers };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(url, { ...options, headers });
  const data = await parseResponseBody(response);

  if (!response.ok) {
    if (response.status === 401) {
      authService.handleUnauthorized();
    }

    throw new ApiError(response.status, data);
  }

  return data;
}

function getAllEmployees() {
  return request(API_URL);
}

function getEmployeeById(id) {
  return request(`${API_URL}/${id}`);
}

function createEmployee(employee) {
  return request(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(employee),
  });
}

function updateEmployee(id, employee) {
  return request(`${API_URL}/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(employee),
  });
}


function updateHourlyRate(id, hourlyRate) {
  return request(`${API_URL}/${id}/hourly-rate`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ hourlyRate }),
  });
}
function deleteEmployee(id) {
  return request(`${API_URL}/${id}`, { method: "DELETE" });
}

function resendInvitation(id) {
  return request(`${API_URL}/${id}/invitation`, { method: "POST" });
}

function reactivateEmployee(id, names) {
  return request(`${API_URL}/${id}/reactivate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(names),
  });
}

const employeeService = {
  getAllEmployees,
  getEmployeeById,
  createEmployee,
  updateEmployee,
  updateHourlyRate,
  deleteEmployee,
  resendInvitation,
  reactivateEmployee,
};

export default employeeService;


