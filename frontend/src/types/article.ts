/**
 * 技术文章详情页 · 结构化正文类型定义（视图模型层）。
 *
 * 后端 `body` 字段保持纯文本不变，前端通过
 * `@/utils/articleParser` 解析为这里的结构化 blocks 再渲染。
 * 后续接入 Markdown / MDX / 富文本编辑器时，只需新增对应 parser，
 * 渲染层（components/article/）零改动。
 */

// ---------------------------------------------------------------- 行内元素

export type InlineSegment =
  | { kind: 'text'; text: string }
  | { kind: 'bold'; text: string }
  | { kind: 'code'; text: string }
  | { kind: 'link'; text: string; href: string }

// ---------------------------------------------------------------- 块级元素

export interface HeadingBlock {
  type: 'heading'
  level: 2 | 3 | 4
  text: string
  /** 锚点 id，TOC 定位用 */
  id: string
  /** 杂志式章节编号（仅 H2 有），如 "01" */
  sectionNo?: string
}

export interface ParagraphBlock {
  type: 'paragraph'
  segments: InlineSegment[]
}

export interface QuoteBlock {
  type: 'quote'
  segments: InlineSegment[]
}

export interface CodeBlock {
  type: 'code'
  language: string
  filename?: string
  code: string
}

export interface ImageBlock {
  type: 'image'
  url: string
  alt?: string
  caption?: string
}

export interface ListItem {
  segments: InlineSegment[]
  /** 任务列表专用；普通列表为 undefined */
  checked?: boolean
}

export interface ListBlock {
  type: 'list'
  ordered: boolean
  /** 整组均为任务项时视为 task list */
  task: boolean
  items: ListItem[]
}

export interface TableBlock {
  type: 'table'
  header: string[]
  rows: string[][]
}

export type CalloutVariant = 'tip' | 'info' | 'warning' | 'danger' | 'success'

export interface CalloutBlock {
  type: 'callout'
  variant: CalloutVariant
  title?: string
  segments: InlineSegment[]
}

export interface DividerBlock {
  type: 'divider'
}

export interface VideoBlock {
  type: 'video'
  url: string
  caption?: string
}

export interface FileBlock {
  type: 'file'
  name: string
  url: string
}

export interface RepoBlock {
  type: 'repo'
  /** owner/name */
  repo: string
  description?: string
}

export interface ApiBlock {
  type: 'api'
  method: string
  path: string
  language: string
  code: string
}

export interface FormulaBlock {
  type: 'formula'
  formula: string
}

export interface MermaidBlock {
  type: 'mermaid'
  code: string
}

export type ArticleBlock =
  | HeadingBlock
  | ParagraphBlock
  | QuoteBlock
  | CodeBlock
  | ImageBlock
  | ListBlock
  | TableBlock
  | CalloutBlock
  | DividerBlock
  | VideoBlock
  | FileBlock
  | RepoBlock
  | ApiBlock
  | FormulaBlock
  | MermaidBlock

// ---------------------------------------------------------------- TOC

export interface TocItem {
  id: string
  level: 2 | 3
  text: string
  /** 章节编号，仅 H2 有 */
  sectionNo?: string
}

// ---------------------------------------------------------------- 文章视图模型

export interface ArticleAuthor {
  name: string
  avatar?: string
  bio?: string
}

/** 相关文章 / 上一篇下一篇 共用的精简结构 */
export interface ArticleNavItem {
  id: number
  title: string
  contentType?: string
  viewCount?: number
}

export interface Article {
  id: number
  title: string
  summary: string
  category: string
  contentType: string
  accessType: string
  author: ArticleAuthor
  tags: string[]
  publishTime: string
  updateTime?: string
  /** 预计阅读分钟数，按字数估算 */
  readTime: number
  viewCount: number
  likeCount: number
  collectCount: number
  commentCount: number
  favorited: boolean
  blocks: ArticleBlock[]
  toc: TocItem[]
}
