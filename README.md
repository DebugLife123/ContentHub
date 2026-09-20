# ContentHub · 数字内容订阅与创作者平台

> 创作者发布数字内容 → 用户订阅 → 根据订阅获得内容权限 → 阅读/观看/下载 → 产生互动。
>
> 开发路线、阶段划分与技术选型一律以根目录 `ContentHub_开发指导计划.docx` 为准，本 README 只做两件事：说明启动方式，记录与计划对齐的当前进度。

## 项目定位

ContentHub 不是一个普通"卖电子书"的商城，而是一个"创作者发布数字内容 → 用户订阅 → 根据订阅获得内容权限 → 阅读/观看/下载 → 产生互动"的小型内容平台。

真正要练的不是 CRUD，而是完整业务链：用户是谁、内容是谁发布的、订阅了什么、订阅后能看什么、哪些内容免费、哪些内容需要权限、热门内容怎么统计，以及这些数据怎样落到 MySQL 和 Redis。

支持内容形态：文章 / PDF / 视频链接 / 代码模板 / Prompt 等数字内容。

## 业务主线（计划 §6）

| 业务 | 流程 |
|---|---|
| 注册登录 | 注册 → 密码加密保存 → 登录 → JWT → Redis 保存 token → 前端保存登录状态 |
| 发布内容 | 创作者登录 → 填写内容 → 保存草稿 → 发布 → 管理员审核 → 上线 |
| 订阅 | 用户查看套餐 → 选择套餐 → 模拟支付 → 创建订阅 → 计算到期时间 → 获得访问权限 |
| 访问内容 | 请求内容 → 判断免费/付费 → 判断用户是否有有效订阅 → 有权限才返回正文/下载地址 |

## 技术栈（计划 §3 表 2）

| 部分 | 计划技术 | 当前状态 |
|---|---|---|
| 前端 | Vue 3 + TypeScript + Vite | ✅ Vue 3 + **TypeScript 5.9** + Vite 4，`vue-tsc` 类型检查已接入构建 |
| 前端状态 | Pinia + Axios + Element Plus | ✅ 已就绪，Pinia 持久化插件已注册 |
| 后端 | Java 17 + Spring Boot 3 | ✅ **Java 17 + Spring Boot 3.2.5** |
| 数据库 | MySQL + MyBatis-Plus | ✅ 已就绪（`contenthub` 库 @3307，MyBatis-Plus 3.5.5） |
| 权限 | Spring Security + JWT | 🟡 JWT 登录链路已通；Redis 持久化 token 与角色授权未做（阶段 2） |
| 缓存 | Redis | ✅ 已接入（`spring.data.redis`，127.0.0.1:6379） |
| 部署 | Docker + Nginx | ⬜ 未开始（阶段 7） |

## 目录结构

```text
ContentHub/
├── backend/                     # Spring Boot 多模块后端
│   ├── contenthub-common/       # DO / Mapper / 枚举 / 异常 / 工具类
│   ├── contenthub-jwt/          # JWT 认证模块：登录过滤器 / Token
│   ├── contenthub-admin/        # 管理端配置：Security 配置
│   └── contenthub-web/          # Web 启动模块：Controller / Service + 配置
├── frontend/                    # Vue3 + TypeScript + Vite 前端
├── docs/
│   └── database.sql             # 数据库建表脚本 + 演示数据（同时作为 MySQL 容器初始化脚本）
├── scripts/                     # 本地开发启停脚本
│   ├── _common.ps1              # 公共函数（原生命令调用、端口探测、等待）
│   ├── dev-up.ps1               # 拉起容器 + 后端 + 前端
│   ├── dev-down.ps1             # 停止后端 / 前端
│   ├── dev-status.ps1           # 状态总览
│   └── register-autostart.ps1   # 注册 / 卸载登录自启任务
├── docker-compose.yml           # ContentHub 专属 MySQL(3308) + Redis(6380)
├── ContentHub_开发指导计划.docx  # 开发路线的唯一依据
└── README.md
```

## 当前进度（按计划 §10 表 9 的阶段口径）

| 阶段 | 天数 | 主要成果 | 状态 |
|---|---|---|---|
| 阶段 0 | Day 1-4 | 环境、Git、项目骨架、数据库连接 | ✅ 已完成 |
| 阶段 1 | Day 5-12 | Vue3 基础 + 用户/分类/内容基础 CRUD | 🟡 进行中 |
| 阶段 2 | Day 13-19 | Spring Security + JWT + Redis 登录权限 | 🟡 部分完成（仅脚手架继承部分） |
| 阶段 3 | Day 20-29 | 内容中心：发布、详情、审核、权限访问 | ⬜ 未开始 |
| 阶段 4 | Day 30-38 | 套餐、订阅、模拟支付、订阅到期 | ⬜ 未开始 |
| 阶段 5 | Day 39-47 | Redis 缓存、热门、搜索、收藏、评论、历史 | ⬜ 未开始 |
| 阶段 6 | Day 48-55 | 创作者中心 + 管理后台 | ⬜ 未开始 |
| 阶段 7 | Day 56-60 | 联调、异常处理、Docker、Nginx、部署 | ⬜ 未开始 |
| 可选升级 | Day 61-70 | Spring AI / RAG / AI 内容助手 | ⬜ 未开始 |

