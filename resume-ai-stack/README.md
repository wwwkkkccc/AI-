# Resume AI Stack (Vue + Java + MySQL + Docker)

完整中文项目文档请看：[PROJECT_DOC.md](./PROJECT_DOC.md)。

## Architecture
- `frontend`: Vue 3 (Vite), serves UI and admin panel.
- `backend`: Spring Boot 3 (Java 17), provides analysis APIs and AI config management APIs.
- `mysql`: stores AI config and analysis records.
- `docker-compose`: orchestrates all components.

## Quick Start
1. Copy `.env.example` to `.env` and fill secrets.
2. Run:
   - `docker compose up -d --build`
3. Health check:
   - `http://127.0.0.1:18081/api/health`

## Routes (behind host nginx)
- UI: `/resume-ai/`
- API: `/resume-ai/api/*`

## Auth
- `POST /api/auth/register` (per IP: max 3 registrations per day)
- `POST /api/auth/login`
- `GET /api/auth/me`

## User API
- `POST /api/analyze` (requires login)
- `GET /api/analyses/mine`

## Admin API (requires ADMIN role)
- `GET /api/admin/config`
- `PUT /api/admin/config`
- `GET /api/admin/analyses` (view customer resumes and suggestions)

Request body:
```json
{
  "baseUrl": "https://aicodelink.shop",
  "apiKey": "sk-xxxx",
  "model": "gpt-5.3-codex"
}
```

Default admin credentials (override via env):
- username: `admin`
- password: `Admin@123456`
