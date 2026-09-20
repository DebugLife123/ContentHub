# ContentHub · 数字内容订阅与创作者平台

> 基于 Spring Boot 2.6 + Vue 3 + Vite 4 的全栈开源脚手架。

## 核心业务

\\\
创作者 → 发布数字内容 → 创建订阅套餐 → 用户订阅 → 获得内容访问权限
                                                       ↓ 收藏 / 评论 / 阅读
                                                  创作者查看数据
\\\

支持内容形态：技术文章 / 系列教程 / 电子书 / 视频课程 / PDF / 代码模板 / Prompt / 数据集 / 专栏

## 技术栈

**后端**：Spring Boot 2.6 · MyBatis-Plus · Spring Security + JWT · MySQL 8 · Knife4j(API文档) · Maven 多模块
**前端**：Vue 3 · Vite 4 · Vue Router 4 · Pinia · Axios · Element Plus · Tailwind CSS

## 目录结构

\\\
ContentHub/
├── backend/                 # Spring Boot 多模块后端
│   ├── contenthub-common/   # 公共模块：DO/Mapper/枚举/异常/工具类
│   ├── contenthub-jwt/      # JWT 认证模块：登录过滤器/Token
│   ├── contenthub-admin/    # 管理端配置：Security 配置
│   └── contenthub-web/      # Web 启动模块：Controller/Service + 配置文件
├── frontend/                # Vue3 + Vite 前端
└── docs/                    # 项目文档
    └── database.sql         # 数据库建表脚本
\\\

## 端口约定

| 服务 | 端口 |
|---|---|
| 后端 API | 8084 |
| 前端 Dev | 5175 |
| MySQL (Docker) | 3307 → 3306 |

## 当前状态\n\n第一阶段底座已完成：后端已复制并改造成 `com.contenthub` 多模块工程，数据库已创建 ContentHub 核心表，前端已改造成 ContentHub 内容订阅平台首页、登录页、内容详情页和创作者工作台。\n\n演示账号：`creator / 123456`。\n\n详细搭建步骤见 [docs/SETUP.md](docs/SETUP.md)。\n\n## 快速启动

### 1. 初始化数据库
\\\ash
# 复用本机 Docker MySQL
docker exec -i springboot-mall-mysql mysql -uroot -p123456 < docs/database.sql
\\\

### 2. 启动后端
\\\ash
cd backend
mvn -DskipTests package
java -jar contenthub-web/target/contenthub-web-0.0.1-SNAPSHOT.jar
\\\

### 3. 启动前端
\\\ash
cd frontend
npm install
npm run dev
\\\

访问 [http://127.0.0.1:5175](http://127.0.0.1:5175) ，API 文档 [http://127.0.0.1:8084/doc.html](http://127.0.0.1:8084/doc.html)
## 当前开发进度（2026-09-20）

已完成第一条可运行业务链路：

- MySQL `contents` 内容表已接入 MyBatis-Plus；
- 后端提供 `GET /contents` 内容列表接口；
- 后端提供 `GET /contents/{id}` 内容详情接口；
- 首页内容卡片已从后端读取已发布内容；
- 内容详情页已从后端读取正文、访问类型和统计数据；
- 数据库脚本包含 3 条演示内容。

接口快速验证：

```powershell
Invoke-RestMethod http://127.0.0.1:8084/contents | ConvertTo-Json -Depth 5
Invoke-RestMethod http://127.0.0.1:8084/contents/1 | ConvertTo-Json -Depth 5
```

下一步建议按这个顺序推进：

1. 创作者内容发布/编辑/下架；
2. 订阅套餐和订阅权限；
3. 收藏、评论、阅读量统计；
4. 创作者数据看板从静态数据切换为真实接口；
5. Redis 接入浏览量与热门内容缓存。
