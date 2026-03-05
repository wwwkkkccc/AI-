# Resume AI Stack

一个面向简历分析场景的全栈项目，包含：
- 前端：Vue 3 + Vite
- 后端：Spring Boot 3（Java 17）
- 数据库：MySQL 8
- 队列：Redis（VIP 优先排队）
- 部署：Docker Compose

## 目录结构
- `frontend/`：用户端 + 管理端页面
- `backend/`：认证、排队分析、题库、管理接口
- `docker-compose.yml`：一键启动/重建服务
- `Jenkinsfile`：CI/CD 流水线（Gitee 提交后自动部署）
- `docs/JENKINS_GITEE_DEPLOY.md`：Jenkins 与 Gitee 对接说明

## 本地启动
1. 复制环境变量模板：
```bash
cp .env.example .env
```
2. 启动服务：
```bash
docker compose up -d --build
```
3. 健康检查：
```bash
curl http://127.0.0.1:18081/api/health
```

## 默认访问路径（Nginx 反向代理后）
- UI：`/resume-ai/`
- API：`/resume-ai/api/*`

## 默认管理员
可通过环境变量覆盖，默认值如下：
- 用户名：`admin`
- 密码：`Admin@123456`

## 关键功能
- 简历上传分析（异步排队）
- VIP 优先队列
- 模型配置管理（URL / API Key / 模型）
- 客户简历查看（含建议、简历预览）
- 面试题库：文件上传入库 / 爬虫入库 / 大模型生成入库

## CI/CD（Gitee -> Jenkins -> 服务器）
已内置：
- `Jenkinsfile`
- `scripts/ci/deploy_remote.sh`

配置步骤请看：
- [docs/JENKINS_GITEE_DEPLOY.md](./docs/JENKINS_GITEE_DEPLOY.md)
