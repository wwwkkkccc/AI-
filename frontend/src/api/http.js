import { toZhMessage } from "../utils/errorMessages";

async function readJsonBody(res) {
  if (res.status === 204) return {};
  const contentType = res.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) return {};
  try {
    return await res.json();
  } catch {
    return {};
  }
}

export async function requestJson(apiBase, path, options = {}, errorMapper = toZhMessage) {
  const res = await fetch(`${apiBase}${path}`, options);
  const data = await readJsonBody(res);
  if (!res.ok) {
    const rawMessage = data?.detail || data?.message || `请求失败：${res.status}`;
    const message = typeof errorMapper === "function" ? errorMapper(rawMessage) : rawMessage;
    throw new Error(message);
  }
  return data;
}
