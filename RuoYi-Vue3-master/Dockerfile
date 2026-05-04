# ===============================================================
# 昕动智能 RuoYi-Vue3 前端 Dockerfile
# Node.js 18 + Nginx
# ===============================================================

# ---- Build Stage ----
FROM docker.m.daocloud.io/node:18-alpine AS builder

WORKDIR /app

# 拷贝 package 文件（利用 Docker 缓存层）
COPY package.json package-lock.json* ./

# 安装依赖
RUN npm install --registry=https://registry.npmmirror.com

# 拷贝源码
COPY . .

# 构建生产版本
RUN npm run build:prod

# ---- Runtime Stage ----
FROM docker.m.daocloud.io/nginx:1.25-alpine AS runtime

# 拷贝自定义 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 拷贝构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
