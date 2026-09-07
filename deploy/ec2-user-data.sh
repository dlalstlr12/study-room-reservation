#!/usr/bin/env bash
# EC2 인스턴스 최초 부팅 시 1회 실행되는 cloud-init user-data.
# Ubuntu 24.04 LTS (amd64) 기준.  대상: t3.medium 권장(전체 스택 ~3GB).
#
# 하는 일: Docker + compose 플러그인 설치, swap 2G 확보(t3.small 대비 안전판), 저장소 clone.
# 실제 배포는 SSH 접속 후 deploy/deploy.sh 로 수행한다.
set -euxo pipefail

REPO_URL="${REPO_URL:-https://github.com/dlalstlr12/study-room-reservation.git}"
APP_USER="ubuntu"
APP_DIR="/home/${APP_USER}/study-room-reservation"

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y ca-certificates curl git

# --- Docker 공식 저장소 ---
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

# --- swap 2G (1GB/2GB 인스턴스에서 빌드 OOM 방지) ---
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# --- 저장소 ---
if [ ! -d "${APP_DIR}" ]; then
  sudo -u "${APP_USER}" git clone "${REPO_URL}" "${APP_DIR}"
fi

echo "cloud-init 완료. 다음: ssh 접속 후  cd ${APP_DIR} && bash deploy/deploy.sh" > /home/${APP_USER}/NEXT_STEPS.txt
chown "${APP_USER}:${APP_USER}" /home/${APP_USER}/NEXT_STEPS.txt
