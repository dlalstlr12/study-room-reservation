# AWS EC2 배포 검증 체크리스트 (로드맵 9단계)

> **목표**: 전체 스택(MySQL·Redis·Kafka·Spring Boot·프론트)을 EC2 한 대에 올려 실제로
> 동작함을 확인하고, 과정·결과를 캡처해 README/노션에 남긴다. 검증이 끝나면 인스턴스를
> **Terminate + 볼륨/스냅샷까지 삭제**해 이후 비용이 0이 되게 정리한다.
>
> 상시 데모는 별도(Render + TiDB Cloud + Upstash) — 이 문서는 "AWS 경험" 확보용 1회성 작업.

담당 표기: **[나]** = 직접, **[클로드]** = 세션에서 대신 실행 가능

---

## 0. 사전 준비 [나]

- [ ] AWS 콘솔 로그인 가능
- [ ] 결제 알림(Billing) 임계 알람 설정 권장: $5 넘으면 메일
- [ ] IAM 사용자 생성 → 액세스 키 발급
  - 권한: `AmazonEC2FullAccess` (검증 끝나고 키 비활성화/삭제)
  - **액세스 키/시크릿을 채팅창에 붙여넣지 말 것.** 아래 2단계에서 터미널로 직접 입력.
- [ ] SSH 키페어용 로컬 디렉터리 준비 (예: `~/.ssh/`)

---

## 1. 로컬에서 먼저 검증 [클로드]  ← AWS 비용 0

AWS 올리기 전에 compose가 깨끗이 뜨는지 로컬에서 확인한다.

```bash
cp deploy/.env.prod.example deploy/.env
# deploy/.env 편집: JWT_SECRET(32자+), APP_CORS_ALLOWED_ORIGINS=http://localhost
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build
BASE=http://localhost bash deploy/verify.sh
```

- [ ] `verify.sh` FAIL=0
- [ ] `http://localhost` 에서 로그인·룸·알림·랭킹·구독 화면 육안 확인
- [ ] `docker compose -f deploy/docker-compose.prod.yml down -v` 로 정리

---

## 2. AWS CLI 설치 + 인증 [클로드 설치 / 나 입력]

```bash
# Windows (PowerShell, 관리자)
winget install -e --id Amazon.AWSCLI
# 또는 https://awscli.amazonaws.com/AWSCLIV2.msi

aws --version
aws configure           # ← Access Key, Secret, region=ap-northeast-2, output=json
aws sts get-caller-identity   # 신원 확인
```

- [ ] `aws sts get-caller-identity` 성공  📸 **캡처①** (계정/사용자 ARN)

---

## 3. 네트워크 · 보안그룹 · 키페어 [클로드]

```bash
export AWS_REGION=ap-northeast-2
KEY_NAME=study-room-demo
SG_NAME=study-room-demo-sg

# 3-1. SSH 키페어 (프라이빗 키를 로컬에 저장)
aws ec2 create-key-pair --key-name $KEY_NAME \
  --query 'KeyMaterial' --output text > ~/.ssh/$KEY_NAME.pem
chmod 400 ~/.ssh/$KEY_NAME.pem

# 3-2. 기본 VPC id
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true \
  --query 'Vpcs[0].VpcId' --output text)

# 3-3. 보안그룹 (내 IP 만 SSH, 80/8085 는 데모용 공개)
MY_IP=$(curl -s https://checkip.amazonaws.com)
SG_ID=$(aws ec2 create-security-group --group-name $SG_NAME \
  --description "study-room demo" --vpc-id $VPC_ID --query 'GroupId' --output text)
aws ec2 authorize-security-group-ingress --group-id $SG_ID \
  --ip-permissions \
  "IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=${MY_IP}/32}]" \
  "IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges=[{CidrIp=0.0.0.0/0}]" \
  "IpProtocol=tcp,FromPort=8085,ToPort=8085,IpRanges=[{CidrIp=0.0.0.0/0}]"
```

- [ ] `~/.ssh/study-room-demo.pem` 생성됨
- [ ] 보안그룹 규칙 확인  📸 **캡처②** (인바운드 규칙)

---

## 4. 인스턴스 시작 [클로드 — 비용 발생, 실행 전 확인]

> **t3.medium (4GB)** 권장. 서울 온디맨드 약 **$0.056/시간** → 검증 3~5시간이면 $0.2~0.3.
> t3.small(2GB)도 가능하나 swap 필요(user-data 에 2G swap 포함).

```bash
# Ubuntu 24.04 LTS (amd64) 최신 AMI — SSM 공개 파라미터
AMI_ID=$(aws ssm get-parameters \
  --names /aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id \
  --query 'Parameters[0].Value' --output text)

INSTANCE_ID=$(aws ec2 run-instances \
  --image-id $AMI_ID --instance-type t3.medium \
  --key-name $KEY_NAME --security-group-ids $SG_ID \
  --block-device-mappings 'DeviceName=/dev/sda1,Ebs={VolumeSize=20,VolumeType=gp3,DeleteOnTermination=true}' \
  --user-data file://deploy/ec2-user-data.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=study-room-demo}]' \
  --query 'Instances[0].InstanceId' --output text)

aws ec2 wait instance-running --instance-ids $INSTANCE_ID
PUBLIC_IP=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)
echo "SSH: ssh -i ~/.ssh/$KEY_NAME.pem ubuntu@$PUBLIC_IP"
```

