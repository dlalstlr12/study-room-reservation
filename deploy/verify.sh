#!/usr/bin/env bash
# 배포된 스택을 엔드투엔드로 훑어 "실제로 동작함"을 증명하는 스모크 테스트.
# 출력을 deploy/verify-YYYYmmdd-HHMM.log 로 저장한다 (README/노션 근거 자료).
#
#   BASE=http://<EC2_IP> bash deploy/verify.sh
#   (미지정 시 http://localhost — EC2 인스턴스 안에서 실행하는 경우)
#
# 의존성: bash, curl.  (JSON 파싱은 sed — jq/python 불필요)
set -uo pipefail

BASE="${BASE:-http://localhost}"
LOG="deploy/verify-$(date +%Y%m%d-%H%M).log"
PASS=0; FAIL=0
export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'   # Git Bash 경로 자동변환 방지

exec > >(tee "$LOG") 2>&1

echo "===================================================================="
echo " 스터디룸 예약 — 배포 검증  $(date '+%F %T')"
echo " 대상: $BASE"
echo "===================================================================="

CT='Content-Type: application/json'
RESP=""; BODY=""; CODE=""

# http METHOD PATH [extra curl args...]   → RESP/BODY/CODE 전역에 채운다 (요청 1회)
http() {
  local m="$1" p="$2"; shift 2
  RESP=$(curl -sS -m 25 -w $'\n%{http_code}' -X "$m" "$BASE$p" "$@" 2>/dev/null || true)
  CODE="${RESP##*$'\n'}"
  BODY="${RESP%$'\n'*}"
}
jget()  { printf '%s' "$1" | sed -n 's/.*"'"$2"'"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1; }
jcount(){ printf '%s' "$1" | tr -cd '{' | wc -c | tr -d ' '; }
check() { # check <설명> <실제> <기대>
  if [ "$2" = "$3" ]; then echo "  [PASS] $1 ($2)"; PASS=$((PASS+1));
  else echo "  [FAIL] $1 (기대 $3, 실제 $2)"; FAIL=$((FAIL+1)); fi
}

USER_EMAIL="smoke+$(date +%s)@demo.local"

echo; echo "[1] 헬스체크"
http GET /api/health; echo "     $BODY"
check "GET /api/health" "$CODE" "200"

echo; echo "[2] 데모 관리자 로그인"
http POST /api/auth/login -H "$CT" -d '{"email":"admin@studyroom.local","password":"admin1234"}'
ADMIN_TOKEN=$(jget "$BODY" accessToken)
check "admin 로그인 (200)" "$CODE" "200"
[ -n "$ADMIN_TOKEN" ] && check "admin 액세스 토큰 발급" "ok" "ok" || check "admin 액세스 토큰 발급" "none" "ok"

echo; echo "[3] 신규 회원 가입 + 로그인"
http POST /api/auth/signup -H "$CT" \
  -d "{\"email\":\"$USER_EMAIL\",\"password\":\"verify1234\",\"name\":\"smoke-test\"}"
check "POST /api/auth/signup" "$CODE" "201"
http POST /api/auth/login -H "$CT" -d "{\"email\":\"$USER_EMAIL\",\"password\":\"verify1234\"}"
USER_TOKEN=$(jget "$BODY" accessToken)
check "user 로그인 (200)" "$CODE" "200"
[ -n "$USER_TOKEN" ] && check "user 액세스 토큰 발급" "ok" "ok" || check "user 액세스 토큰 발급" "none" "ok"

AUTH_U="Authorization: Bearer ${USER_TOKEN}"
AUTH_A="Authorization: Bearer ${ADMIN_TOKEN}"

echo; echo "[4] 룸 목록 (공개)"
http GET /api/rooms; echo "     룸 수(근사): $(jcount "$BODY")"
check "GET /api/rooms" "$CODE" "200"

echo; echo "[5] 내 정보 (인증)"
http GET /api/members/me -H "$AUTH_U"
check "GET /api/members/me" "$CODE" "200"

echo; echo "[6] PRO 구독 시작 (인증)"
http POST /api/subscriptions -H "$AUTH_U"; echo "     $BODY"
check "POST /api/subscriptions" "$CODE" "200"

echo; echo "[7] 정기결제 배치 즉시 실행 (ADMIN, Spring Batch)"
http POST /api/admin/billing/run -H "$AUTH_A"; echo "     $BODY"
check "POST /api/admin/billing/run" "$CODE" "200"

echo; echo "[8] 결제 이력 확인 (배치 결과 반영)"
sleep 2
http GET /api/subscriptions/me/payments -H "$AUTH_U"
echo "     결제 건수(근사): $(jcount "$BODY")"
check "GET /api/subscriptions/me/payments" "$CODE" "200"

echo; echo "[9] 전체 공지 발송 (ADMIN -> Kafka -> 알림 파이프라인)"
http POST /api/notifications/announcements -H "$AUTH_A" -H "$CT" \
  -d '{"title":"deploy smoke test","body":"kafka notification pipeline check"}'
check "POST /api/notifications/announcements (202 Accepted)" "$CODE" "202"

echo; echo "[10] 알림 수신 확인 (Kafka 라운드트립 대기)"
sleep 6
http GET /api/notifications -H "$AUTH_U"
NC=$(jcount "$BODY")
echo "     수신 알림 수(근사): $NC"
check "GET /api/notifications" "$CODE" "200"
if [ "${NC:-0}" -ge 1 ]; then check "공지 알림 1건 이상 수신" "ok" "ok"
else check "공지 알림 수신" "none" "ok"; fi

echo; echo "[11] 랭킹 재구축 (ADMIN -> Redis Sorted Set)"
http POST /api/rankings/rebuild -H "$AUTH_A"
check "POST /api/rankings/rebuild" "$CODE" "204"
http GET "/api/rankings?scope=all" -H "$AUTH_U"
check "GET /api/rankings" "$CODE" "200"

echo; echo "[12] Kafka 토픽 목록 (컨테이너 내부)"
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -q '^sr-kafka$'; then
  docker exec sr-kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server kafka:29092 --list 2>/dev/null | sed 's/^/     /' \
    || echo "     (토픽 조회 실패 — 무시 가능)"
else
  echo "     (docker 미접근 — EC2 호스트에서 실행 시 표시됨)"
fi

echo
echo "===================================================================="
echo " 결과:  PASS=$PASS  FAIL=$FAIL"
echo " 로그 저장: $LOG"
echo "===================================================================="
[ "$FAIL" -eq 0 ]
