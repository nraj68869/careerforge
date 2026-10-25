import { tokenStore } from "./tokenStore";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export const AUTH_EXPIRED_EVENT = "careerforge:auth-expired";

// shared by request() and requestBlob(): adds the token and handles an expired session
async function send(path, { auth = true, ...options } = {}) {
  const token = auth ? tokenStore.get() : null;
  const isForm = options.body instanceof FormData;   // the browser sets the upload headers itself

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        ...(options.body && !isForm ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new Error("Cannot reach the CareerForge API. Check that Spring Boot is running on port 8080.");
  }

  // We HAD a token and the server rejected it: expired, revoked, or the user was deleted/disabled
  if (response.status === 401 && token) {
    tokenStore.clear();
    window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
    throw new Error("Your session has expired. Please log in again.");
  }
  return response;
}

async function errorFrom(response) {
  const text = await response.text();
  let body = null;
  if (text) {
    try { body = JSON.parse(text); } catch { body = { message: text }; }
  }
  return new Error(body?.message || body?.error || `Request failed with status ${response.status}`);
}

export async function request(path, options) {
  const response = await send(path, options);
  if (response.status === 204) return null;
  if (!response.ok) throw await errorFrom(response);
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

// for files (PDF, CSV)
export async function requestBlob(path, options) {
  const response = await send(path, options);
  if (!response.ok) throw await errorFrom(response);
  return response.blob();
}