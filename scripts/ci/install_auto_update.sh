#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/resume-ai-stack}"
SERVICE_SRC="${APP_DIR}/deploy/systemd/resume-ai-auto-update.service"
TIMER_SRC="${APP_DIR}/deploy/systemd/resume-ai-auto-update.timer"
SERVICE_DST='/etc/systemd/system/resume-ai-auto-update.service'
TIMER_DST='/etc/systemd/system/resume-ai-auto-update.timer'

if [ "$(id -u)" -ne 0 ]; then
  echo '[install-auto-update] please run as root'
  exit 1
fi

if [ ! -f "${SERVICE_SRC}" ] || [ ! -f "${TIMER_SRC}" ]; then
  echo '[install-auto-update] service or timer template not found'
  exit 1
fi

chmod +x "${APP_DIR}/scripts/ci/pull_deploy.sh"
install -m 644 "${SERVICE_SRC}" "${SERVICE_DST}"
install -m 644 "${TIMER_SRC}" "${TIMER_DST}"

systemctl daemon-reload
systemctl enable --now resume-ai-auto-update.timer
systemctl restart resume-ai-auto-update.timer

systemctl status resume-ai-auto-update.timer --no-pager
systemctl list-timers --all | grep 'resume-ai-auto-update' || true
