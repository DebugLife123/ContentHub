import api from '../axios'

/** 后端统一返回结构 */
export interface ApiResponse<T> {
  success: boolean
  message?: string | null
  errorCode?: string | null
  data: T
}

/** 内容实体（对应后端 ContentDO） */
export interface ContentItem {
  id: number
  creatorId: number
  title: string
  summary?: string | null
  cover?: string | null
  /** ARTICLE / TUTORIAL / EBOOK / VIDEO / PDF / CODE / PROMPT / DATASET / COLUMN */
  contentType: string
  body?: string | null
  fileUrl?: string | null
  /** FREE 免费 / SUBSCRIBED 订阅可见 */
  accessType: string
  /** DRAFT / PUBLISHED / OFFLINE */
  status: string
  viewCount?: number | null
  likeCount?: number | null
  createTime?: string
  updateTime?: string
}

/** 查询已发布内容，可用 contentType 筛选 */
export function listContents(contentType?: string) {
  return api.get<ApiResponse<ContentItem[]>>('/contents', {
    params: contentType ? { contentType } : {},
  })
}

/** 查询单篇已发布内容 */
export function getContent(id: string | number) {
  return api.get<ApiResponse<ContentItem>>(`/contents/${id}`)
}
