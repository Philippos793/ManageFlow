import authService from "./authService";
import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/notifications`;

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

function getNotifications() {
  return request(API_URL);
}

function markAsRead(id) {
  return request(`${API_URL}/${id}/read`, { method: "PATCH" });
}

const notificationService = {
  getNotifications,
  markAsRead,
};

export default notificationService;