**结论：阶段 0 已完成并通过验收；当前处于「阶段 1 中段」，约合计划的 Day 8-10。**

### 阶段 0 已完成（含验收方式）

计划阶段 0 的四条验收标准全部满足：

| 计划验收标准 | 状态 | 验证方式 |
|---|---|---|
| 能启动后端并访问一个 hello API | ✅ | `POST /admin/test`（需登录） |
| 能启动前端并显示首页 | ✅ | `http://127.0.0.1:5175` |
| 后端能连 MySQL | ✅ | `GET /contents` 返回 3 条种子数据 |
| 后端能连 Redis | ✅ | `GET /admin/redis/verify`，并在 `redis-cli` 中查到明文 key |

本轮完成的技术栈对齐（计划 §3 表 2）：

- **Spring Boot 2.6.3 → 3.2.5**，`java.version` 8 → 17；
- **springfox → springdoc-openapi 2.3.0**（Knife4j 换用 `knife4j-openapi3-jakarta-spring-boot-starter` 4.5.0），原 `Docket`/`@EnableSwagger2WebMvc` 重写为 `OpenAPI` + `GroupedOpenApi`；
- **MyBatis-Plus** `mybatis-plus-boot-starter` 3.5.2 → `mybatis-plus-spring-boot3-starter` 3.5.5；
- **MySQL 驱动** `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`；
- **`javax.*` → `jakarta.*`**：24 处（servlet 16 处、validation 3 处等），`JwtTokenHelper` 中 JDK 自带的 `javax.security.auth.login` 保持不变；
- **Spring Security 6**：移除已删除的 `WebSecurityConfigurerAdapter`，改为 `SecurityFilterChain` Bean + Lambda DSL，`mvcMatchers` → `requestMatchers`；
- **Redis 接入**：`spring.data.redis` 配置（Boot 3 前缀）、`RedisConfig`（key 用 String、value 用 JSON，便于 `redis-cli` 观察）、补充 `StringRedisTemplate`；
- **前端 TypeScript**：`typescript` 5.9 + `vue-tsc`，`tsconfig.json`（`strict`，`allowJs` 渐进迁移），`vite.config.ts`，`npm run build` 已串入类型检查；核心模块 `axios` / `router` / `stores` / `composables` / `api` / `utils` 及 4 个 ContentHub 页面均已迁移为 TS。

### 阶段 0 遗留说明

- `contenthub-admin` 的 `JwtAuthenticationSecurityConfig` 仍使用 `SecurityConfigurerAdapter` + `http.apply()`，编译通过但有「已废弃并标记删除」告警。建议在阶段 2 重写认证与角色授权时一并改为显式注册 `AuthenticationProvider` / filter Bean；
- 旧商城的 `Cart` / `Order` / `Product` 模块未删除，属于阶段 1 的清理范围。其中 `OrderProductVO` 被 MyBatis-Plus 误当作实体扫描，启动时会打印两条 `Can not find table primary key` 警告，随阶段 1 清理一并消失；
- `GET /admin/redis/verify` 是阶段 0 的验收用临时接口，阶段 2 落地真实 Redis 业务后应删除。

### 阶段 1 已完成

- `users`、`contents` 表 + `UserDO` / `ContentDO` + Mapper；
- 注册接口 `POST /register`（校验两次密码一致，密码用 BCrypt 加密后入库）；
- 内容列表 `GET /contents`（支持 `contentType` 筛选）、内容详情 `GET /contents/{id}`，仅返回 `PUBLISHED` 内容；
- 首页与内容详情页已改为从后端读取真实数据；
- 统一返回 `Response` 与全局异常处理 `GlobalExceptionHandler`。

### 阶段 1 未完成

- **分类模块整体缺失**：计划表 6 中标记为"必须"的 `content_category` 表未建，无任何分类代码；
- **内容 CRUD 缺失**：只有两个 GET，无新增 / 编辑 / 删除 / 下架；
- **分页缺失**：列表直接 `selectList` 返回全量，未使用 MyBatis-Plus 分页；
- 参数校验、DTO/VO 分层未覆盖内容模块；
- 旧商城模块未替换：`Cart` / `Order` / `Product` 的 DO、Mapper、Controller、Service 全部仍在；
- 尚未打 Git tag `v0.1`（计划 Day 12 的里程碑）。

