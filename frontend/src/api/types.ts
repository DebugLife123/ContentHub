/** 后端统一返回结构 */
export interface ApiResponse<T> {
  success: boolean
  message?: string | null
  errorCode?: string | null
  data: T
}

/** 后端 PageResponse 结构 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** USER / CREATOR / ADMIN */
export type Role = 'USER' | 'CREATOR' | 'ADMIN'

export interface UserInfo {
  id: number
  username: string
  nickname?: string | null
  avatar?: string | null
  email?: string | null
  bio?: string | null
  role: Role
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresInMinutes: number
  role: Role
  username: string
}

export interface Category {
  id: number
  name: string
  sort: number
  status: 'ENABLED' | 'DISABLED'
  contentCount?: number
  createTime?: string
}

/** 内容状态流转（阶段 3）：DRAFT -> PENDING -> PUBLISHED / REJECTED，PUBLISHED -> OFFLINE */
export type ContentStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED' | 'OFFLINE'

export interface ContentItem {
  id: number
  creatorId: number
  categoryId?: number | null
  categoryName?: string | null
  title: string
  summary?: string | null
  cover?: string | null
  /** ARTICLE/TUTORIAL/EBOOK/VIDEO/PDF/CODE/PROMPT/DATASET/COLUMN */
  contentType: string
  /** 完整正文；被锁时为 null */
  body?: string | null
  /** 被锁时的试读片段 */
  bodyPreview?: string | null
  /** 是否因订阅权限被锁 */
  locked?: boolean
  lockReason?: string | null
  fileUrl?: string | null
  /** FREE / SUBSCRIBED */
  accessType: string
  status: ContentStatus
  /** 审核驳回原因 */
  rejectReason?: string | null
  viewCount?: number | null
  likeCount?: number | null
  favorited?: boolean
  favoriteCount?: number
  /** 评论数（阶段 5） */
  commentCount?: number
  /** 热度分（阶段 5：Redis ZSet 分数） */
  hotScore?: number
  createTime?: string
  updateTime?: string
}

export interface ContentPayload {
  title: string
  summary?: string | null
  cover?: string | null
  contentType: string
  categoryId?: number | null
  body?: string | null
  fileUrl?: string | null
  accessType?: string
  // 刻意没有 status：新建一律是草稿，状态流转走 submit / offline / 管理员审核接口
}

export interface ContentQuery {
  pageNum?: number
  pageSize?: number
  categoryId?: number | null
  contentType?: string
  keyword?: string
  status?: ContentStatus
}

export interface CategoryPayload {
  name: string
  sort?: number
  status?: 'ENABLED' | 'DISABLED'
}

/** 创作者资料（阶段 3 Day 20） */
export interface CreatorProfile {
  id: number
  userId: number
  displayName: string
  intro?: string | null
  verified?: boolean
  subscriberCount?: number
  contentCount?: number
  publishedCount?: number
  createTime?: string
}

/** 订阅套餐（阶段 4 Day 30） */
export interface SubscriptionPlan {
  id: number
  creatorId: number
  creatorName?: string | null
  name: string
  description?: string | null
  price: number
  durationDays: number
  status: 'ACTIVE' | 'INACTIVE'
  subscriberCount?: number
  createTime?: string
}

export interface PlanPayload {
  name: string
  description?: string | null
  price: number
  durationDays: number
  status?: 'ACTIVE' | 'INACTIVE'
}

/** 订阅记录（阶段 4 Day 37） */
export interface Subscription {
  id: number
  planId: number
  planName?: string | null
  creatorId: number
  creatorName?: string | null
  startTime: string
  endTime: string
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELED'
  remainingDays: number
  valid: boolean
  createTime?: string
}

/** 评论（阶段 5 Day 45） */
export interface Comment {
  id: number
  contentId: number
  contentTitle?: string | null
  userId: number
  username?: string | null
  /** NORMAL 正常 / HIDDEN 已隐藏 */
  status: 'NORMAL' | 'HIDDEN'
  body: string
  canDelete?: boolean
  createTime?: string
}

/** 阅读历史（阶段 5 Day 46） */
export interface ReadingHistory {
  id: number
  contentId: number
  contentTitle?: string | null
  contentType?: string | null
  accessType?: string | null
  progress: number
  lastReadTime?: string
}

/** 管理端用户（阶段 6 Day 51） */
export interface AdminUser {
  id: number
  username: string
  nickname?: string | null
  email?: string | null
  role: Role
  status: 'ENABLED' | 'DISABLED'
  contentCount?: number
  createTime?: string
}

// ------------------------------------------------------------------ Skill 商城（当前为 mock 数据）

/** Skill 分类 */
export interface SkillCategory {
  id: string
  name: string
}

/** 快速上手步骤 */
export interface SkillStep {
  title: string
  detail?: string
}

/** 团队协作信息 */
export interface SkillTeam {
  maintainers: number
  contributors: number
  openIssues: number
  lastCommit: string
}

/** Skill 评论 */
export interface SkillComment {
  id: number
  user: string
  time: string
  body: string
}

/** Skill 列表项 */
export interface SkillItem {
  id: string
  name: string
  icon: string
  categoryId: string
  summary: string
  author: string
  repo: string
  stars: number
  version: string
  /** FREE 免费 / MEMBER 会员解锁 */
  accessType: 'FREE' | 'MEMBER'
  platforms: string[]
  tags: string[]
  updatedAt: string
}

/** Skill 详情（比列表项多出详情页需要的字段） */
export interface SkillDetail extends SkillItem {
  license: string
  size: string
  downloads: number
  /** 1-5 级，5 最好 */
  securityLevel: number
  securityLabel: string
  submitter: string
  submitTime: string
  officialUrl: string
  installCommand: string
  /** 功能特点 */
  features: string[]
  /** 为什么收录 */
  whyIncluded: string
  quickStart: SkillStep[]
  team: SkillTeam
  comments: SkillComment[]
  /** 是否因会员权限被锁；由调用方按当前会员状态计算 */
  locked?: boolean
  lockReason?: string | null
}

export interface SkillQuery {
  categoryId?: string
  keyword?: string
  sort?: 'stars' | 'updated' | 'name'
}

/** 创作者仪表盘统计（阶段 6 Day 48） */
export interface CreatorDashboard {
  contentCount: number
  publishedCount: number
  draftCount: number
  pendingCount: number
  rejectedCount: number
  offlineCount: number
  totalViews: number
  totalFavorites: number
  totalComments: number
  subscriberCount: number
  planCount: number
}
