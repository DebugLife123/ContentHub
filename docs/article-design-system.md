# ContentHub 技术文章详情页 · 设计规范（Design System）

> 版本 v1.0 ｜ 适用范围：技术博客 / 编程教程 / 架构文章 / AI 技术文章详情页
> 实现位置：`frontend/src/components/article/` + `frontend/src/styles/article-system.scss`

---

## Step 1 · 设计目标与原则

### 目标
建立一套适合长文技术阅读的、统一可复用的文章内容排版系统，让页面第一眼给人的感觉是
“这是一个专业的技术内容平台”，而不是“模板网站”。

### 关键词
专业 · 极简 · 信息密度高 · 阅读舒适 · 层次清晰 · 技术感 · 内容优先 · 长文友好 · 视觉统一 · 可扩展

### 风格定位
“现代开发者内容平台 + 高级杂志排版 + 技术文档”。
延续 ContentHub 既有品牌基因（暖纸底色、墨色文字、DM Mono 微标签、编辑排版感），
在其上建立一套**文章阅读专用**的排版层，而不是另起炉灶。

### 核心原则（红线）
1. 页面核心永远是“阅读内容”，任何装饰不得抢夺正文的视觉优先级。
2. 丰富感只能来自：Typography / 内容结构 / 间距 / 代码块 / 图片 / 提示框 / 表格 / 引用 / TOC / 元信息 / 相关内容。
3. 禁止：花哨渐变、堆砌卡片、厚重阴影、无意义动画、营销落地页与电商风格。
4. 所有内容组件共享同一套 token（颜色 / 字号 / 间距 / 圆角 / 线宽），不允许“一处一样”。

---

## Step 2 · 页面信息架构（IA）

```
┌─────────────────────────── AppHeader（全站已有） ───────────────────────────┐
│                                                                            │
│  ← 返回内容库                               （Breadcrumb / Back）           │
│                                                                            │
│  ┌──────────┬────────────────────────────────┬───────────────┐             │
│  │          │  ARTICLE HEADER                │               │             │
│  │          │  · 分类 · 内容类型 · 阅读状态    │               │             │
│  │          │  · H1 标题                      │               │             │
│  │          │  · 摘要（Lede）                 │               │             │
│  │          │  · Meta 行（作者/日期/时长/数据）│               │             │
│  │  TOC     ├────────────────────────────────┤  SIDEBAR      │             │
│  │  目录     │  ARTICLE BODY（结构化正文）      │  · 作者卡      │             │
│  │  sticky  │   01 章节编号 + H2              │  · 文章数据    │             │
│  │  scroll- │   段落 / 代码 / 引用 / 图片 …     │  · 标签        │             │
│  │  spy     │   …                             │  · 相关文章    │             │
│  │          │   总结                          │  · 订阅 CTA   │             │
│  │          ├────────────────────────────────┤  sticky       │             │
│  │          │  ARTICLE FOOTER                 │               │             │
│  │          │  · 本文完 · 标签 · 上一篇/下一篇  │               │             │
│  │          ├────────────────────────────────┤               │             │
│  │          │  COMMENTS 评论区                 │               │             │
│  └──────────┴────────────────────────────────┴───────────────┘             │
│                                                                            │
│  ┌─────────────────────── Site Footer（全站已有） ──────────────────────┐   │
└────────────────────────────────────────────────────────────────────────────┘
```

### 布局栅格（桌面端）
| 区域 | 宽度 | 说明 |
|---|---|---|
| 页面容器 | max 1320px，居中 | 与全站 topbar 对齐 |
| 左栏 TOC | 224px | `position: sticky`，滚动高亮 |
| 中央正文 | 720–760px（弹性上限） | 视觉中心，永不超宽 |
| 右栏 Sidebar | 280px | `position: sticky` |
| 栏间距 | 56–72px | 用留白分栏，不用卡片边线 |

### 阅读节奏（Vertical Rhythm）
标题 → 摘要 → Meta → 正文 → 章节编号+H2 → 段落 → 代码 → 解释 → 图片 → 提示框 → 代码 → 总结。
长短交替，靠间距和组件形态自然产生节奏。

---

## Step 3 · Typography System

### 字体族
| 角色 | 字体 | 用途 |
|---|---|---|
| UI / 标题 | `Manrope, 'Microsoft YaHei', sans-serif` | 标题、按钮、导航 |
| 正文阅读 | `'Noto Serif SC', 'Songti SC', serif` | 文章段落、引用（衬线利于长文） |
| 等宽 | `'DM Mono', 'JetBrains Mono', Consolas, monospace` | 代码、Meta 微标签、章节编号 |

