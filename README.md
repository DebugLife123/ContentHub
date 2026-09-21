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
│   └── src/
│       ├── api/                 # 接口封装与共享类型（auth / content / category / types）
│       ├── stores/user.ts       # Pinia 登录态
│       ├── router/index.ts      # 路由与守卫（requiresAuth / roles）
│       └── views/               # 首页 / 内容库 / 详情 / 登录 / 注册 / 个人中心
│           ├── creator/         # 创作者工作台、发布与编辑
│           └── admin/           # 分类管理
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
| 阶段 1 | Day 5-12 | Vue3 基础 + 用户/分类/内容基础 CRUD | ✅ 已完成（tag `v0.1`） |
| 阶段 2 | Day 13-19 | Spring Security + JWT + Redis 登录权限 | ✅ 已完成 |
| 阶段 3 | Day 20-29 | 内容中心：发布、详情、审核、权限访问 | ⬜ 未开始 |
| 阶段 4 | Day 30-38 | 套餐、订阅、模拟支付、订阅到期 | ⬜ 未开始 |
| 阶段 5 | Day 39-47 | Redis 缓存、热门、搜索、收藏、评论、历史 | ⬜ 未开始 |
| 阶段 6 | Day 48-55 | 创作者中心 + 管理后台 | ⬜ 未开始 |
| 阶段 7 | Day 56-60 | 联调、异常处理、Docker、Nginx、部署 | ⬜ 未开始 |
| 可选升级 | Day 61-70 | Spring AI / RAG / AI 内容助手 | ⬜ 未开始 |

**结论：阶段 0 / 1 / 2 均已完成；下一步进入阶段 3（内容中心：状态流转、审核、按订阅判断访问权限）。**

### 阶段 1 与阶段 2 已完成

计划这两阶段的验收标准与落实方式：

| 阶段 | 计划验收标准 | 落实 |
|---|---|---|
| 1 | 可以新增分类 | `POST /api/categories`（仅管理员），含重名与「分类下有内容不可删」校验 |
| 1 | 可以新增/编辑/删除/分页查询内容 | `POST` / `PUT` / `DELETE /api/contents`，`GET /api/contents/page` 真分页（`PaginationInnerInterceptor`） |
| 1 | 前端能看到内容列表和详情 | 首页、内容库（筛选+分页）、内容详情、创作者工作台、发布/编辑页、分类管理页 |
| 1 | 分类 → 内容 → 详情跑通 | 已端到端验证 |
| 2 | 未登录不能访问个人中心 | `GET /api/users/me` 需登录，实测 401 |
| 2 | 普通用户不能访问创作者后台 | `GET /api/contents/mine` 需 CREATOR/ADMIN，实测 USER 403 |
| 2 | 管理员可以进入管理页面 | `GET /api/categories/all` 需 ADMIN，实测 CREATOR 403 / ADMIN 200 |
| 2 | Redis 可查到 token 并能测试过期 | `login:token:{token}`，值=用户名，TTL 实测 86400 秒（1440 分钟） |

主要实现要点：

- **Redis 存 token（Day 16）**：登录成功写入 `login:token:{token}`（计划表 8 的 Key 命名），TTL 与 JWT 过期时间共用 `jwt.tokenExpireTime`；`TokenAuthenticationFilter` 在验签之后**再查一次 Redis**，因此退出登录能让 token 立即失效——纯 JWT 是签发即不可撤回的。
- **真实角色（Day 17）**：新增 `LoginUser implements UserDetails`，携带 `userId` 与 `role`。改造前 `UserDetailServiceImpl` 把 authorities 写死成 `ADMIN`，等于任何登录用户都是管理员；现在从 `users.role` 读取并映射为 `ROLE_*`。
- **角色授权**：`WebSecurityConfig` 按「读公开、写按角色」编排，先匹配先生效。`RestAccessDeniedHandler` 原先只打日志、不写响应体（注释里写着「预留，后面引入多角色时会用到」），现在返回 403 + 业务错误码，前端才能区分「没登录」与「登录了但权限不够」。
- **移除废弃 API**：`JwtAuthenticationSecurityConfig` 不再继承 `SecurityConfigurerAdapter` / 使用 `http.apply()`，改为显式暴露 `DaoAuthenticationProvider` 与 `JwtAuthenticationFilter` Bean，编译告警消失。
- **统一 API 前缀**：`server.servlet.context-path: /api`，与计划表 19 的接口约定一致。**因此 API 文档地址变为 `http://127.0.0.1:8084/api/doc.html`。**
- **VO/DTO 分层**：接口不再直接返回 `ContentDO`（原先把 `is_deleted`、正文全文都暴露给了列表页）。新增 `ContentListVO` / `ContentDetailVO` / `CategoryVO` / `UserInfoVO` 与带 `@NotBlank`/`@Size` 校验的 Req 对象。
- **逻辑删除**：`is_deleted` 字段加上 `@TableLogic`，`deleteById` 自动变为 `UPDATE`，查询自动过滤。
- **归属校验**：计划里「创作者只能改自己的内容」属于阶段 3 Day 22，但 Security 只按角色放行，不做归属校验就等于任何创作者都能改别人的内容，故提前落地（非 ADMIN 且非作者本人返回 `NOT_CONTENT_OWNER`）。
- **前端登录态与守卫（Day 18）**：`stores/user.ts` 用 Pinia 管 token 与用户信息，`router/index.ts` 用 `meta.requiresAuth` / `meta.roles` 做守卫。守卫只是体验层——前端可被绕过，真正的权限仍在后端。
- **清理**：删除遗留商城的 `Cart`/`Order`/`Product` 全部后端模块（DO/Mapper/Service/Controller）与前端 `pages/**`、遗留组件、`api/frontend/*`；`OrderProductVO` 被 MyBatis-Plus 误扫描的启动警告随之消失。

