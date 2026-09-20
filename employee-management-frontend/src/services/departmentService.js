import authService from "./authService";
import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/departments`;

async function request(url, options = {}) {
  const token = authService.getToken();
  const headers = { ...options.headers };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(url, { ...options, headers });
  const responseText = await response.text();
  let data = null;

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

function getAllDepartments() {
  return request(API_URL);
}

function createDepartment(department) {
  return request(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(department),
  });
}

function updateDepartment(id, department) {
  return request(`${API_URL}/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(department),
  });
}

function deleteDepartment(id) {
  return request(`${API_URL}/${id}`, { method: "DELETE" });
}

export default {
  getAllDepartments,
  createDepartment,
  updateDepartment,
  deleteDepartment,
};