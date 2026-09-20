import { ApiError } from "../utils/apiErrors.js";

const API_URL = `${import.meta.env.VITE_API_URL}/employee-invitations`;

async function request(path, body) {
  const response = await fetch(`${API_URL}/${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  let data = null;
  const responseText = await response.text();
  if (responseText) {
    try {
      data = JSON.parse(responseText);
    } catch {
      data = responseText;
    }
  }

  if (!response.ok) throw new ApiError(response.status, data);
  return data;
}

function validate(token) {
  return request("validate", { token });
}

function accept(token, username, password) {
  return request("accept", { token, username, password });
}

export default { validate, accept };
