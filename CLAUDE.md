# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Resume AI is a full-stack resume analysis system with interview question knowledge base management. Users upload resumes, the system scores them against job descriptions using local ATS rules + optional LLM optimization, and returns actionable feedback. Admins manage users, AI model config, and the interview question knowledge base.

## Tech Stack

- Backend: Spring Boot 3.3.5, Java 17, Spring Data JPA (Hibernate ddl-auto: update), Spring WebFlux (WebClient for LLM calls), Spring Data Redis
- Frontend: Vue 3 single-file app (one `App.vue`), Vite 5, served via Nginx
- Database: MySQL 8 (utf8mb4), Redis 7 (ZSET priority queue)
- OCR: Tesseract (tess4j) for image-based resume/JD extraction
- File parsing: PDFBox 3 (PDF), POI 5 (DOCX), Jsoup (web crawling)
- Deployment: Docker Compose, Jenkins pipeline

## Build & Run

```bash
# Full stack (requires Docker)
cp .env.example .env
docker compose up -d --build

# Health check
curl http://127.0.0.1:18081/api/health

# Rebuild only backend/frontend
docker compose up -d --build backend frontend

# View logs
docker compose logs -f backend
```

There is no standalone Maven or npm dev workflow outside Docker — the Dockerfiles handle all builds:
- Backend: `maven:3.9.9-eclipse-temurin-17` builds the JAR, `eclipse-temurin:17-jre` runs it
- Frontend: `node:20-alpine` runs `npm run build`, `nginx:1.27-alpine` serves the dist

Backend has `spring-boot-starter-test` in pom.xml but no test classes exist yet. To run tests when added: `cd backend && mvn test`

## Architecture

### Single Controller Pattern

All API endpoints live in one controller: `ApiController.java` (`/api/*`). No Spring Security filter chain — auth is handled manually via `AuthService.requireUser(request)` / `requireAdmin(request)` which reads `Authorization: Bearer <token>` headers and validates against `user_sessions` table.

### Async Analysis Queue

Resume analysis is not synchronous. The flow is:
1. `POST /api/analyze` -> `AnalysisQueueService.enqueue()` saves file to disk, creates `AnalysisJob` in DB, adds jobId to Redis ZSET
2. `@Scheduled` poller (`consumeOne()`) pops one job at a time from Redis ZSET (VIP users get lower score = higher priority)
3. `AnalyzeService.processQueuedJob()` runs the actual analysis pipeline
4. Frontend polls `GET /api/analyze/jobs/{jobId}` until status is DONE/FAILED

### Analysis Pipeline (AnalyzeService)

`analyzeInternal()` is the core chain:
1. Extract text from file (PDF/DOCX/image OCR/plain text)
2. Build ATS report: tokenize resume + JD, compute weighted keyword coverage, section completeness, action verb strength, quantification strength -> composite score
3. Try LLM call via OpenAI-compatible `/chat/completions` endpoint (configurable base URL/key/model in `ai_config` table). Returns structured JSON with rewritten experience, skills recommendations, interview questions
4. If LLM fails or no API key configured -> local fallback generates rule-based suggestions
5. Merge interview questions from knowledge base (keyword overlap scoring) with LLM/fallback questions
6. Persist `AnalysisRecord` and return response

### Interview Knowledge Base

Three import paths, all ending at `InterviewQuestionKbService`:
- File upload: PDF/DOCX/TXT/image -> extract text -> parse questions by line/sentence splitting + heuristic question detection
- Web crawler (`InterviewKbCrawlerService`): BFS crawl from seed URL, extract questions from HTML blocks and `<script>` tags, supports auth cookies
- LLM import (`InterviewKbLlmImportService`): generate questions via LLM for a given topic

Questions are stored in `interview_kb_items` with pre-computed keyword CSV for retrieval scoring.

### Frontend

The entire frontend is a single `App.vue` file (~1500 lines) with inline styles. No router, no Vuex/Pinia — all state is reactive `ref()`s. Tabs switch between views (analysis, history, admin panels). API calls use `fetch()` with token from localStorage.

Vite base path is `/resume-ai/`. Nginx proxies `/api/*` to `backend:8080`.

## Key Configuration

All config is via environment variables (see `.env.example`). In `application.yml`, every value has a `${ENV_VAR:default}` pattern.

Important settings:
- `APP_AI_DEFAULT_BASE_URL` / `APP_AI_DEFAULT_API_KEY` / `APP_AI_DEFAULT_MODEL`: LLM endpoint config (stored in DB `ai_config` table, admin can update at runtime via API)
- `APP_AUTH_MAX_REGISTER_PER_IP_PER_DAY`: rate limit (default 3)
- `APP_QUEUE_POLL_MS`: queue consumer interval (default 700ms)

## Port Mapping

- `18081:8080` — backend (Spring Boot)
- `18180:80` — frontend (Nginx)

## CI/CD

Jenkins pipeline (`Jenkinsfile`) uses "upload package" strategy — Jenkins checks out code, tars the workspace, SCPs to server, extracts, runs `docker compose up -d --build`. The deploy server does NOT need git access. Deploy script: `scripts/ci/deploy_remote.sh`.

## Database

JPA auto-creates/updates tables. Key tables: `user_accounts`, `user_sessions`, `register_ip_daily`, `ai_config` (singleton row id=1), `analysis_jobs`, `analysis_records`, `interview_kb_docs`, `interview_kb_items`.

## Conventions

- No Lombok — all models use explicit getters/setters
- No Spring Security dependency (only `spring-security-crypto` for BCrypt)
- Error responses are `{ "detail": "message" }` — mapped by `@ExceptionHandler` methods in `ApiController`
- `clean()` helper (null -> empty string + trim) is duplicated across services rather than extracted
- Token format: 36 random bytes, Base64 URL-encoded
- Username: lowercase, 4-32 chars, letters/numbers/underscore only
