# syntax=docker/dockerfile:1
# 멀티스테이지: (1) Vite 정적 빌드 → (2) nginx 로 정적 파일 서빙 + /api·/ws 리버스 프록시

# ---------- build ----------
FROM node:20-bookworm-slim AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# EC2 자체 호스팅에서는 nginx 가 같은 오리진에서 /api·/ws 를 프록시하므로 빈 값(상대경로).
# Vercel 등 프론트/백엔드 분리 배포 시에는 빌드 시 백엔드 URL 을 주입한다.
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# ---------- runtime ----------
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
