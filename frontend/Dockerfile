# ── Development stage ─────────────────────────────────────────
FROM node:20-alpine AS development
WORKDIR /app

COPY package*.json ./
RUN npm install

# 소스는 볼륨 마운트로 주입, node_modules는 컨테이너 내부 유지
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host"]

# ── Build stage ──────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Runtime stage ─────────────────────────────────────────────
FROM nginx:1.25-alpine AS runtime

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
