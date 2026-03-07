// Normalize pagination payloads from different backend response styles.
export function readPageItems(data) {
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.content)) return data.content;
  return [];
}

export function readPageTotal(data, fallback = 0) {
  const raw = data?.total ?? data?.totalElements;
  const n = Number(raw);
  return Number.isFinite(n) ? n : fallback;
}