### 字级（Type Scale）
| 层级 | 字号 | 字重 | 行高 | 字距 | 间距（上/下） |
|---|---|---|---|---|---|
| H1（文题） | clamp(40px, 4.4vw, 56px) | 800 | 1.18 | -0.03em | 0 / 28px |
| Lede 摘要 | 19px | 400 | 1.75 | 0 | 0 / 36px |
| H2（章节） | 32px | 750 | 1.3 | -0.02em | 64px / 24px |
| H2 章节编号 | 13px DM Mono | 500 | 1 | +0.14em | 与 H2 绑定 |
| H3 | 24px | 700 | 1.4 | -0.015em | 44px / 16px |
| H4 | 19px | 700 | 1.45 | -0.01em | 32px / 12px |
| 正文 | 17px | 400 | 1.9 | 0 | 0 / 24px |
| 正文-小字 | 15px | 400 | 1.8 | 0 | 用于 caption / 辅助 |
| Meta 微标签 | 12–13px DM Mono | 500 | 1.6 | +0.04em | — |
| 代码 | 13.5px DM Mono | 400 | 1.75 | 0 | — |
| 引用 | 19px 衬线 | 500 | 1.8 | 0 | — |

### 行内元素
- **粗体**：`font-weight: 700`，颜色继承。
- *斜体*：仅用于英文术语，中文不使用斜体。
- `行内代码`：13.5px 等宽，底色 `--art-code-inline-bg`，左右 5px 内边距，圆角 4px，字色 `--art-accent-deep`。
- [链接]：墨色 + 1px 下划线（accent 色 40% 透明度），hover 时下划线变实心 accent；外链带 `↗`。

---

## Step 4 · 颜色系统

> 复用全站 token，新增文章域 token（`--art-*`），全部集中在 `article-system.scss` 的 `:root`。

| Token | 值 | 用途 |
|---|---|---|
| `--ink` | `#171717` | 标题、正文强色（继承全站） |
| `--paper` | `#f5f2ec` | 页面底色（继承全站） |
| `--line` | `#d8d3ca` | 分隔线（继承全站） |
| `--muted` | `#7d7a73` | 次级文字（继承全站） |
| `--orange` | `#f17d47` | 品牌点缀（继承全站，克制使用） |
| `--art-text` | `#26241f` | 正文颜色（比标题略柔） |
| `--art-accent` | `#c14e1d` | 正文域强调色（链接、active TOC） |
| `--art-accent-deep` | `#a03d12` | 行内代码文字 |
| `--art-surface` | `#edeae2` | 轻底色（表格斑马纹、引用底） |
| `--art-line-strong` | `#c4beb1` | 表格头线、章节顶线 |
| `--art-code-bg` | `#201d19` | 代码块深底（暖黑，非纯黑） |
| `--art-code-head` | `#2a2721` | 代码块工具栏底 |
| `--art-code-line` | `#3a352c` | 代码块内线 |
| `--art-code-text` | `#e8e2d5` | 代码文字 |
| `--art-code-muted` | `#8d8574` | 行号 / 注释 |
| Callout tip | 绿 `#3e6b34` / 底 `#eef0e4` | 最佳实践 |
| Callout info | 蓝灰 `#3a5a78` / 底 `#eaeef0` | 说明 |
| Callout warning |  amber `#96650f` / 底 `#f6efdd` | 注意 |
| Callout danger | 红 `#a33b2a` / 底 `#f4e6e1` | 严重风险 |

**配色纪律**：强调色全局只有橙系一支；callout 的彩色只出现在 3px 左边线和图标上，大面积底色保持低饱和暖灰。

---

## Step 5 · 间距系统

采用 4px 基网，文章域使用以下语义化间距 token：

| Token | 值 | 用途 |
|---|---|---|
| `--art-gap-xs` | 8px | 图标与文字、Meta 项内 |
| `--art-gap-sm` | 16px | 紧凑组件内距、列表项间 |
| `--art-gap-md` | 24px | 段落间距、组件内边距 |
| `--art-gap-lg` | 36px | 块级组件上下间距（代码/图/表/引用/callout） |
| `--art-gap-xl` | 48px | H3 之上、正文与 Footer 之间 |
| `--art-gap-2xl` | 64px | H2 之上、章节分隔 |
| `--art-gap-3xl` | 88px | Header 与正文之间、评论区之上 |

