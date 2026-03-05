# Resume AI Stack

一个用于简历分析与面试题库管理的全栈项目，支持规则分析 + 大模型分析、VIP 优先队列、知识库与爬虫入库。

## 技术栈
- 前端：Vue 3 + Vite
- 后端：Spring Boot 3 (Java 17)
- 数据库：MySQL 8
- 队列与优先级：Redis 7
- 部署：Docker Compose
- CI/CD：Gitee + Jenkins + SSH 发布

## 目录结构
- `frontend/`：用户端与管理端页面
- `backend/`：认证、简历分析、题库、管理接口
- `scripts/ci/deploy_remote.sh`：上传包到服务器并部署
- `docker-compose.yml`：应用容器编排
- `jenkins/docker-compose.yml`：Jenkins 容器编排
- `Jenkinsfile`：流水线定义

## 本地开发
1. 复制环境变量

```bash
cp .env.example .env
```

2. 启动服务

```bash
docker compose up -d --build
```

3. 健康检查

```bash
curl http://127.0.0.1:18081/api/health
```

## 访问地址（按当前 compose 默认端口）
- 前端：`http://<host>:18180`
- 后端：`http://<host>:18081`

## 默认管理账号
可通过环境变量覆盖，默认值如下：
- 用户名：`admin`
- 密码：`Admin@123456`

## CI/CD（Gitee -> Jenkins -> Server）
已内置：
- `Jenkinsfile`
- `scripts/ci/deploy_remote.sh`
- `docs/JENKINS_GITEE_DEPLOY.md`

部署方式为“上传包模式”：Jenkins 在本地打包并上传到服务器，不依赖服务器访问 Gitee。

## 手动部署（不经过 Jenkins）
在项目根目录执行：

```bash
DEPLOY_HOST=45.207.201.227 \
DEPLOY_PORT=22 \
DEPLOY_USER=root \
DEPLOY_PATH=/opt/ai-resume \
./scripts/ci/deploy_remote.sh
```
