#!/usr/bin/env bash
# EC2 인스턴스에서 실행. 전체 스택을 빌드·기동하고 헬스체크까지 확인한다.
# 퍼블릭 IP 는 매 재시작마다 바뀌므로 이 스크립트가 IMDSv2 로 조회해 .env 에 반영한다.
#
#   cd ~/study-room-reservation && bash deploy/deploy.sh
set -euo pipefail

cd "$(dirname "$0")/.."
COMPOSE="docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env"

# --- deploy/.env 준비 ---
if [ ! -f deploy/.env ]; then
  cp deploy/.env.prod.example deploy/.env
  # 랜덤 시크릿 자동 생성 (한 번만)
  JWT=$(openssl rand -base64 48 | tr -d '\n/+' | cut -c1-48)
  DBP=$(openssl rand -hex 12)
  DBR=$(openssl rand -hex 12)
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${JWT}|"            deploy/.env
  sed -i "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=${DBP}|"    deploy/.env
  sed -i "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=${DBR}|" deploy/.env
  echo "[deploy] deploy/.env 생성 + 랜덤 시크릿 주입"
fi

# --- 퍼블릭 IP 조회 (IMDSv2) 후 CORS 오리진 갱신 ---
TOKEN=$(curl -sS -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 300" || true)
PUBLIC_IP=$(curl -sS -H "X-aws-ec2-metadata-token: ${TOKEN}" \
  http://169.254.169.254/latest/meta-data/public-ipv4 || true)

if [ -n "${PUBLIC_IP}" ]; then
  ORIGIN="http://${PUBLIC_IP}"
  sed -i "s|^APP_CORS_ALLOWED_ORIGINS=.*|APP_CORS_ALLOWED_ORIGINS=${ORIGIN}|" deploy/.env
  echo "[deploy] APP_CORS_ALLOWED_ORIGINS=${ORIGIN}"
else
  echo "[deploy] 경고: 퍼블릭 IP 조회 실패 — deploy/.env 의 APP_CORS_ALLOWED_ORIGINS 를 직접 확인하세요"
fi

# --- 빌드 & 기동 ---
$COMPOSE up -d --build

# --- 헬스 대기 ---
echo -n "[deploy] backend 헬스 대기"
for i in $(seq 1 40); do
  if curl -fsS http://localhost/api/health >/dev/null 2>&1; then
    echo " — OK"
    break
  fi
  echo -n "."
  sleep 5
  if [ "$i" -eq 40 ]; then
    echo " — 실패. 로그: $COMPOSE logs backend"
    exit 1
  fi
done

echo
echo "[deploy] 완료"
echo "  프론트   : http://${PUBLIC_IP:-<EC2_IP>}"
echo "  API 헬스 : http://${PUBLIC_IP:-<EC2_IP>}/api/health"
echo "  Swagger  : http://${PUBLIC_IP:-<EC2_IP>}/swagger-ui.html"
echo "  Kafka UI : http://${PUBLIC_IP:-<EC2_IP>}:8085  (보안그룹에서 8085 열려 있어야 함)"
echo "  데모 관리자: admin@studyroom.local / admin1234"
