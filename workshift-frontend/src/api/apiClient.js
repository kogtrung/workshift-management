import { getAccessToken } from "../features/auth/authStorage";

const DEFAULT_BASE_URL = "http://localhost:8080/api/v1";

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || DEFAULT_BASE_URL).replace(/\/+$/, "");

export async function apiFetch(path, options = {}) {
  const urlPath = String(path || "").startsWith("/") ? path : `/${path}`;
  const url = `${API_BASE_URL}${urlPath}`;

  const headers = new Headers(options.headers || {});
  if (!headers.has("Authorization")) {
    const accessToken = getAccessToken();
    if (accessToken) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }
  }
  if (!headers.has("Content-Type") && options.body && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  const payload = isJson ? await response.json().catch(() => null) : await response.text().catch(() => null);

  if (!response.ok) {
    const message = (payload && payload.message) || response.statusText || "Request failed";
    const error = new Error(message);
    error.status = response.status;
    error.payload = payload;
    throw error;
  }

  return payload;
}

export function unwrapApiResponse(payload) {
  if (!payload || typeof payload !== "object") {
    throw new Error("Invalid API response");
  }
  if (!("data" in payload)) {
    return payload;
  }
  return payload.data;
}
