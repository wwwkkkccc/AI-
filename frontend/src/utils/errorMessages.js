// 后端错误消息 -> 中文用户提示。
const ERROR_MESSAGE_MAP = Object.freeze({
  "invalid username or password": "用户名或密码错误",
  "missing auth token": "请先登录",
  "token expired or invalid": "登录已过期，请重新登录",
  "admin access required": "需要管理员权限",
  "account is blacklisted": "账号已被拉黑",
  "cannot blacklist admin": "不能拉黑管理员账号",
  "user not found": "用户不存在",
  "file is required": "请上传简历文件",
  "file is too large": "文件过大，请上传 <= 5MB",
  "jd_text should have at least 20 characters": "JD 文本至少需要 20 个字符",
  "target_role is required": "目标岗位不能为空",
  "analysis_id is required": "分析 ID 不能为空",
  "analysis record not found": "分析记录不存在",
  "cannot access this analysis record": "无权访问该分析记录",
  "job not found": "任务不存在",
  "cannot access this job": "无权访问该任务",
  "chat session not found": "会话不存在",
  "cannot access this chat session": "无权访问该会话",
  "request body must be valid json": "请求体 JSON 格式无效",
  "request validation failed": "请求参数校验失败",
  "unsupported content type, use application/json or application/x-www-form-urlencoded":
    "不支持的内容类型",
  "internal server error": "服务器内部错误",
  queued: "已进入队列",
  "VIP priority queued": "已进入 VIP 优先队列"
});

export function toZhMessage(message) {
  const msg = String(message || "").trim();
  if (!msg) return "请求失败";
  const lower = msg.toLowerCase();
  return ERROR_MESSAGE_MAP[msg] || ERROR_MESSAGE_MAP[lower] || msg;
}
