import authService from "./authService";
import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/dashboard/admin-summary`;

async function getAdminSummary() {
  const token = authService.getToken();
  const headers = {};

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(API_URL, { headers });
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
    if (response.status === 401) authService.handleUnauthorized();
    throw new ApiError(response.status, data);
  }

  return data;
}

export default { getAdminSummary };