### 阶段 2 已完成

- JWT 生成与解析、Spring Security 配置、BCrypt 密码编码、`POST /login` 登录链路可用；
- `users.role` 字段与 `USER` / `CREATOR` / `ADMIN` 三角色种子数据已就绪。

### 阶段 2 未完成

- **token 未写入 Redis**（计划 Day 16 的核心），当前 token 只存在于 JWT 本身，前端存 `localStorage`；
- **角色授权未做**：`WebSecurityConfig` 只保护 `/admin/**`，其余为 `anyRequest().permitAll()`，未按 USER / CREATOR / ADMIN 区分；
- 前端无路由守卫，`stores/user.js` 与 `composables/auth.js` 未接入登录流程；
- 缺少登出接口。

### 阶段 3 及以后

均未开始。已建表但**尚无对应代码**的有：`creator_profiles`、`subscription_plans`、`subscriptions`、`favorites`、`comments`。

`CreatorDashboard.vue` 目前是纯静态假数据，不是真实接口。计划表 6 中的 `content_category`（必须）与 `reading_history`（建议）两张表尚未创建。

## 已实现接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 登录，返回 JWT（计划中的 `/api/auth/login`） |
| POST | `/register` | 注册（计划中的 `/api/auth/register`） |
| POST | `/user/info` | 获取当前用户信息 |
| GET | `/contents` | 查询已发布内容，可用 `?contentType=PROMPT` 筛选 |
| GET | `/contents/{id}` | 查询单篇已发布内容 |
| POST | `/admin/test` | 脚手架自带的 hello 接口，需登录 |
| GET | `/admin/redis/verify` | **阶段 0 验收用**：写一个带 TTL 的 key 再读回，确认 Redis 连通 |

> 计划表 19 约定的接口前缀是 `/api/*`，当前实现尚未统一加上 `/api` 前缀，也未提供 `/api/auth/logout`。
> 前端 Vite 已配置 `/api` → `http://localhost:8084` 并去掉前缀，因此浏览器侧通过 `/api/contents` 访问。

## 演示账号

三个角色账号均已写入 `docs/database.sql`，密码统一为 `123456`（计划 §16 要求至少准备普通用户、创作者、管理员三个账号）：

| 用户名 | 角色 |
|---|---|
| `admin` | ADMIN |
| `creator` | CREATOR |
| `user` | USER |

## 环境要求

计划 §10 阶段 0 要求本机具备：JDK 17、Maven、MySQL、Redis、Node.js、Git、Docker。

当前开发机实际版本：JDK 17.0.10、Maven 3.9.14、Node.js 22.19.0、npm 10.9.3、MySQL 8.0、Redis 7。

MySQL 与 Redis 由 ContentHub **自己的** `docker-compose.yml` 提供，不再复用其他项目的容器。

## 端口约定

| 服务 | 端口 | 归属 |
|---|---|---|
| 后端 API | 8084 | 宿主机进程 |
| 前端 Dev | 5175 | 宿主机进程 |
| MySQL | 3308 → 3306 | 容器 `contenthub-mysql` |
| Redis | 6380 → 6379 | 容器 `contenthub-redis` |

端口刻意避开本机其他项目（`springboot-mall` 占用 3307、`educheck` 占用 6379），互不干扰。

## 快速启动

一条命令拉起全部依赖（幂等，可反复执行）：

```powershell
.\scripts\dev-up.ps1
```

它会依次：`docker compose up -d` 启动 MySQL/Redis → 等两者就绪 → 后端源码比 jar 新时自动 `mvn package` → 启动后端 8084 → 启动前端 5175。

### 其他脚本

| 脚本 | 用途 |
|---|---|
| `scripts\dev-up.ps1` | 拉起全部服务（`-Restart` 先停再起；`-Rebuild` 强制重打包；`-NoBackend` / `-NoFrontend` 跳过） |
| `scripts\dev-down.ps1` | 停止后端与前端（默认保留容器；`-WithContainers` 一并停容器） |
| `scripts\dev-status.ps1` | 查看容器、端口、接口与自启任务状态，并附带最近日志 |
| `scripts\register-autostart.ps1` | 注册 / 卸载「登录时自启」计划任务（`-Remove` 卸载） |

> **维护脚本时注意**：这些 `.ps1` 必须保存为 **UTF-8 带 BOM**。Windows PowerShell 5.1（计划任务默认用它）会把无 BOM 的 UTF-8 文件按系统 ANSI（本机为 GBK）解码，导致中文乱码甚至语法错误。
> 另外脚本内调用 `docker` / `mvn` 等原生命令统一走 `_common.ps1` 的 `Invoke-External`：5.1 下若 `$ErrorActionPreference='Stop'`，原生命令写到 stderr 的正常进度输出会被当成终止错误抛出（`docker compose up -d` 就会触发）。

