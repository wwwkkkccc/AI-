# Gitee + Jenkins 自动部署（上传包模式）

## 1. 目标
- 每次 `git push` 到 Gitee（如 `main`）后自动触发 Jenkins。
- Jenkins 在自己的工作区拉代码、打包、上传到服务器，然后远端执行 `docker compose up -d --build backend frontend`。
- 部署服务器**不需要**能访问 Gitee，避免 `git clone` 超时问题。

## 2. 流程说明
1. Jenkins Checkout 指定分支代码。
2. 执行 `scripts/ci/deploy_remote.sh`：
   - 打包当前工作区（排除 `.git/.env/node_modules/target/dist`）。
   - `scp` 上传压缩包到部署服务器 `/tmp`。
   - 远端解压覆盖代码目录，保留原有 `.env`。
   - 执行 `docker compose up -d --build backend frontend`。

## 3. 启动 Jenkins（Docker）
在 Jenkins 机器执行：

```bash
cd /opt
mkdir -p jenkins-stack
cd jenkins-stack
# 将仓库中的 jenkins/docker-compose.yml 放到当前目录
docker compose up -d
```

访问：`http://<JENKINS_IP>:8080`

## 4. Jenkins 必装插件
- `Git`
- `Pipeline`
- `Gitee`（可选，用于 Push 触发；不装也可用轮询/Webhook 插件替代）

## 5. SSH 密钥准备（无 Jenkins 凭据模式）
`Jenkinsfile` 默认使用 `SSH_KEY_PATH=/var/jenkins_home/.ssh/id_rsa`。  
先在部署服务器执行：

```bash
mkdir -p /opt/jenkins-stack/jenkins_home/.ssh /root/.ssh
chmod 700 /opt/jenkins-stack/jenkins_home/.ssh /root/.ssh
ssh-keygen -t rsa -b 4096 -N "" -f /opt/jenkins-stack/jenkins_home/.ssh/id_rsa
cat /opt/jenkins-stack/jenkins_home/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys
chmod 600 /root/.ssh/authorized_keys
sed -i 's/^PubkeyAuthentication.*/PubkeyAuthentication yes/' /etc/ssh/sshd_config
systemctl restart ssh || systemctl restart sshd
```

## 6. 新建 Pipeline 任务
1. New Item -> Pipeline
2. 选择 `Pipeline script from SCM`
3. SCM 选 `Git`
4. Repository URL：`https://gitee.com/wangknagchi/ai-resume.git`
5. Branch：`*/main`
6. Script Path：`Jenkinsfile`

## 7. 配置 Gitee Webhook
1. Jenkins 任务中开启 Gitee 触发（或对应触发器）。
2. Gitee 仓库 -> WebHooks -> 新建，选择 Push 事件。
3. 填入 Jenkins 任务提供的 webhook 地址。
4. 在 Gitee 点击测试，确认 Jenkins 收到触发。

## 8. 默认部署参数
`Jenkinsfile` 参数默认值：
- `DEPLOY_HOST=45.207.201.227`
- `DEPLOY_PORT=22`
- `DEPLOY_USER=root`
- `DEPLOY_PATH=/opt/resume-ai-stack`
- `DEPLOY_BRANCH=main`

## 9. 常见问题
- `scp: command not found` 或 `ssh: command not found`
  - 在 Jenkins 运行环境安装 `openssh-client`。
- `Permission denied (publickey)`
  - 检查 Jenkins 凭据是否绑定到任务，私钥是否可登录服务器。
- 触发成功但未部署
  - 检查 Jenkins 控制台日志中 `deploy_remote.sh` 输出。
- `docker compose: command not found`
  - 在部署服务器安装 Docker Compose 插件。