规则：
- 同层级组件之间只允许出现 `--art-gap-lg` 一档，避免间距抖动。
- H2 上方间距（64px）必须大于任何块级组件间距（36px），保证章节切分感。
- 不使用阴影堆叠层次，层次由**线 + 留白 + 字级**完成。

圆角纪律：正文组件仅允许 4px（代码块/图片/表格 6px），禁止大圆角卡片。

---

## Step 6 · 代码块规范

代码块是技术博客最重要的组件，对标 GitHub / VS Code：

```
┌──────────────────────────────────────────────────┐
│ ▤ UserService.java          JAVA        ⧉ 复制    │  ← 工具栏 36px
├──────────────────────────────────────────────────┤
│  1  @Service                                     │
│  2  public class UserService {                   │
│  3      private final UserRepository repo;       │
│  4  }                                            │
└──────────────────────────────────────────────────┘
```

- 深暖黑底 `--art-code-bg`，等宽字体，13.5px / 1.75。
- 顶部工具栏：左侧文件名（无文件名时显示语言），右侧语言徽标 + 复制按钮（hover 显现，复制后变“已复制”）。
- 行号列：右对齐、宽度自适应、不可选中、`--art-code-muted`。
- 长行横向滚动，绝不撑破版心；`tab-size: 2`。
- 圆角 6px，无外阴影；上下间距 `--art-gap-lg`。
- 行内代码与代码块共用等宽字体与暖黑基因，但用浅底区分场景。

---

## Step 7 · 内容组件规范

> 所有组件上下间距统一 `--art-gap-lg`（36px），正文宽度内对齐。

### 7.1 引用 Quote
左侧 3px accent 竖线 + 极轻暖底，衬线 19px；不做成大卡片。
```
┃ Controller 只负责参数校验与响应组装，
┃ 业务规则全部放在 Service。
```

### 7.2 Callout（提示框家族）
统一结构：`3px 左边线 + 图标 + 小标题（DM Mono 12px 大写）+ 正文 15px`。
| 变体 | 图标 | 场景 |
|---|---|---|
| TIP | 💡 | 最佳实践、技巧 |
| INFO | ℹ | 补充说明 |
| WARNING | ⚠ | 注意事项、性能风险 |
| DANGER | ⚠ | 安全风险、错误示范 |
| SUCCESS | ✓ | 正确示范、验证结果 |

### 7.3 图片 Image
- 最大宽度 100%，圆角 6px，点击放大（dialog）。
- 图片说明：`图 01 ｜ Spring Boot 分层架构示意图`，13px muted，居中，自动编号。
- 图片与正文上下各留 36px。

### 7.4 表格 Table
- 表头：13px DM Mono 大写感 + 底部 2px 强线。
- 单元格：14px / 1.7，纵向 padding 12px。
- 偶数行 `--art-surface` 斑马纹；无外框粗线，仅横线。
- 容器横向滚动，代码内容用等宽字体。

### 7.5 列表
- 无序：自绘 `–` 符号（accent 色）；有序：DM Mono 编号。
- 任务列表：方框 ☐/☑，已完成文字 muted。
- 列表项行高 1.8，项间距 8px；嵌套缩进 24px。

### 7.6 其余组件
| 组件 | 规范 |
|---|---|
| Divider | `· · ·` 三点式或 1px 线 + 中央留白，上下 48px |
| Video | 16:9 比例容器，圆角 6px，附 caption |
| File Download | 一行式：📄 文件名 · 大小 ｜ 下载↗，底线分隔，不用大卡片 |
| Mermaid | 结构支持；前端以代码块形式降级展示（预留渲染接入点） |
| Math Formula | 居中、衬线斜体 18px，独立成行，上下 36px |
| GitHub Repo Card | 单行信息条：▣ owner/repo · 描述 · ★ stars，底线分隔 |
| API Request / Response | 代码块变体：工具栏左侧 METHOD + 路径徽标（GET 绿 / POST 橙 / DELETE 红） |

---

## Step 8 · Vue3 组件架构

