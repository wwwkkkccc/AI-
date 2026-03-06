#!/usr/bin/env bash
set -euo pipefail

DEPLOY_REPO_URL="${DEPLOY_REPO_URL:-https://github.com/wwwkkkccc/AI-.git}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
SOURCE_REPO_DIR="${SOURCE_REPO_DIR:-/opt/resume-ai-source}"
APP_DIR="${APP_DIR:-/opt/resume-ai-stack}"
LAST_DEPLOY_FILE="${APP_DIR}/.last_deployed_commit"
LOCK_FILE="/tmp/resume-ai-auto-update.lock"
LOG_PREFIX='[auto-update]'

exec 9>"${LOCK_FILE}"
if ! flock -n 9; then
  echo "${LOG_PREFIX} another run is already in progress"
  exit 0
fi

TMP_DIR="$(mktemp -d /tmp/resume-ai-update-XXXXXX)"
ENV_BACKUP=''

cleanup() {
  rm -rf "${TMP_DIR}" || true
  if [ -n "${ENV_BACKUP}" ] && [ -f "${ENV_BACKUP}" ]; then
    rm -f "${ENV_BACKUP}" || true
  fi
}
trap cleanup EXIT

echo "${LOG_PREFIX} repo=${DEPLOY_REPO_URL} branch=${DEPLOY_BRANCH}"

if [ ! -d "${SOURCE_REPO_DIR}/.git" ]; then
  mkdir -p "$(dirname "${SOURCE_REPO_DIR}")"
  git clone --branch "${DEPLOY_BRANCH}" --depth 1 "${DEPLOY_REPO_URL}" "${SOURCE_REPO_DIR}"
else
  git -C "${SOURCE_REPO_DIR}" remote set-url origin "${DEPLOY_REPO_URL}"
  git -C "${SOURCE_REPO_DIR}" fetch --depth 1 origin "${DEPLOY_BRANCH}"
  git -C "${SOURCE_REPO_DIR}" checkout -B "${DEPLOY_BRANCH}" "origin/${DEPLOY_BRANCH}"
  git -C "${SOURCE_REPO_DIR}" reset --hard "origin/${DEPLOY_BRANCH}"
  git -C "${SOURCE_REPO_DIR}" clean -fd
fi

TARGET_SHA="$(git -C "${SOURCE_REPO_DIR}" rev-parse "origin/${DEPLOY_BRANCH}" 2>/dev/null || git -C "${SOURCE_REPO_DIR}" rev-parse HEAD)"
LAST_DEPLOYED="$(cat "${LAST_DEPLOY_FILE}" 2>/dev/null || true)"

if [ "${TARGET_SHA}" = "${LAST_DEPLOYED}" ]; then
  echo "${LOG_PREFIX} no new commit"
  exit 0
fi

mkdir -p "${TMP_DIR}/package"
PACKAGE_ITEMS=(
  docker-compose.yml
  .env.example
  README.md
  PROJECT_DOC.md
  backend
  frontend
  scripts
  docs
  .github
  deploy
)

for item in "${PACKAGE_ITEMS[@]}"; do
  if [ -e "${SOURCE_REPO_DIR}/${item}" ]; then
    cp -a "${SOURCE_REPO_DIR}/${item}" "${TMP_DIR}/package/"
  fi
done

mkdir -p "${APP_DIR}"
if [ -f "${APP_DIR}/.env" ]; then
  ENV_BACKUP="$(mktemp /tmp/resume-ai-env-XXXXXX)"
  cp "${APP_DIR}/.env" "${ENV_BACKUP}"
fi

if docker ps -a --format '{{.Names}}' | grep -qx 'resume-ai-jenkins'; then
  docker rm -f resume-ai-jenkins || true
fi

JENKINS_KEY_PATH='/opt/jenkins-stack/jenkins_home/.ssh/id_rsa.pub'
if [ -f "${JENKINS_KEY_PATH}" ] && [ -f /root/.ssh/authorized_keys ]; then
  AUTH_TMP="/tmp/root-authorized-keys-$$"
  grep -Fvx -f "${JENKINS_KEY_PATH}" /root/.ssh/authorized_keys > "${AUTH_TMP}" || true
  cat "${AUTH_TMP}" > /root/.ssh/authorized_keys
  chmod 600 /root/.ssh/authorized_keys
  rm -f "${AUTH_TMP}"
fi

rm -rf /opt/jenkins-stack
docker images 'jenkins/jenkins' -q | xargs -r docker rmi -f || true

rm -rf \
  "${APP_DIR}/backend" \
  "${APP_DIR}/frontend" \
  "${APP_DIR}/docs" \
  "${APP_DIR}/scripts" \
  "${APP_DIR}/jenkins" \
  "${APP_DIR}/ai-resume" \
  "${APP_DIR}/resume-ai-stack" \
  "${APP_DIR}/.github" \
  "${APP_DIR}/deploy"
rm -f \
  "${APP_DIR}/docker-compose.yml" \
  "${APP_DIR}/README.md" \
  "${APP_DIR}/PROJECT_DOC.md" \
  "${APP_DIR}/.env.example" \
  "${APP_DIR}/.gitignore" \
  "${APP_DIR}/Jenkinsfile" \
  "${APP_DIR}/CLAUDE.md" \
  "${APP_DIR}/PLAN_AI_FEATURES.md"

cp -a "${TMP_DIR}/package/." "${APP_DIR}/"

if [ -n "${ENV_BACKUP}" ] && [ -f "${ENV_BACKUP}" ]; then
  mv "${ENV_BACKUP}" "${APP_DIR}/.env"
  ENV_BACKUP=''
fi

if [ ! -f "${APP_DIR}/.env" ] && [ -f "${APP_DIR}/.env.example" ]; then
  cp "${APP_DIR}/.env.example" "${APP_DIR}/.env"
fi

cd "${APP_DIR}"
docker compose up -d --build mysql redis backend frontend
printf '%s\n' "${TARGET_SHA}" > "${LAST_DEPLOY_FILE}"
docker compose ps

echo "${LOG_PREFIX} deployed ${TARGET_SHA}"
