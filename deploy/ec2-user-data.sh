#!/usr/bin/env bash
# EC2 cloud-init user-data (runs once on first boot). Ubuntu 24.04 LTS amd64.
# Target: t3.medium (full stack ~3GB).
#
# Installs Docker + compose plugin, adds a 2G swapfile (safety net for smaller
# instances), and clones the repo. The actual deploy is run over SSH via deploy/deploy.sh.
set -euxo pipefail

REPO_URL="${REPO_URL:-https://github.com/dlalstlr12/study-room-reservation.git}"
APP_USER="ubuntu"
APP_DIR="/home/${APP_USER}/study-room-reservation"

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y ca-certificates curl git

# --- Docker official repo ---
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${VERSION_CODENAME}") stable" > /etc/apt/sources.list.d/docker.list
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable --now docker
usermod -aG docker "${APP_USER}"

# --- 2G swap (avoid build OOM on 1GB/2GB instances) ---
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# --- repo ---
if [ ! -d "${APP_DIR}" ]; then
  sudo -u "${APP_USER}" git clone "${REPO_URL}" "${APP_DIR}"
fi

echo "cloud-init done. Next: ssh in, then  cd ${APP_DIR} && bash deploy/deploy.sh" > /home/${APP_USER}/NEXT_STEPS.txt
chown "${APP_USER}:${APP_USER}" /home/${APP_USER}/NEXT_STEPS.txt
