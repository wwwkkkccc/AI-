#!/usr/bin/env bash
set -euo pipefail

# Usage:
# DEPLOY_HOST=45.207.201.227 DEPLOY_USER=root DEPLOY_PATH=/opt/resume-ai-stack ./scripts/ci/deploy_remote.sh

DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
DEPLOY_HOST="${DEPLOY_HOST:-45.207.201.227}"
DEPLOY_PORT="${DEPLOY_PORT:-22}"
DEPLOY_USER="${DEPLOY_USER:-root}"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/resume-ai-stack}"
PACKAGE_TAG="${PACKAGE_TAG:-local}"

if [ -z "${DEPLOY_PATH}" ] || [ "${DEPLOY_PATH}" = "/" ]; then
  echo "[deploy] invalid DEPLOY_PATH: '${DEPLOY_PATH}'"
  exit 1
fi

LOCAL_PACKAGE="$(mktemp -u "/tmp/resume-ai-${PACKAGE_TAG}-XXXXXX.tar.gz")"
REMOTE_PACKAGE="/tmp/$(basename "${LOCAL_PACKAGE}")"

cleanup() {
  rm -f "${LOCAL_PACKAGE}" || true
}
trap cleanup EXIT

echo "[deploy] host=${DEPLOY_HOST} port=${DEPLOY_PORT} user=${DEPLOY_USER} path=${DEPLOY_PATH} branch=${DEPLOY_BRANCH}"
echo "[deploy] pack local source -> ${LOCAL_PACKAGE}"

tar \
  --exclude-vcs \
  --exclude="./.git" \
  --exclude="./.env" \
  --exclude="./frontend/node_modules" \
  --exclude="./frontend/dist" \
  --exclude="./backend/target" \
  --exclude="./jenkins/jenkins_home" \
  -czf "${LOCAL_PACKAGE}" .

echo "[deploy] upload package -> ${DEPLOY_HOST}:${REMOTE_PACKAGE}"
scp -o StrictHostKeyChecking=no -P "${DEPLOY_PORT}" "${LOCAL_PACKAGE}" "${DEPLOY_USER}@${DEPLOY_HOST}:${REMOTE_PACKAGE}"

echo "[deploy] execute remote deploy"
ssh -o StrictHostKeyChecking=no -p "${DEPLOY_PORT}" "${DEPLOY_USER}@${DEPLOY_HOST}" <<EOF
set -euo pipefail

DEPLOY_PATH='${DEPLOY_PATH}'
REMOTE_PACKAGE='${REMOTE_PACKAGE}'

if [ -z "\${DEPLOY_PATH}" ] || [ "\${DEPLOY_PATH}" = "/" ]; then
  echo "[deploy] invalid DEPLOY_PATH: '\${DEPLOY_PATH}'"
  exit 1
fi

mkdir -p "\${DEPLOY_PATH}"

# Keep existing .env on server
ENV_BACKUP=''
if [ -f "\${DEPLOY_PATH}/.env" ]; then
  ENV_BACKUP="/tmp/resume-ai-env-\$\$"
  cp "\${DEPLOY_PATH}/.env" "\${ENV_BACKUP}"
fi

# Remove old code folders/files to avoid stale artifacts
rm -rf "\${DEPLOY_PATH}/backend" "\${DEPLOY_PATH}/frontend" "\${DEPLOY_PATH}/docs" "\${DEPLOY_PATH}/scripts" "\${DEPLOY_PATH}/jenkins"
rm -f "\${DEPLOY_PATH}/docker-compose.yml" "\${DEPLOY_PATH}/README.md" "\${DEPLOY_PATH}/PROJECT_DOC.md" "\${DEPLOY_PATH}/.env.example" "\${DEPLOY_PATH}/.gitignore" "\${DEPLOY_PATH}/Jenkinsfile"

tar -xzf "\${REMOTE_PACKAGE}" -C "\${DEPLOY_PATH}"
rm -f "\${REMOTE_PACKAGE}"

if [ -n "\${ENV_BACKUP}" ] && [ -f "\${ENV_BACKUP}" ]; then
  mv "\${ENV_BACKUP}" "\${DEPLOY_PATH}/.env"
fi

if [ ! -f "\${DEPLOY_PATH}/.env" ] && [ -f "\${DEPLOY_PATH}/.env.example" ]; then
  cp "\${DEPLOY_PATH}/.env.example" "\${DEPLOY_PATH}/.env"
fi

cd "\${DEPLOY_PATH}"
docker compose up -d --build backend frontend
docker compose ps
EOF

echo "[deploy] done"
