# ===== 第一阶段：Maven 构建后端 JAR =====
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先复制 pom.xml 并下载依赖，利用 Docker 缓存
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# 复制源码并打包（跳过测试）
COPY src ./src
RUN mvn -q -DskipTests package

# ===== 第二阶段：运行时镜像 =====
FROM eclipse-temurin:17-jre
WORKDIR /app
# 安装 Tesseract OCR 引擎及中英文语言包（用于简历/JD 图片文字识别）
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng tesseract-ocr-chi-sim \
    && rm -rf /var/lib/apt/lists/*
# 从构建阶段复制打包好的 JAR 文件
COPY --from=build /app/target/resume-ai-backend.jar /app/app.jar
# 暴露后端服务端口
EXPOSE 8080
# 启动 Spring Boot 应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
