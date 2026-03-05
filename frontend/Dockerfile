# ===== 第一阶段：构建前端静态资源 =====
FROM node:20-alpine AS build
WORKDIR /app
# 先复制依赖描述文件，利用 Docker 缓存加速
COPY package.json package-lock.json* ./
# 安装项目依赖
RUN npm install
# 复制全部源码
COPY . .
# 执行生产构建，输出到 dist 目录
RUN npm run build

# ===== 第二阶段：使用 Nginx 托管静态文件 =====
FROM nginx:1.27-alpine
# 复制自定义 Nginx 配置（含反向代理规则）
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 从构建阶段复制打包产物到 Nginx 静态目录
COPY --from=build /app/dist /usr/share/nginx/html
# 暴露 80 端口供外部访问
EXPOSE 80
