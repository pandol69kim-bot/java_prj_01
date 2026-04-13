# ── Development stage ─────────────────────────────────────────
FROM gradle:8-jdk17-alpine AS development
WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon --quiet

# 소스는 볼륨 마운트로 주입
EXPOSE 8080
CMD ["gradle", "bootRun", "--no-daemon"]

# ── Build stage ──────────────────────────────────────────────
FROM gradle:8-jdk17-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon --quiet

COPY src/ src/
RUN gradle bootJar --no-daemon -x test

# ── Runtime stage ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
