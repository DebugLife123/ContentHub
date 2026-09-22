/**
 * ContentItem（后端模型） → Article（文章详情页视图模型）。
 *
 * 只做前端组装：解析正文、估算阅读时长、整理展示字段。
 * 不改动任何后端 API。
 */

import type { ContentItem } from '@/api/types'
import type { Article } from '@/types/article'
import { estimateReadTime, extractToc, parseArticleBody } from '@/utils/articleParser'

/** 内容类型展示名 */
const CONTENT_TYPE_LABEL: Record<string, string> = {
  ARTICLE: '技术文章',
  TUTORIAL: '教程',
  EBOOK: '电子书',
  VIDEO: '视频',
  PDF: '文档',
  CODE: '代码',
  PROMPT: 'Prompt',
  DATASET: '数据集',
  COLUMN: '专栏',
}

/** 内容状态展示名（阅读状态） */
export const CONTENT_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  PENDING: '审核中',
  PUBLISHED: '已发布',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
}

export function contentTypeLabel(type: string | undefined): string {
  return (type && CONTENT_TYPE_LABEL[type]) || type || '文章'
}

/** "2026-09-21 10:30:00" / ISO → "2026.09.21" */
export function formatDate(value?: string | null): string {
  if (!value) return ''
  const match = /^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/.exec(value)
  if (!match) return value
  const [, year, month, day] = match
  return `${year}.${month.padStart(2, '0')}.${day.padStart(2, '0')}`
}

/** 1200 → 1.2k */
export function formatCount(value?: number | null): string {
  const num = value ?? 0
  if (num >= 10000) return `${(num / 10000).toFixed(1)}w`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
  return String(num)
}

/** 组标签：分类 + 内容类型（后端暂无 tags 字段，先派生，结构已预留） */
function deriveTags(content: ContentItem): string[] {
  const tags = new Set<string>()
  if (content.categoryName) tags.add(content.categoryName)
  const typeLabel = CONTENT_TYPE_LABEL[content.contentType]
  if (typeLabel && typeLabel !== content.categoryName) tags.add(typeLabel)
  return [...tags]
}

export function toArticle(content: ContentItem): Article {
  const blocks = parseArticleBody(content.body)
  // 附件并入正文末尾，走统一的 File 组件
  if (content.fileUrl) {
    const name = content.fileUrl.split('/').pop() || '附件'
    blocks.push({ type: 'file', name, url: content.fileUrl })
  }
  return {
    id: content.id,
    title: content.title,
    summary: content.summary || '',
    cover: content.cover || undefined,
    category: content.categoryName || '未分类',
    contentType: contentTypeLabel(content.contentType),
    accessType: content.accessType,
    author: {
      id: content.creatorId,
      name: `创作者 #${content.creatorId}`,
      bio: 'ContentHub 数字内容创作者',
    },
    tags: deriveTags(content),
    publishTime: formatDate(content.createTime),
    updateTime: formatDate(content.updateTime),
    readTime: estimateReadTime(blocks),
    viewCount: content.viewCount ?? 0,
    likeCount: content.likeCount ?? 0,
    collectCount: content.favoriteCount ?? 0,
    commentCount: content.commentCount ?? 0,
    favorited: !!content.favorited,
    blocks,
    toc: extractToc(blocks),
  }
}
