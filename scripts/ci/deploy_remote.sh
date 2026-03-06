#!/usr/bin/env bash
set -euo pipefail

DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
DEPLOY_HOST="${DEPLOY_HOST:-45.207.201.227}"
DEPLOY_PORT="${DEPLOY_PORT:-22}"
DEPLOY_USER="${DEPLOY_USER:-root}"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/resume-ai-stack}"
DEPLOY_REPO_URL="${DEPLOY_REPO_URL:-https://github.com/wwwkkkccc/AI-.git}"
PACKAGE_TAG="${PACKAGE_TAG:-local}"
SSH_KEY_PATH="${SSH_KEY_PATH:-}"
DEPLOY_PASSWORD="${DEPLOY_PASSWORD:-}"

if [ -z "${DEPLOY_PATH}" ] || [ "${DEPLOY_PATH}" = "/" ]; then
  echo "[deploy] invalid DEPLOY_PATH: '${DEPLOY_PATH}'"
  exit 1
fi

if [ -n "${SSH_KEY_PATH}" ] && [ ! -f "${SSH_KEY_PATH}" ]; then
  echo "[deploy] SSH_KEY_PATH not found: '${SSH_KEY_PATH}'"
  exit 1
fi

SSH_COMMON_OPTS=(-o StrictHostKeyChecking=no)
if [ -n "${SSH_KEY_PATH}" ]; then
  SSH_COMMON_OPTS+=(-i "${SSH_KEY_PATH}")
fi

SSH_PREFIX=()
if [ -n "${DEPLOY_PASSWORD}" ]; then
  if ! command -v sshpass >/dev/null 2>&1; then
    echo "[deploy] sshpass is required when DEPLOY_PASSWORD is set"
    exit 1
  fi
  export SSHPASS="${DEPLOY_PASSWORD}"
  SSH_PREFIX=(sshpass -e)
fi

SCP_CMD=("${SSH_PREFIX[@]}" scp "${SSH_COMMON_OPTS[@]}" -P "${DEPLOY_PORT}")
SSH_CMD=("${SSH_PREFIX[@]}" ssh "${SSH_COMMON_OPTS[@]}" -p "${DEPLOY_PORT}")

LOCAL_PACKAGE="$(mktemp -u "/tmp/resume-ai-${PACKAGE_TAG}-XXXXXX.tar.gz")"
REMOTE_PACKAGE="/tmp/$(basename "${LOCAL_PACKAGE}")"

cleanup() {
  rm -f "${LOCAL_PACKAGE}" || true
}
trap cleanup EXIT

PACKAGE_ITEMS=(
  docker-compose.yml
  .env.example
  README.md
  PROJECT_DOC.md
  backend
  frontend
  scripts
  docs
)

PACKAGE_ARGS=()
for item in "${PACKAGE_ITEMS[@]}"; do
  if [ -e "${item}" ]; then
    PACKAGE_ARGS+=("${item}")
  fi
done

if [ "${#PACKAGE_ARGS[@]}" -eq 0 ]; then
  echo "[deploy] no package items found"
  exit 1
fi

echo "[deploy] host=${DEPLOY_HOST} port=${DEPLOY_PORT} user=${DEPLOY_USER} path=${DEPLOY_PATH} branch=${DEPLOY_BRANCH}"
echo "[deploy] package -> ${LOCAL_PACKAGE}"

tar -czf "${LOCAL_PACKAGE}" "${PACKAGE_ARGS[@]}"

echo "[deploy] upload -> ${DEPLOY_HOST}:${REMOTE_PACKAGE}"
"${SCP_CMD[@]}" "${LOCAL_PACKAGE}" "${DEPLOY_USER}@${DEPLOY_HOST}:${REMOTE_PACKAGE}"

echo "[deploy] remote deploy"
"${SSH_CMD[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" <<EOF
set -euo pipefail

DEPLOY_PATH='${DEPLOY_PATH}'
REMOTE_PACKAGE='${REMOTE_PACKAGE}'
DEPLOY_REPO_URL='${DEPLOY_REPO_URL}'

if [ -z "\${DEPLOY_PATH}" ] || [ "\${DEPLOY_PATH}" = "/" ]; then
  echo "[deploy] invalid DEPLOY_PATH: '\${DEPLOY_PATH}'"
  exit 1
fi

mkdir -p "\${DEPLOY_PATH}"

ENV_BACKUP=''
if [ -f "\${DEPLOY_PATH}/.env" ]; then
  ENV_BACKUP="/tmp/resume-ai-env-\$\$"
  cp "\${DEPLOY_PATH}/.env" "\${ENV_BACKUP}"
fi

if docker ps -a --format '{{.Names}}' | grep -qx 'resume-ai-jenkins'; then
  docker rm -f resume-ai-jenkins || true
fi

JENKINS_KEY_PATH='/opt/jenkins-stack/jenkins_home/.ssh/id_rsa.pub'
if [ -f "\${JENKINS_KEY_PATH}" ] && [ -f /root/.ssh/authorized_keys ]; then
  AUTH_TMP="/tmp/root-authorized-keys-\$\$"
  grep -Fvx -f "\${JENKINS_KEY_PATH}" /root/.ssh/authorized_keys > "\${AUTH_TMP}" || true
  cat "\${AUTH_TMP}" > /root/.ssh/authorized_keys
  chmod 600 /root/.ssh/authorized_keys
  rm -f "\${AUTH_TMP}"
fi

rm -rf /opt/jenkins-stack

docker images 'jenkins/jenkins' -q | xargs -r docker rmi -f || true

rm -rf   "\${DEPLOY_PATH}/backend"   "\${DEPLOY_PATH}/frontend"   "\${DEPLOY_PATH}/docs"   "\${DEPLOY_PATH}/scripts"   "\${DEPLOY_PATH}/jenkins"   "\${DEPLOY_PATH}/ai-resume"   "\${DEPLOY_PATH}/resume-ai-stack"
rm -f   "\${DEPLOY_PATH}/docker-compose.yml"   "\${DEPLOY_PATH}/README.md"   "\${DEPLOY_PATH}/PROJECT_DOC.md"   "\${DEPLOY_PATH}/.env.example"   "\${DEPLOY_PATH}/.gitignore"   "\${DEPLOY_PATH}/Jenkinsfile"   "\${DEPLOY_PATH}/CLAUDE.md"   "\${DEPLOY_PATH}/PLAN_AI_FEATURES.md"

tar -xzf "\${REMOTE_PACKAGE}" -C "\${DEPLOY_PATH}"
rm -f "\${REMOTE_PACKAGE}"

if [ -n "\${ENV_BACKUP}" ] && [ -f "\${ENV_BACKUP}" ]; then
  mv "\${ENV_BACKUP}" "\${DEPLOY_PATH}/.env"
fi

if [ ! -f "\${DEPLOY_PATH}/.env" ] && [ -f "\${DEPLOY_PATH}/.env.example" ]; then
  cp "\${DEPLOY_PATH}/.env.example" "\${DEPLOY_PATH}/.env"
fi

if [ -d "\${DEPLOY_PATH}/.git" ]; then
  git -C "\${DEPLOY_PATH}" remote set-url origin "\${DEPLOY_REPO_URL}" || true
fi

cd "\${DEPLOY_PATH}"
docker compose up -d --build mysql redis backend frontend
docker compose ps
EOF

echo "[deploy] done"
