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
  /** 编辑时只允许 DRAFT / OFFLINE；发布必须走审核 */
  status?: 'DRAFT' | 'OFFLINE'
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
