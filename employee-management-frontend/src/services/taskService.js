import authService from "./authService";
import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/tasks`;

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

async function requestDownload(url) {
  const token = authService.getToken();
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  const response = await fetch(url, { headers });

  if (!response.ok) {
    const responseText = await response.text();
    let data = null;
    if (responseText) {
      try {
        data = JSON.parse(responseText);
      } catch {
        data = responseText;
      }
    }
    if (response.status === 401) {
      authService.handleUnauthorized();
    }
    throw new ApiError(response.status, data);
  }

  return response.blob();
}

function getAllTasks() {
  return request(API_URL);
}

function createTask(task) {
  return request(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(task),
  });
}

function updateTask(id, task) {
  return request(`${API_URL}/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(task),
  });
}

function archiveTask(id) {
  return request(`${API_URL}/${id}/archive`, { method: "PATCH" });
}

function acceptTask(id) {
  return request(`${API_URL}/${id}/accept`, { method: "POST" });
}

function declineTask(id) {
  return request(`${API_URL}/${id}/decline`, { method: "POST" });
}

function requestChanges(id, changesRequest) {
  return request(`${API_URL}/${id}/request-changes`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(changesRequest),
  });
}

function updateChecklistItem(taskId, itemId, completed) {
  return request(`${API_URL}/${taskId}/checklist/${itemId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ completed }),
  });
}

function updateProgress(taskId, progress) {
  return request(`${API_URL}/${taskId}/progress`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ progress }),
  });
}

function getAttachments(taskId) {
  return request(`${API_URL}/${taskId}/attachments`);
}

function getTaskActivities(taskId) {
  return request(`${API_URL}/${taskId}/activities`);
}

function uploadAttachment(taskId, file) {
  const formData = new FormData();
  formData.append("file", file);
  return request(`${API_URL}/${taskId}/attachments`, {
    method: "POST",
    body: formData,
  });
}

function downloadAttachment(taskId, attachmentId) {
  return requestDownload(`${API_URL}/${taskId}/attachments/${attachmentId}/download`);
}

export default {
  getAllTasks,
  createTask,
  updateTask,
  archiveTask,
  acceptTask,
  declineTask,
  requestChanges,
  updateChecklistItem,
  updateProgress,
  getAttachments,
  getTaskActivities,
  uploadAttachment,
  downloadAttachment,
};
