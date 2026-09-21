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

/** 内容状态；PENDING / REJECTED 属于阶段 3 的审核流转 */
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
  body?: string | null
  fileUrl?: string | null
  /** FREE / SUBSCRIBED */
  accessType: string
  status: ContentStatus
  viewCount?: number | null
  likeCount?: number | null
  createTime?: string
  updateTime?: string
}

/** 新增 / 编辑内容的提交体 */
export interface ContentPayload {
  title: string
  summary?: string | null
  cover?: string | null
  contentType: string
  categoryId?: number | null
  body?: string | null
  fileUrl?: string | null
  accessType?: string
  status?: ContentStatus
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
