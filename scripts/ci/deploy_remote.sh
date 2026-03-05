#!/usr/bin/env bash
# 远程部署脚本：将本地源码打包上传到远程服务器，通过 Docker Compose 构建并启动服务
set -euo pipefail

# 使用示例：
# DEPLOY_HOST=45.207.201.227 DEPLOY_USER=root DEPLOY_PATH=/opt/resume-ai-stack ./scripts/ci/deploy_remote.sh
# SSH_KEY_PATH=/path/to/id_rsa DEPLOY_HOST=45.207.201.227 ./scripts/ci/deploy_remote.sh

# ===== 环境变量（带默认值） =====
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"                    # 部署分支
DEPLOY_HOST="${DEPLOY_HOST:-45.207.201.227}"              # 目标服务器 IP
DEPLOY_PORT="${DEPLOY_PORT:-22}"                          # SSH 端口
DEPLOY_USER="${DEPLOY_USER:-root}"                        # SSH 登录用户
DEPLOY_PATH="${DEPLOY_PATH:-/opt/resume-ai-stack}"        # 服务器上的部署目录
PACKAGE_TAG="${PACKAGE_TAG:-local}"                       # 打包标签（用于区分不同构建）
SSH_KEY_PATH="${SSH_KEY_PATH:-}"                           # SSH 私钥路径（可选）

# 校验部署路径，防止误删根目录
if [ -z "${DEPLOY_PATH}" ] || [ "${DEPLOY_PATH}" = "/" ]; then
  echo "[deploy] invalid DEPLOY_PATH: '${DEPLOY_PATH}'"
  exit 1
fi

# 校验 SSH 私钥文件是否存在
if [ -n "${SSH_KEY_PATH}" ] && [ ! -f "${SSH_KEY_PATH}" ]; then
  echo "[deploy] SSH_KEY_PATH not found: '${SSH_KEY_PATH}'"
  exit 1
fi

# ===== 构建 SSH/SCP 命令参数 =====
SSH_COMMON_OPTS=(-o StrictHostKeyChecking=no)  # 跳过主机密钥确认
if [ -n "${SSH_KEY_PATH}" ]; then
  SSH_COMMON_OPTS+=(-i "${SSH_KEY_PATH}")      # 指定私钥文件
fi

SCP_CMD=(scp "${SSH_COMMON_OPTS[@]}" -P "${DEPLOY_PORT}")   # SCP 上传命令
SSH_CMD=(ssh "${SSH_COMMON_OPTS[@]}" -p "${DEPLOY_PORT}")   # SSH 远程执行命令

# ===== 生成临时打包文件路径 =====
LOCAL_PACKAGE="$(mktemp -u "/tmp/resume-ai-${PACKAGE_TAG}-XXXXXX.tar.gz")"   # 本地临时压缩包
REMOTE_PACKAGE="/tmp/$(basename "${LOCAL_PACKAGE}")"                          # 远程服务器上的压缩包路径

# 脚本退出时自动清理本地临时文件
cleanup() {
  rm -f "${LOCAL_PACKAGE}" || true
}
trap cleanup EXIT

echo "[deploy] host=${DEPLOY_HOST} port=${DEPLOY_PORT} user=${DEPLOY_USER} path=${DEPLOY_PATH} branch=${DEPLOY_BRANCH}"
echo "[deploy] pack local source -> ${LOCAL_PACKAGE}"

# 打包本地源码（排除不需要的目录和文件）
tar \
  --exclude-vcs \
  --exclude="./.git" \
  --exclude="./.env" \
  --exclude="./frontend/node_modules" \
  --exclude="./frontend/dist" \
  --exclude="./backend/target" \
  --exclude="./jenkins/jenkins_home" \
  -czf "${LOCAL_PACKAGE}" .

# 通过 SCP 上传压缩包到远程服务器
echo "[deploy] upload package -> ${DEPLOY_HOST}:${REMOTE_PACKAGE}"
"${SCP_CMD[@]}" "${LOCAL_PACKAGE}" "${DEPLOY_USER}@${DEPLOY_HOST}:${REMOTE_PACKAGE}"

# ===== 通过 SSH 在远程服务器上执行部署 =====
echo "[deploy] execute remote deploy"
"${SSH_CMD[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" <<EOF
set -euo pipefail

DEPLOY_PATH='${DEPLOY_PATH}'
REMOTE_PACKAGE='${REMOTE_PACKAGE}'

# 再次校验部署路径（远程侧安全检查）
if [ -z "\${DEPLOY_PATH}" ] || [ "\${DEPLOY_PATH}" = "/" ]; then
  echo "[deploy] invalid DEPLOY_PATH: '\${DEPLOY_PATH}'"
  exit 1
fi

# 确保部署目录存在
mkdir -p "\${DEPLOY_PATH}"

# 备份服务器上已有的 .env 文件（保留生产环境配置）
ENV_BACKUP=''
if [ -f "\${DEPLOY_PATH}/.env" ]; then
  ENV_BACKUP="/tmp/resume-ai-env-\$\$"
  cp "\${DEPLOY_PATH}/.env" "\${ENV_BACKUP}"
fi

# 删除旧的代码目录和文件，避免残留过期文件
rm -rf "\${DEPLOY_PATH}/backend" "\${DEPLOY_PATH}/frontend" "\${DEPLOY_PATH}/docs" "\${DEPLOY_PATH}/scripts" "\${DEPLOY_PATH}/jenkins"
rm -f "\${DEPLOY_PATH}/docker-compose.yml" "\${DEPLOY_PATH}/README.md" "\${DEPLOY_PATH}/PROJECT_DOC.md" "\${DEPLOY_PATH}/.env.example" "\${DEPLOY_PATH}/.gitignore" "\${DEPLOY_PATH}/Jenkinsfile"

# 解压新代码到部署目录
tar -xzf "\${REMOTE_PACKAGE}" -C "\${DEPLOY_PATH}"
# 清理远程临时压缩包
rm -f "\${REMOTE_PACKAGE}"

# 恢复之前备份的 .env 文件
if [ -n "\${ENV_BACKUP}" ] && [ -f "\${ENV_BACKUP}" ]; then
  mv "\${ENV_BACKUP}" "\${DEPLOY_PATH}/.env"
fi

# 如果服务器上没有 .env 文件，则从示例文件创建
if [ ! -f "\${DEPLOY_PATH}/.env" ] && [ -f "\${DEPLOY_PATH}/.env.example" ]; then
  cp "\${DEPLOY_PATH}/.env.example" "\${DEPLOY_PATH}/.env"
fi

# 进入部署目录，使用 Docker Compose 构建并启动后端和前端服务
cd "\${DEPLOY_PATH}"
docker compose up -d --build backend frontend
# 显示容器运行状态
docker compose ps
EOF

echo "[deploy] done"
