# GitHub 自动拉取部署

适用场景：服务器可以访问 GitHub，但 GitHub Actions 无法稳定 SSH 进入服务器。

## 方案
- 服务器本机每 2 分钟检查一次 GitHub `main`
- 发现新提交后自动更新代码并执行 `docker compose up -d --build mysql redis backend frontend`
- 保留服务器上的 `.env`
- 自动清理旧 Jenkins 容器与遗留 SSH 公钥

## 首次安装
在服务器执行：

```bash
mkdir -p /opt/resume-ai-stack/scripts/ci /opt/resume-ai-stack/deploy/systemd
curl -L https://raw.githubusercontent.com/wwwkkkccc/AI-/main/scripts/ci/pull_deploy.sh -o /opt/resume-ai-stack/scripts/ci/pull_deploy.sh
curl -L https://raw.githubusercontent.com/wwwkkkccc/AI-/main/scripts/ci/install_auto_update.sh -o /opt/resume-ai-stack/scripts/ci/install_auto_update.sh
curl -L https://raw.githubusercontent.com/wwwkkkccc/AI-/main/deploy/systemd/resume-ai-auto-update.service -o /opt/resume-ai-stack/deploy/systemd/resume-ai-auto-update.service
curl -L https://raw.githubusercontent.com/wwwkkkccc/AI-/main/deploy/systemd/resume-ai-auto-update.timer -o /opt/resume-ai-stack/deploy/systemd/resume-ai-auto-update.timer
chmod +x /opt/resume-ai-stack/scripts/ci/pull_deploy.sh /opt/resume-ai-stack/scripts/ci/install_auto_update.sh
/opt/resume-ai-stack/scripts/ci/install_auto_update.sh
```

## 手动触发一次

```bash
systemctl start resume-ai-auto-update.service
journalctl -u resume-ai-auto-update.service -n 100 --no-pager
```

## 查看定时器

```bash
systemctl status resume-ai-auto-update.timer --no-pager
systemctl list-timers --all | grep resume-ai-auto-update
```
