export function filterMineItemsByKeyword(items, keyword) {
  const rawKeyword = String(keyword || "").trim().toLowerCase();
  if (!rawKeyword) return items;
  return items.filter((item) => {
    const fields = [item.filename, item.targetRole, item.optimizedSummary]
      .map((value) => String(value || "").toLowerCase());
    return fields.some((value) => value.includes(rawKeyword));
  });
}

export function filterKbDocsByKeyword(items, keyword) {
  const rawKeyword = String(keyword || "").trim().toLowerCase();
  if (!rawKeyword) return items;
  return items.filter((item) => {
    const fields = [item.title, item.filename, item.uploadedBy]
      .map((value) => String(value || "").toLowerCase());
    return fields.some((value) => value.includes(rawKeyword));
  });
}
