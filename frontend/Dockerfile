# ============================================================
# ContentHub 前端镜像（多阶段构建）
#
# 构建阶段用 Node 跑 `npm run build`（含 vue-tsc 类型检查），
# 运行阶段把 dist 交给 Nginx —— 生产环境跑的是构建产物，
# 而不是 Vite dev server。
# ============================================================

# ---------------- 构建阶段 ----------------
FROM node:22-alpine AS build
WORKDIR /build

# 先装依赖：package.json / lock 没变就能命中缓存。
#
# .npmrc 必须在这一层就 COPY 进来：它声明了 npmmirror 镜像源，
# 而下面这行 RUN npm ci 在它之后才执行。以前只 COPY 了 package.json 与 lock，
# .npmrc 要等后面的 `COPY . .` 才进容器 —— 于是 npm ci 全程在用默认的
# registry.npmjs.org。国内直连它极慢甚至不通，npm 会在重试中崩溃：
#   npm error Exit handler never called!
# 表现是镜像构建失败，但本地跑 npm ci 完全正常，很容易误判成网络抖动。
COPY package.json package-lock.json .npmrc ./
RUN npm ci

COPY . .
# 与本地 `npm run build` 一致，会先做类型检查再打包
RUN npm run build

# ---------------- 运行阶段 ----------------
FROM nginx:1.25-alpine

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
