import { tokenStore } from "./tokenStore";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export const AUTH_EXPIRED_EVENT = "careerforge:auth-expired";

// auth: false -> used for login/register (no token is sent)
export async function request(path, { auth = true, ...options } = {}) {
  const token = auth ? tokenStore.get() : null;

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new Error("Cannot reach the CareerForge API. Check that Spring Boot is running on port 8080.");
  }

  if (response.status === 204) return null;

  const text = await response.text();
  let body = null;
  if (text) {
    try { body = JSON.parse(text); } catch { body = { message: text }; }
  }

  // We HAD a token and the server rejected it: expired, revoked, or the user was deleted/disabled
  if (response.status === 401 && token) {
    tokenStore.clear();
    window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
    throw new Error("Your session has expired. Please log in again.");
  }

  if (!response.ok) {
    throw new Error(body?.message || body?.error || `Request failed with status ${response.status}`);
  }
  return body;
}