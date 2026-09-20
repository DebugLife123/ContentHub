# ContentHub 本地开发环境搭建记录

## 1. 环境确认

在 Windows PowerShell 中执行：

```powershell
java -version       # JDK 17+
mvn -version        # Maven 3.9+
node -v             # Node.js 18+，当前项目使用 Node 22
npm -v
git --version
docker --version
```

ContentHub 当前使用：JDK 17.0.10、Maven 3.9.14、Node.js 22.19.0、npm 10.9.3、MySQL 8.0、Redis 7。

## 2. 项目目录

```text
ContentHub/
├── backend/     # Spring Boot 多模块工程
├── frontend/    # Vue 3 + Vite 工程
├── docs/        # 数据库脚本和文档
└── README.md
```

## 3. 数据库

当前复用 Docker 容器 `springboot-mall-mysql`，本机端口是 3307，账号是 `root / 123456`。

```powershell
Get-Content .\docs\database.sql | docker exec -i springboot-mall-mysql mysql -uroot -p123456
```

脚本会创建 `contenthub` 数据库及用户、创作者、内容、订阅套餐、订阅、收藏、评论 7 张核心表。

## 4. 启动后端

```powershell
cd .\backend
mvn -DskipTests package
java -jar .\contenthub-web\target\contenthub-web-0.0.1-SNAPSHOT.jar
```

后端地址：`http://127.0.0.1:8084`

登录接口：`POST /login`，示例账号：`creator / 123456`

## 5. 启动前端

```powershell
cd .\frontend
npm install
npm run dev -- --host 127.0.0.1
```

前端地址：`http://127.0.0.1:5175`

Vite 会把 `/api/*` 代理到 `http://127.0.0.1:8084/*`。

## 6. Redis

Redis 已在本机 Docker 中运行，已有容器映射到 6379。当前第一阶段底座还没有强依赖 Redis；后续做验证码、缓存、阅读统计、异步任务时再接入。

## 7. 下一步开发顺序

1. 把旧商城的 Product/Cart/Order 模块替换为 ContentHub 的 Content/Subscription/Comment/Favorite 模块。
2. 建立 `contenthub-common` 下的 DO、Mapper、枚举和分页对象。
3. 在 `contenthub-web` 中增加公开内容 API、订阅 API 和创作者工作台 API。
4. 前端接入真实内容列表、登录状态、订阅流程和创作者数据接口。
5. 再接对象存储（PDF、视频、数据集）和支付渠道。
## 本轮新增内容接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/contents` | 查询已发布内容，可用 `?contentType=PROMPT` 筛选 |
| GET | `/contents/{id}` | 查询单篇已发布内容 |

当前首页和内容详情页已经通过 Vite `/api` 代理调用以上接口。后端开发端口为 `8084`，前端开发端口为 `5175`。
