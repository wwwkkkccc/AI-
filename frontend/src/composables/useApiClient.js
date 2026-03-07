import { requestJson } from "../api/http";

export function useApiClient({ apiBase, token }) {
  async function apiRequest(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (token.value) {
      headers.Authorization = `Bearer ${token.value}`;
    }
    const opts = { ...options, headers };
    return requestJson(apiBase, path, opts);
  }

  return { apiRequest };
}