### 阶段 0 遗留说明（已在本轮处理）

- ~~`JwtAuthenticationSecurityConfig` 仍使用已废弃的 `SecurityConfigurerAdapter`~~ → 阶段 2 已改为显式 Bean 装配；
- ~~旧商城模块未删除~~ → 阶段 1 已清理；
- `GET /api/admin/redis/verify` 是阶段 0 的验收用临时接口，仍保留（受 ADMIN 角色保护），阶段 5 落地真实缓存业务后可删除。

### 阶段 0 完成的技术栈对齐（保留备查）

- **Spring Boot 2.6.3 → 3.2.5**，`java.version` 8 → 17；
- **springfox → springdoc-openapi 2.3.0**（Knife4j 换用 `knife4j-openapi3-jakarta-spring-boot-starter` 4.5.0），原 `Docket`/`@EnableSwagger2WebMvc` 重写为 `OpenAPI` + `GroupedOpenApi`；
- **MyBatis-Plus** `mybatis-plus-boot-starter` 3.5.2 → `mybatis-plus-spring-boot3-starter` 3.5.5；
- **MySQL 驱动** `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`；
- **`javax.*` → `jakarta.*`**：24 处（servlet 16 处、validation 3 处等），`JwtTokenHelper` 中 JDK 自带的 `javax.security.auth.login` 保持不变；
- **Spring Security 6**：移除已删除的 `WebSecurityConfigurerAdapter`，改为 `SecurityFilterChain` Bean + Lambda DSL，`mvcMatchers` → `requestMatchers`；
- **Redis 接入**：`spring.data.redis` 配置（Boot 3 前缀）、`RedisConfig`（key 用 String、value 用 JSON，便于 `redis-cli` 观察）、补充 `StringRedisTemplate`；
- **前端 TypeScript**：`typescript` 5.9 + `vue-tsc`，`tsconfig.json`（`strict`，`allowJs` 渐进迁移），`vite.config.ts`，`npm run build` 已串入类型检查。

### 阶段 3 及以后

阶段 3-7 与可选 AI 阶段均未开始。

已建表但**尚无对应代码**的有：`creator_profiles`、`subscription_plans`、`subscriptions`、`favorites`、`comments`；计划表 6 中标记为「建议」的 `reading_history` 尚未创建。

内容状态目前只支持 `DRAFT` / `PUBLISHED` / `OFFLINE`，`PENDING` / `REJECTED` 的审核流转属于阶段 3（接口层已显式拒绝这两个值，避免出现没有审核流程却能把内容置为待审核的中间态）。

## 已实现接口

所有接口统一以 `/api` 为前缀（`server.servlet.context-path`），与计划表 19 的约定一致。

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/auth/register` | 公开 | 注册普通用户 |
| POST | `/api/auth/login` | 公开 | 登录，返回 JWT；token 同时写入 Redis |
| POST | `/api/auth/logout` | 登录 | 退出登录，从 Redis 删除 token 使其立即失效 |
| GET | `/api/users/me` | 登录 | 当前登录用户信息 |
| GET | `/api/categories` | 公开 | 分类列表（仅启用中） |
| GET | `/api/categories/all` | ADMIN | 全部分类（含禁用，供管理页） |
| POST | `/api/categories` | ADMIN | 新增分类 |
| PUT | `/api/categories/{id}` | ADMIN | 修改分类 |
| DELETE | `/api/categories/{id}` | ADMIN | 删除分类（逻辑删除；分类下有内容时拒绝） |
| GET | `/api/contents/page` | 公开 | 内容分页查询，支持 `categoryId` / `contentType` / `keyword` / `pageNum` / `pageSize` |
| GET | `/api/contents/{id}` | 公开 | 内容详情（仅已发布，含正文） |
| GET | `/api/contents/mine` | CREATOR / ADMIN | 我的内容（含草稿与下架），支持 `status` 筛选 |
| GET | `/api/contents/mine/{id}` | CREATOR / ADMIN | 我的内容详情（不限状态，编辑草稿时回显） |
| POST | `/api/contents` | CREATOR / ADMIN | 新增内容 |
| PUT | `/api/contents/{id}` | CREATOR / ADMIN（仅本人） | 编辑内容 |
| DELETE | `/api/contents/{id}` | CREATOR / ADMIN（仅本人） | 删除内容（逻辑删除） |
| POST | `/api/admin/test` | ADMIN | 脚手架自带的 hello 接口 |
| GET | `/api/admin/redis/verify` | ADMIN | 阶段 0 验收用：写一个带 TTL 的 key 再读回 |

> **`/api/contents/mine` 的匹配顺序**：Security 里「需要登录」的规则必须写在「公开 GET」规则之前，否则会被后者覆盖。
> 同理，Spring MVC 优先匹配字面量路径，因此 `/contents/mine` 不会被 `/contents/{id}` 当成 `id="mine"`。

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

后端地址 `http://127.0.0.1:8084`，API 文档 `http://127.0.0.1:8084/api/doc.html`，前端 `http://127.0.0.1:5175`（Vite 将 `/api/*` 原样转发到 `http://127.0.0.1:8084/api/*`）。

