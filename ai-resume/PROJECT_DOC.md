# Resume AI 项目文档

更新日期：2026-03-05

## 1. 项目简介

Resume AI 是一个“简历分析 + 管理后台”系统，面向个人用户与管理员。

核心能力：

- 用户注册、登录、鉴权
- 简历上传与岗位匹配分析
- 分析任务排队（Redis）
- VIP 优先分析
- 管理员用户管理（VIP、拉黑）
- 管理员模型配置（Base URL、API Key、模型）
- 管理员查看客户简历分析记录

## 2. 技术栈

- 前端：Vue 3 + Vite + Nginx
- 后端：Spring Boot 3.3.5（Java 17）
- 数据库：MySQL 8
- 队列：Redis 7（ZSET 优先队列）
- 部署：Docker Compose

## 3. 系统架构

```text
浏览器
  -> Nginx /resume-ai/
     -> 前端静态资源（Vue）
     -> /resume-ai/api/* 转发到后端
         -> Spring Boot
            -> MySQL（业务数据）
            -> Redis（任务队列）
```

## 4. 功能设计

### 4.1 用户与权限

- 角色：
  - `USER`：提交分析、查看自己的分析记录
  - `ADMIN`：系统配置、用户管理、查看全量分析记录
- 注册限制：
  - 单 IP 每日最多注册 3 次（可配置）
- 拉黑规则：
  - 被拉黑用户不可登录
  - 已登录用户在后续请求时也会被拦截

### 4.2 分析流程

1. 用户提交 `POST /api/analyze`（文件 + JD + 目标岗位）
2. 后端创建任务并入 Redis 队列
3. 前端通过 `GET /api/analyze/jobs/{jobId}` 轮询状态
4. 任务完成后返回分析结果并写入 `analysis_records`

任务状态：

- `PENDING`：排队中
- `PROCESSING`：分析中
- `DONE`：完成
- `FAILED`：失败

### 4.3 VIP 优先队列

- 使用 Redis ZSET 作为优先队列
- VIP 用户优先级更高，先被消费
- 同优先级下按任务创建时间先后处理

### 4.4 管理员用户管理

- 用户列表分页查询（支持用户名关键字）
- 单用户状态更新：
  - 升级/取消 VIP
  - 拉黑/取消拉黑
- 保护规则：
  - 管理员账号不允许被拉黑

## 5. 数据模型（MySQL）

主要表：

- `user_accounts`：用户、角色、VIP、拉黑状态
- `user_sessions`：登录会话 Token
- `register_ip_daily`：IP 每日注册次数
- `ai_config`：模型配置
- `analysis_jobs`：排队任务及状态
- `analysis_records`：分析完成后的结果

## 6. 接口文档

接口基础路径：`/api`  
线上通常通过 Nginx 暴露为：`/resume-ai/api`

鉴权方式：

- 请求头：`Authorization: Bearer <token>`

### 6.1 认证接口

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

注册/登录请求体：

```json
{
  "username": "demo_user",
  "password": "12345678"
}
```

### 6.2 分析接口

- `POST /api/analyze`（multipart/form-data）
  - 字段：`file`、`jd_text`、`target_role`
  - 返回：`jobId`、`status`、`queuePosition`、`vipPriority`
- `GET /api/analyze/jobs/{jobId}`
  - 查询任务状态
  - `DONE` 时返回 `result`
- `GET /api/analyses/mine`
  - 查询当前用户自己的分析记录

分析入队响应示例：

```json
{
  "jobId": "70e78ec2150b9c4c88b875ea",
  "status": "PENDING",
  "queuePosition": 2,
  "vipPriority": false,
  "message": "queued"
}
```

### 6.3 管理员接口

- `GET /api/admin/config`
- `PUT /api/admin/config`
- `GET /api/admin/analyses`
- `GET /api/admin/users`
- `PUT /api/admin/users/{id}`

更新用户状态请求体示例：

```json
{
  "vip": true,
  "blacklisted": false
}
```

更新模型配置请求体示例：

```json
{
  "baseUrl": "https://aicodelink.shop",
  "apiKey": "sk-xxxx",
  "model": "gpt-5.3-codex"
}
```

## 7. 部署说明（Docker Compose）

### 7.1 服务

- `mysql`
- `redis`
- `backend`
- `frontend`

### 7.2 快速启动

1. 准备环境变量文件：

```bash
cp .env.example .env
```

2. 启动服务：

```bash
docker compose up -d --build
```

3. 检查状态：

```bash
docker compose ps
```

4. 后端健康检查：

```bash
curl http://127.0.0.1:18081/api/health
```

## 8. 默认访问路径

- 前端：`/resume-ai/`
- 后端 API：`/resume-ai/api/*`

## 9. 运维命令

```bash
# 查看容器状态
docker compose ps

# 查看后端日志
docker compose logs -f backend

# 查看前端日志
docker compose logs -f frontend

# 查看 Redis 日志
docker compose logs -f redis

# 重建并启动
docker compose up -d --build
```

## 10. 常见问题与排查

### 10.1 页面看不到“用户管理”

原因通常是前端缓存旧资源。  
处理方式：

- 强制刷新 `Ctrl + F5`
- 或无痕窗口重新打开
- 或清浏览器缓存后重新登录

### 10.2 后端构建报 Java 版本错误

现象：`invalid target release: 17`  
原因：Maven 使用了低版本 Java。  
处理：设置 `JAVA_HOME` 为 JDK 17 后重新构建。

### 10.3 分析任务一直排队

检查项：

- Redis 容器是否正常运行
- 后端日志是否有消费任务异常
- MySQL 连接是否正常

## 11. 安全建议

- 生产环境必须修改默认管理员账号密码
- 不要在仓库、日志、截图中暴露 API Key
- 对后台入口开启 HTTPS
- 配置服务器防火墙，仅开放必要端口

