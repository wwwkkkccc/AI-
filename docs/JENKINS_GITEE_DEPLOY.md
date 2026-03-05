# Gitee + Jenkins 自动部署说明

## 1. 目标
- 每次 `git push` 到 Gitee（例如 `main` 分支）后，Jenkins 自动触发构建。
- Jenkins 通过 SSH 登录部署机，自动执行：
  - 拉取最新代码
  - 重建并更新 `backend/frontend` 容器

## 2. 启动 Jenkins（Docker）
在 Jenkins 服务器执行：

```bash
cd /opt
mkdir -p jenkins
cd jenkins
# 把本仓库里的 jenkins/docker-compose.yml 放到当前目录
docker compose up -d
```

访问：`http://<JENKINS_IP>:8080`

## 3. Jenkins 必装插件
- `Git`
- `Pipeline`
- `SSH Agent`
- `Gitee`（用于 Gitee Push 触发；若未装可改用轮询或 Generic Webhook）

## 4. Jenkins 凭据准备
在 Jenkins -> `Manage Credentials` 新增 SSH 私钥凭据：
- 类型：`SSH Username with private key`
- ID：`resume-ai-server-ssh`（与 `Jenkinsfile` 保持一致）
- 用户：部署服务器 SSH 用户（例如 `root`）
- 私钥：对应服务器可登录私钥

## 5. 创建流水线任务
1. New Item -> Pipeline
2. 选择 `Pipeline script from SCM`
3. SCM 选择 `Git`
4. Repository URL：`https://gitee.com/wangknagchi/ai-resume.git`
5. Branch：`*/main`
6. Script Path：`Jenkinsfile`

## 6. 配置 Gitee 触发
1. 在 Jenkins 任务中启用 “Gitee webhook 触发构建”（或同类选项）
2. 在 Gitee 仓库 -> WebHooks 新增触发器，选择 Push 事件
3. 把 Jenkins 页面中给出的 webhook URL 粘贴到 Gitee
4. 保存后，在 Gitee 点“测试”并确认 Jenkins 有构建记录

## 7. 部署逻辑说明
`Jenkinsfile` 远端执行的核心逻辑：
- 若部署目录不存在 `.git`，先 `git clone`
- 每次强制同步到远端分支最新提交
- 执行 `docker compose up -d --build backend frontend`

默认部署参数：
- 部署机：`45.207.201.227`
- 用户：`root`
- 目录：`/opt/ai-resume`
- 分支：`main`

都可在 Jenkins 构建参数中调整。

## 8. 常见问题
- `ssh: permission denied`：
  - 检查 Jenkins 凭据是否正确、服务器是否允许该用户 + 私钥登录。
- `docker compose: command not found`：
  - 在部署机安装 Docker Compose 插件。
- webhook 不触发：
  - 检查 Jenkins 对外地址可达、任务触发开关是否已开启、Gitee webhook 日志返回码。