### 开机自启

已注册计划任务 `ContentHub Dev Up`（当前用户、登录后 30 秒触发、`Limited` 运行级别，不需要管理员权限），登录后自动执行 `dev-up.ps1`。

```powershell
# 立即试运行一次
Start-ScheduledTask -TaskName "ContentHub Dev Up"
# 查看状态
.\scripts\dev-status.ps1
# 卸载自启
.\scripts\register-autostart.ps1 -Remove
```

若要在 IDEA 里跑后端，先释放 8084 以免端口冲突：

```powershell
.\scripts\dev-down.ps1          # 停掉脚本起的后端与前端，容器保持运行
# 然后在 IDEA 里 Run
# 之后想恢复脚本托管：
.\scripts\dev-up.ps1
```

### 手动启动（不使用脚本时）

```powershell
# 1. 基础设施
docker compose up -d

# 2. 后端
cd .\backend
mvn -DskipTests package
java -jar .\contenthub-web\target\contenthub-web-0.0.1-SNAPSHOT.jar

# 3. 前端
cd .\frontend
npm install
npm run dev -- --host 127.0.0.1
```

后端地址 `http://127.0.0.1:8084`，API 文档 `http://127.0.0.1:8084/doc.html`，前端 `http://127.0.0.1:5175`（Vite 将 `/api/*` 代理到 `http://127.0.0.1:8084/*`）。

数据库无需手动初始化：`docs/database.sql` 已挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d/`，首次创建数据卷时自动建表并写入演示数据。要重置数据库：

```powershell
docker compose down -v      # 连同数据卷一起删除
docker compose up -d        # 重新初始化
```

### Redis 连通性验收（阶段 0）

```powershell
# 先登录拿 token，再用 token 访问验收接口
$login = Invoke-RestMethod http://127.0.0.1:8084/login -Method Post `
  -Body '{"username":"creator","password":"123456"}' -ContentType 'application/json'
Invoke-RestMethod http://127.0.0.1:8084/admin/redis/verify `
  -Headers @{ Authorization = "Bearer $($login.data.token)" } | ConvertTo-Json

# 直接在 Redis 中确认 key 存在且是明文
docker exec contenthub-redis redis-cli get "contenthub:stage0:ping"
```

预期 `matched` 为 `true`，且 `redis-cli` 能读到明文的 `pong@...` 值。

### 接口快速验证

```powershell
Invoke-RestMethod http://127.0.0.1:8084/contents | ConvertTo-Json -Depth 5
Invoke-RestMethod http://127.0.0.1:8084/contents/1 | ConvertTo-Json -Depth 5
```

## 下一步（严格按计划的阶段顺序）

1. **收尾阶段 1（Day 5-12）**：建 `content_category` 表并给 `contents` 补 `category_id`，完成分类 CRUD；补齐内容新增/编辑/删除与分页查询（需先在 `MybatisPlusConfig` 装配 `PaginationInnerInterceptor`）；替换掉遗留的 Cart / Order / Product 商城模块；打通"分类 → 内容 → 详情"并打 tag `v0.1`。
2. **阶段 2（Day 13-19）**：token 写入 Redis 并设置过期；落地 USER / CREATOR / ADMIN 角色授权；前端登录态与路由守卫；完成权限越权测试（顺带清理阶段 0 遗留的 `SecurityConfigurerAdapter` 废弃用法）。
3. **阶段 3（Day 20-29）**：创作者资料、内容状态流转（DRAFT / PENDING / PUBLISHED / REJECTED / OFFLINE）、管理员审核、免费与付费内容的访问权限判断、收藏。
4. **阶段 4（Day 30-38）**：订阅套餐、模拟支付、创建订阅并计算起止时间、访问内容时校验有效订阅、我的订阅页，打 tag `v0.2`。
5. **阶段 5（Day 39-47）**：内容详情缓存、热门内容 ZSet、浏览量 INCR 与定时同步 MySQL、关键词搜索、评论、阅读历史。
6. **阶段 6（Day 48-55）**：创作者仪表盘与内容管理、管理员用户/分类/内容/评论/套餐管理、前端按角色动态显示菜单。
7. **阶段 7（Day 56-60）**：补齐异常处理与参数校验、Docker 容器化、Docker Compose、Nginx 反向代理、部署、整理 README 与截图，打 tag `v1.0`。

## 文档

- `ContentHub_开发指导计划.docx` — 开发路线与阶段划分的唯一依据
- `docs/database.sql` — 数据库建表脚本与演示数据
