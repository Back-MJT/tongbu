# ===============================================================
# 昕动智能 RuoYi 后端 Dockerfile
# Java 17 + Maven + Spring Boot 4
# ===============================================================

# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 拷贝 POM（利用 Docker 缓存层）
COPY pom.xml .
COPY ruoyi-admin/pom.xml        ruoyi-admin/
COPY ruoyi-framework/pom.xml     ruoyi-framework/
COPY ruoyi-system/pom.xml        ruoyi-system/
COPY ruoyi-common/pom.xml        ruoyi-common/
COPY ruoyi-iot/pom.xml           ruoyi-iot/
COPY ruoyi-intervention/pom.xml  ruoyi-intervention/

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B || true

# 拷贝源码
COPY . .

# 编译 (跳过测试加快CI)
RUN mvn package -DskipTests -B -q

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-jammy AS runtime

# 安装 curl (用于 healthcheck)
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 创建非root用户
RUN groupadd --gid 1000 ruoyi && \
    useradd --uid 1000 --gid ruoyi --shell /bin/bash --create-home ruoyi
USER ruoyi

# 拷贝编译产物
COPY --from=builder /app/ruoyi-admin/target/ruoyi-admin.jar app.jar

# 日志目录
RUN mkdir -p /app/logs && chown ruoyi:ruoyi /app/logs

# 环境变量
ENV JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE="druid,iot" \
    TZ="Asia/Shanghai"

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:8080/ || exit 1

# 启动
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
