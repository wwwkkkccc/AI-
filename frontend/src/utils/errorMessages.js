// Backend message -> user-facing message.
const ERROR_MESSAGE_MAP = Object.freeze({
  "invalid username or password": "Invalid username or password",
  "missing auth token": "Please login first",
  "token expired or invalid": "Login expired, please login again",
  "admin access required": "Admin permission required",
  "account is blacklisted": "Account is blacklisted",
  "cannot blacklist admin": "Cannot blacklist admin user",
  "user not found": "User not found",
  "file is required": "Please upload a resume file",
  "file is too large": "File too large, please upload <= 5MB",
  "jd_text should have at least 20 characters": "JD text is too short",
  "target_role is required": "Target role is required",
  "analysis_id is required": "Analysis ID is required",
  "analysis record not found": "Analysis record not found",
  "cannot access this analysis record": "No permission to access this analysis record",
  "job not found": "Job not found",
  "cannot access this job": "No permission to access this job",
  "chat session not found": "Chat session not found",
  "cannot access this chat session": "No permission to access this chat session",
  "request body must be valid json": "Invalid request body JSON",
  "request validation failed": "Request validation failed",
  "unsupported content type, use application/json or application/x-www-form-urlencoded":
    "Unsupported content type",
  "internal server error": "Internal server error",
  queued: "Queued",
  "VIP priority queued": "Queued with VIP priority"
});

export function toZhMessage(message) {
  const msg = String(message || "").trim();
  return ERROR_MESSAGE_MAP[msg] || msg || "Request failed";
}