```
views/ContentDetail.vue                 ← 路由入口：数据加载、锁定逻辑、进度上报
components/article/
├── ArticleLayout.vue                   ← 三栏栅格 + 响应式（slot: toc / default / aside）
├── ArticleHeader.vue                   ← 返回、eyebrow、H1、摘要、Meta 行
├── ArticleToc.vue                      ← 目录 + IntersectionObserver scroll-spy
├── ArticleBody.vue                     ← 结构化正文渲染器（按 block.type 分发）
├── InlineText.vue                      ← 行内渲染：bold / code / link / text
├── blocks/
│   ├── BlockCode.vue                   ← 代码块（工具栏/行号/复制/横滚）
│   ├── BlockQuote.vue                  ← 引用
│   ├── BlockCallout.vue                ← TIP/INFO/WARNING/DANGER/SUCCESS
│   ├── BlockImage.vue                  ← 图片 + caption 编号 + 点击放大
│   ├── BlockTable.vue                  ← 表格（斑马纹/横滚）
│   ├── BlockList.vue                   ← 有序/无序/任务列表
│   ├── BlockVideo.vue                  ← 视频
│   ├── BlockFile.vue                   ← 附件下载行
│   ├── BlockRepo.vue                   ← GitHub 仓库卡
│   ├── BlockApi.vue                    ← API 请求/响应示例
│   ├── BlockFormula.vue                ← 数学公式
│   └── BlockDivider.vue                ← 分隔
├── ArticleSidebar.vue                  ← 作者卡 / 数据 / 标签 / 相关文章 / 订阅 CTA
├── ArticleFooter.vue                   ← 本文完 / 标签 / 上一篇·下一篇
└── ArticleComments.vue                 ← 评论区（输入 + 列表 + 分页）
styles/article-system.scss              ← 全部 token + 各组件样式
types/article.ts                        ← 结构化正文类型定义
utils/articleParser.ts                  ← 纯文本/Markdown-lite → 结构化 blocks
```

---

## Step 9 · Article 数据结构

```ts
/** 文章（视图模型：由 ContentItem + 解析结果组装，不改动后端 API） */
interface Article {
  id: number
  title: string
  summary: string
  category: string
  contentType: string
  accessType: 'FREE' | 'SUBSCRIBED'
  author: { name: string; avatar?: string; bio?: string }
  tags: string[]
  publishTime: string
  updateTime?: string
  readTime: number          // 分钟，按字数估算
  viewCount: number
  likeCount: number
  collectCount: number
  blocks: ArticleBlock[]    // 结构化正文
  toc: TocItem[]            // 由 blocks 中 h2/h3 提取
}

type ArticleBlock =
  | { type: 'heading'; level: 2 | 3 | 4; text: string; id: string }
  | { type: 'paragraph'; segments: InlineSegment[] }
  | { type: 'quote'; segments: InlineSegment[] }
  | { type: 'code'; language: string; filename?: string; code: string }
  | { type: 'image'; url: string; alt?: string; caption?: string }
  | { type: 'list'; ordered: boolean; items: ListItem[] }
  | { type: 'table'; header: string[]; rows: string[][] }
  | { type: 'callout'; variant: 'tip'|'info'|'warning'|'danger'|'success'; title?: string; segments: InlineSegment[] }
  | { type: 'divider' }
  | { type: 'video'; url: string; caption?: string }
  | { type: 'file'; name: string; url: string }
  | { type: 'repo'; repo: string; description?: string }
  | { type: 'api'; method: string; path: string; code: string; language: string }
  | { type: 'formula'; formula: string }
  | { type: 'mermaid'; code: string }

type InlineSegment =
  | { kind: 'text'; text: string }
  | { kind: 'bold'; text: string }
  | { kind: 'code'; text: string }
  | { kind: 'link'; text: string; href: string }
```

### 正文书写语法（Markdown-lite，向后兼容纯文本）
创作者后台 `body` 是纯文本字段，**不修改后端**。解析器支持下列语法，
旧数据（纯段落文本）自动降级为 paragraph 块，零迁移成本：

```
## 章节标题          → heading
### 小节             → heading
> 引用文字           → quote
```java UserService.java … ``` → code
:::warning 标题      → callout（::: 结束）
- 列表项 / 1. 列表项 / - [ ] 任务
| 列A | 列B |        → table（首行表头）
![说明](url)         → image
---                  → divider
@video: url          → video
@file: 名称 | url    → file
@repo: owner/name | 描述 → repo
@api: GET /api/users → api（下一代码块为其示例）
$$E=mc^2$$           → formula
```mermaid … ```     → mermaid
**粗体**  `代码`  [文字](url) → 行内元素
```
```

---

## 附 · 响应式断点

| 断点 | 行为 |
|---|---|
| ≥1280px | 三栏完整布局 |
| 1024–1279px | TOC 收窄 200px，正文弹性收缩 |
| 768–1023px | 隐藏左 TOC（入口变为正文顶部可折叠目录），Sidebar 移至正文之后 |
| ≤767px | 单栏；隐藏 TOC 与 Sidebar 次要信息；H1 34px；代码块工具栏保留复制 |
| 390px | Meta 行换行为两行；评论区全宽 |
