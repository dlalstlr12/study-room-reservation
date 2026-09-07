# syntax=docker/dockerfile:1
# 멀티스테이지: (1) Gradle 로 bootJar 빌드 → (2) JRE 런타임에 jar 만 복사

# ---------- build ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# 의존성 레이어 캐시: 빌드 스크립트/래퍼 먼저 복사
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 후 실행 가능 jar 생성 (테스트는 CI 에서 별도 수행)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- runtime ----------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# 헬스체크용 curl + 비루트 실행 계정
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app

COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

# 512MB 프리티어(Render) ~ t3.small 대비: 컨테이너 메모리 비율 힙 + 빠른 기동
# TieredStopAtLevel=1 = C2 컴파일러 생략 → 저사양 CPU 기동 시간 대폭 단축(피크 처리량은 포기)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=demo

EXPOSE 8080
USER app

# 컨테이너 헬스체크(compose 의 healthcheck 와 별개로 이미지 자체에도 명시)
HEALTHCHECK --interval=15s --timeout=3s --start-period=180s --retries=20 \
  CMD curl -fsS http://localhost:${PORT:-8080}/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
