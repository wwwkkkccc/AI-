#!/usr/bin/env bash
set -euo pipefail

# 用法示例:
# DEPLOY_HOST=45.207.201.227 DEPLOY_USER=root DEPLOY_PATH=/opt/ai-resume ./scripts/ci/deploy_remote.sh

REPO_URL="${REPO_URL:-https://gitee.com/wangknagchi/ai-resume.git}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
DEPLOY_HOST="${DEPLOY_HOST:-45.207.201.227}"
DEPLOY_PORT="${DEPLOY_PORT:-22}"
DEPLOY_USER="${DEPLOY_USER:-root}"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/ai-resume}"

echo "[deploy] host=${DEPLOY_HOST} port=${DEPLOY_PORT} user=${DEPLOY_USER} path=${DEPLOY_PATH} branch=${DEPLOY_BRANCH}"

ssh -o StrictHostKeyChecking=no -p "${DEPLOY_PORT}" "${DEPLOY_USER}@${DEPLOY_HOST}" <<EOF
set -euo pipefail

if [ ! -d "${DEPLOY_PATH}/.git" ]; then
  mkdir -p "${DEPLOY_PATH}"
  rm -rf "${DEPLOY_PATH}"
  git clone "${REPO_URL}" "${DEPLOY_PATH}"
fi

cd "${DEPLOY_PATH}"
git fetch --all --prune
git checkout "${DEPLOY_BRANCH}"
git reset --hard "origin/${DEPLOY_BRANCH}"

docker compose up -d --build backend frontend
docker compose ps
EOF

echo "[deploy] done"