数据库无需手动初始化：`docs/database.sql` 已挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d/`，首次创建数据卷时自动建表并写入演示数据。要重置数据库：

```powershell
docker compose down -v      # 连同数据卷一起删除
docker compose up -d        # 重新初始化
```

### Redis 连通性验收（阶段 0）

```powershell
# 先登录拿 token，再用 token 访问验收接口（该接口需要 ADMIN 角色）
$login = Invoke-RestMethod http://127.0.0.1:8084/api/auth/login -Method Post `
  -Body '{"username":"admin","password":"123456"}' -ContentType 'application/json'
Invoke-RestMethod http://127.0.0.1:8084/api/admin/redis/verify `
  -Headers @{ Authorization = "Bearer $($login.data.token)" } | ConvertTo-Json

# 直接在 Redis 中确认 key 存在且是明文
docker exec contenthub-redis redis-cli get "contenthub:stage0:ping"
```

预期 `matched` 为 `true`，且 `redis-cli` 能读到明文的 `pong@...` 值。

### 登录态验收（阶段 2）

```powershell
$login = Invoke-RestMethod http://127.0.0.1:8084/api/auth/login -Method Post `
  -Body '{"username":"creator","password":"123456"}' -ContentType 'application/json'
$token = $login.data.token

# 1) token 已写入 Redis，TTL 与 JWT 一致（1440 分钟 = 86400 秒）
docker exec contenthub-redis redis-cli get "login:token:$token"
docker exec contenthub-redis redis-cli ttl "login:token:$token"

# 2) 退出登录后同一个 token 立即失效（纯 JWT 做不到）
Invoke-RestMethod http://127.0.0.1:8084/api/auth/logout -Method Post -Headers @{ Authorization = "Bearer $token" }
docker exec contenthub-redis redis-cli exists "login:token:$token"   # 期望 0
```

### 接口快速验证

```powershell
Invoke-RestMethod http://127.0.0.1:8084/api/contents/page | ConvertTo-Json -Depth 5
Invoke-RestMethod http://127.0.0.1:8084/api/contents/1 | ConvertTo-Json -Depth 5
Invoke-RestMethod http://127.0.0.1:8084/api/categories | ConvertTo-Json -Depth 5
```

> **Windows PowerShell 5.1 提示**：`Invoke-RestMethod` 在响应未声明 `charset` 时会按 ISO-8859-1 解码，中文会显示成乱码；URL 里的中文也不会自动编码。
> 上面的命令看结构没问题，但若要**断言中文内容**，请显式解码并编码参数：
> ```powershell
> $resp = Invoke-WebRequest 'http://127.0.0.1:8084/api/contents/1' -UseBasicParsing
> ([System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json).data.title
> $kw = [uri]::EscapeDataString('前端')
> Invoke-RestMethod "http://127.0.0.1:8084/api/contents/page?keyword=$kw"
> ```

## 下一步（严格按计划的阶段顺序）

1. **阶段 3（Day 20-29）**：创作者资料（`creator_profiles` 目前只有表）、状态流转补齐 `PENDING` / `REJECTED`、管理员审核接口与审核页、按 `access_type` 与有效订阅判断内容访问权限（无权限时只返回预览）、收藏。
2. **阶段 4（Day 30-38）**：订阅套餐、模拟支付、创建订阅并计算起止时间、访问内容时校验有效订阅、我的订阅页，打 tag `v0.2`。
3. **阶段 5（Day 39-47）**：内容详情缓存、热门内容 ZSet、浏览量 INCR 与定时同步 MySQL、评论、阅读历史（`reading_history` 表待建）。
4. **阶段 6（Day 48-55）**：创作者仪表盘增强、管理员用户/内容/评论/套餐管理、前端按角色动态显示菜单（分类管理页已完成）。
5. **阶段 7（Day 56-60）**：补齐异常处理与参数校验、Docker 构建前后端、扩展 `docker-compose.yml` 加入 backend + nginx、Nginx 反向代理、部署、整理截图，打 tag `v1.0`。

## 文档

- `ContentHub_开发指导计划.docx` — 开发路线与阶段划分的唯一依据
- `docs/database.sql` — 数据库建表脚本与演示数据
