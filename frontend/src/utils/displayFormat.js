function asNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

export function formatScore(value) {
  return asNumber(value).toFixed(2);
}

export function formatCoverage(value) {
  return `${Math.round(Math.max(0, Math.min(100, asNumber(value))) * 100) / 100}%`;
}

export function formatTime(value) {
  if (!value) return "-";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString();
}

export function roleText(role) {
  return String(role || "").toUpperCase() === "ADMIN" ? "管理员" : "普通用户";
}

export function statusText(status) {
  const map = {
    PENDING: "排队中",
    PROCESSING: "分析中",
    DONE: "已完成",
    FAILED: "失败"
  };
  return map[status] || status || "-";
}

export function statusClass(status) {
  const v = String(status || "").toUpperCase();
  if (v === "DONE") return "status-done";
  if (v === "FAILED") return "status-failed";
  if (v === "PROCESSING") return "status-processing";
  return "status-pending";
}

export function pageRange(current, total) {
  const t = Math.max(1, Number(total) || 1);
  const c = Math.min(Math.max(0, Number(current) || 0), t - 1);
  const start = Math.max(0, c - 2);
  const end = Math.min(t - 1, start + 4);
  const realStart = Math.max(0, end - 4);
  const pages = [];
  for (let i = realStart; i <= end; i += 1) {
    pages.push(i);
  }
  return pages;
}