- [ ] **`DeleteOnTermination=true` 확인** (종료 시 볼륨 자동 삭제 → 잔여 비용 0)
- [ ] 인스턴스 `running`  📸 **캡처③** (EC2 콘솔 인스턴스 목록)

---

## 5. 배포 + 검증 + 캡처 [클로드]

```bash
# cloud-init(도커 설치 + clone) 완료까지 2~3분 대기 후 SSH
ssh -i ~/.ssh/$KEY_NAME.pem ubuntu@$PUBLIC_IP

# --- 인스턴스 안에서 ---
cd ~/study-room-reservation
git pull                       # 최신 배포 브랜치 반영
bash deploy/deploy.sh          # .env 자동 생성 + 퍼블릭 IP CORS 반영 + up --build + 헬스 대기
BASE=http://localhost bash deploy/verify.sh
docker compose -f deploy/docker-compose.prod.yml ps
docker exec sr-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --describe --all-groups
exit
```

브라우저(로컬)에서 `http://<PUBLIC_IP>` 접속:

- [ ] 첫 화면 로딩 📸 **캡처④** 대시보드
- [ ] `admin@studyroom.local / admin1234` 로그인 → 관리자 페이지 📸 **캡처⑤**
- [ ] 룸 홀딩 → 카운트다운 동작 📸 **캡처⑥**
- [ ] 관리자: 정기결제 실행 → 알림 벨에 결제 알림(실시간) 📸 **캡처⑦**
- [ ] 관리자: 전체 공지 → 다른 계정 알림 수신 📸 **캡처⑧**
- [ ] 랭킹 페이지 📸 **캡처⑨**
- [ ] `http://<PUBLIC_IP>:8085` Kafka UI — 토픽/컨슈머 lag 📸 **캡처⑩**
- [ ] `http://<PUBLIC_IP>/swagger-ui.html` 📸 **캡처⑪**
- [ ] `deploy/verify-*.log` 를 로컬로 복사 (`scp`) → `docs/deploy/` 에 보관
- [ ] `docker compose ... logs backend` 부팅 로그 일부 저장

```bash
scp -i ~/.ssh/$KEY_NAME.pem ubuntu@$PUBLIC_IP:'~/study-room-reservation/deploy/verify-*.log' docs/deploy/
```

---

## 6. 정리 — 이후 비용 0 [클로드 — 실행 전 확인]

```bash
# 6-1. 인스턴스 종료 (볼륨은 DeleteOnTermination=true 라 함께 삭제됨)
aws ec2 terminate-instances --instance-ids $INSTANCE_ID
aws ec2 wait instance-terminated --instance-ids $INSTANCE_ID

# 6-2. 잔여 리소스 삭제
aws ec2 delete-security-group --group-id $SG_ID
aws ec2 delete-key-pair --key-name $KEY_NAME
rm ~/.ssh/$KEY_NAME.pem

# 6-3. 스냅샷/AMI 없는지 확인 (없어야 정상)
aws ec2 describe-snapshots --owner-ids self --query 'Snapshots[].SnapshotId'
aws ec2 describe-images   --owners self     --query 'Images[].ImageId'

# 6-4. 볼륨 잔여 없는지
aws ec2 describe-volumes --query 'Volumes[].VolumeId'
```

- [ ] 인스턴스 `terminated` 📸 **캡처⑫**
- [ ] 스냅샷/AMI/볼륨 목록 비어 있음
- [ ] IAM 액세스 키 비활성화 또는 삭제 [나]
- [ ] 며칠 뒤 Billing 대시보드에서 청구액 확인 (예상: $1 미만) 📸 **캡처⑬**

---

## 7. 기록 [클로드]

- [ ] `docs/deploy/` 에 캡처 + `verify-*.log` + 부팅 로그 정리
- [ ] `README.md` 에 "AWS EC2 배포 검증" 섹션 추가 (구성도 + 캡처 + 로그 링크 +
      "비용 절감을 위해 검증 후 인스턴스 종료, 상시 데모는 Render/TiDB/Upstash" 명시)
- [ ] 노션 허브에 "9단계 — 배포" 항목: AWS 검증 + 상시 배포 구성 요약
- [ ] `docs/roadmap.md` 9단계 체크

---

## 캡처 목록 요약

| # | 화면 | 어디서 |
|---|---|---|
| ① | `aws sts get-caller-identity` | 터미널 |
| ② | 보안그룹 인바운드 규칙 | EC2 콘솔 |
| ③ | 인스턴스 running | EC2 콘솔 |
| ④~⑨ | 앱 기능 동작 (대시보드/관리자/홀딩/결제알림/공지/랭킹) | `http://<IP>` |
| ⑩ | Kafka UI 토픽·lag | `http://<IP>:8085` |
| ⑪ | Swagger UI | `http://<IP>/swagger-ui.html` |
| ⑫ | 인스턴스 terminated | EC2 콘솔 |
| ⑬ | Billing 청구액 | Billing 대시보드 |

> ①②③⑫⑬ (콘솔/터미널)은 본인이 캡처, ④~⑪ (앱 화면)은 클로드가 브라우저로 캡처 가능.
