/**
 * 技术文章正文解析器。
 *
 * 后端 `body` 是纯文本字段（不修改 API），这里把它解析成结构化 blocks：
 *
 * - 支持 Markdown-lite：标题 / 代码围栏 / 引用 / 列表 / 任务列表 / 表格 /
 *   图片 / 分隔线 / 行内元素；
 * - 支持 ContentHub 扩展指令：`:::callout`、`@video`、`@file`、`@repo`、
 *   `@api`、`$$formula$$`、```mermaid；
 * - 旧数据（普通段落纯文本）自动降级为 paragraph，零迁移成本。
 */

import type {
  ArticleBlock, CalloutVariant, HeadingBlock, InlineSegment, ListItem, TocItem,
} from '@/types/article'

const CALLOUT_VARIANTS: CalloutVariant[] = ['tip', 'info', 'warning', 'danger', 'success']

const DEFAULT_CALLOUT_TITLE: Record<CalloutVariant, string> = {
  tip: 'Tip',
  info: 'Info',
  warning: 'Warning',
  danger: 'Important',
  success: 'Success',
}

const CJK = /[\u4e00-\u9fa5\u3000-\u303f\uff00-\uffef]/

/** 判断两个字符之间是否需要补空格（中英混排友好） */
function needSpace(prev: string, next: string): boolean {
  return !CJK.test(prev) && !CJK.test(next)
}

function joinParagraphLines(lines: string[]): string {
  let out = ''
  for (const line of lines) {
    const text = line.trim()
    if (!text) continue
    if (!out) {
      out = text
      continue
    }
    out += needSpace(out.slice(-1), text.slice(0, 1)) ? ` ${text}` : text
  }
  return out
}

// ------------------------------------------------------------------ 行内解析

interface InlinePattern {
  kind: InlineSegment['kind']
  re: RegExp
}

const INLINE_PATTERNS: InlinePattern[] = [
  { kind: 'code', re: /`([^`]+)`/ },
  { kind: 'bold', re: /\*\*([^*]+)\*\*/ },
  { kind: 'link', re: /\[([^\]]+)\]\(([^)\s]+)\)/ },
]

/** 把一行文本拆成 文本 / 粗体 / 行内代码 / 链接 片段 */
export function parseInline(source: string): InlineSegment[] {
  const segments: InlineSegment[] = []
  let rest = source

  while (rest) {
    let best: { index: number; length: number; segment: InlineSegment } | null = null

    for (const pattern of INLINE_PATTERNS) {
      const match = pattern.re.exec(rest)
      if (!match || match.index === undefined) continue
      if (best && match.index >= best.index) continue

      if (pattern.kind === 'link') {
        best = {
          index: match.index,
          length: match[0].length,
          segment: { kind: 'link', text: match[1], href: match[2] },
        }
      } else {
        best = {
          index: match.index,
          length: match[0].length,
          segment: { kind: pattern.kind, text: match[1] } as InlineSegment,
        }
      }
    }

    if (!best) {
      segments.push({ kind: 'text', text: rest })
      break
    }

    if (best.index > 0) {
      segments.push({ kind: 'text', text: rest.slice(0, best.index) })
    }
    segments.push(best.segment)
    rest = rest.slice(best.index + best.length)
  }

  return segments.filter((s) => s.kind !== 'text' || s.text !== '')
}

/** 纯文本快捷通道（无需行内解析时使用） */
export function textSegments(text: string): InlineSegment[] {
  return parseInline(text)
}

// ------------------------------------------------------------------ 锚点 id

const slugCounter = new Map<string, number>()

function headingId(text: string): string {
  let slug = text
    .trim()
    .toLowerCase()
    .replace(/[`*_[\]()]/g, '')
    .replace(/[^\w\u4e00-\u9fa5-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 48)
  if (!slug) slug = 'section'
  const used = slugCounter.get(slug) ?? 0
  slugCounter.set(slug, used + 1)
  return used === 0 ? slug : `${slug}-${used + 1}`
}

// ------------------------------------------------------------------ 主解析

interface ParseState {
  blocks: ArticleBlock[]
  sectionNo: number
  /** @api 指令挂起的 method/path，下一个代码块归它 */
  pendingApi: { method: string; path: string } | null
}

export function parseArticleBody(raw: string | null | undefined): ArticleBlock[] {
  slugCounter.clear()
  const state: ParseState = { blocks: [], sectionNo: 0, pendingApi: null }
  if (!raw) return state.blocks

  const lines = raw.replace(/\r\n?/g, '\n').split('\n')
  let i = 0

  while (i < lines.length) {
    const line = lines[i]
    const trimmed = line.trim()

    // 空行
    if (!trimmed) {
      i += 1
      continue
    }

    // 代码围栏（含 mermaid 与 @api 的示例体）
    const fence = /^```([^\s`]*)\s*(.*)$/.exec(trimmed)
    if (fence) {
      const { code, nextIndex } = readFence(lines, i, fence[1])
      pushCode(state, fence[1], fence[2], code)
      i = nextIndex
      continue
    }

    // 数学公式块
    if (trimmed.startsWith('$$')) {
      const { value, nextIndex } = readBlock(lines, i, '$$')
      state.blocks.push({ type: 'formula', formula: value })
      i = nextIndex
      continue
    }

    // Callout：:::warning 自定义标题 ... :::
    const callout = /^:::\s*([a-zA-Z]+)\s*(.*)$/.exec(trimmed)
    if (callout && CALLOUT_VARIANTS.includes(callout[1].toLowerCase() as CalloutVariant)) {
      const variant = callout[1].toLowerCase() as CalloutVariant
      const title = callout[2].trim() || DEFAULT_CALLOUT_TITLE[variant]
      const buffer: string[] = []
      i += 1
      while (i < lines.length && lines[i].trim() !== ':::') {
        buffer.push(lines[i])
        i += 1
      }
      i += 1 // 跳过结束的 :::
      state.blocks.push({
        type: 'callout',
        variant,
        title,
        segments: parseInline(joinParagraphLines(buffer)),
      })
      continue
    }

    // 标题
    const heading = /^(#{2,4})\s+(.*)$/.exec(trimmed)
    if (heading) {
      const level = heading[1].length as 2 | 3 | 4
      const text = heading[2].trim()
      const block: HeadingBlock = { type: 'heading', level, text, id: headingId(text) }
      if (level === 2) {
        state.sectionNo += 1
        block.sectionNo = String(state.sectionNo).padStart(2, '0')
      }
      state.blocks.push(block)
      i += 1
      continue
    }

    // # 一级标题在正文里按 H2 处理（文题已在 Header 呈现）
    const h1 = /^#\s+(.*)$/.exec(trimmed)
    if (h1) {
      const text = h1[1].trim()
      state.sectionNo += 1
      state.blocks.push({
        type: 'heading',
        level: 2,
        text,
        id: headingId(text),
        sectionNo: String(state.sectionNo).padStart(2, '0'),
      })
      i += 1
      continue
    }

    // 分隔线
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
      state.blocks.push({ type: 'divider' })
      i += 1
      continue
    }

    // 表格
    if (trimmed.startsWith('|')) {
      const tableLines: string[] = []
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        tableLines.push(lines[i].trim())
        i += 1
      }
      const table = buildTable(tableLines)
      if (table) state.blocks.push(table)
      continue
    }

    // 引用
    if (/^>\s?/.test(trimmed)) {
      const buffer: string[] = []
      while (i < lines.length && /^>\s?/.test(lines[i].trim())) {
        buffer.push(lines[i].trim().replace(/^>\s?/, ''))
        i += 1
      }
      state.blocks.push({ type: 'quote', segments: parseInline(joinParagraphLines(buffer)) })
      continue
    }

    // 列表 / 任务列表
    if (/^([-*+]|\d+[.)])\s+/.test(trimmed) || /^[-*+]\s+\[[ xX]\]\s+/.test(trimmed)) {
      const { block, nextIndex } = readList(lines, i)
      state.blocks.push(block)
      i = nextIndex
      continue
    }

    // 扩展指令
    const directive = /^@(video|file|repo|api)\s*:\s*(.*)$/i.exec(trimmed)
    if (directive) {
      const name = directive[1].toLowerCase()
      const value = directive[2].trim()
      if (name === 'video') {
        state.blocks.push({ type: 'video', url: value })
      } else if (name === 'file') {
        const [fileLabel, fileUrl] = splitOnce(value, '|')
        state.blocks.push({ type: 'file', name: fileLabel.trim(), url: (fileUrl || fileLabel).trim() })
      } else if (name === 'repo') {
        const [repo, description] = splitOnce(value, '|')
        state.blocks.push({ type: 'repo', repo: repo.trim(), description: description?.trim() })
      } else {
        const [method, path] = value.split(/\s+/)
        state.pendingApi = { method: (method || 'GET').toUpperCase(), path: path || '' }
      }
      i += 1
      continue
    }

    // 独立成行的图片
    const image = /^!\[([^\]]*)\]\(([^)\s]+)\)$/.exec(trimmed)
    if (image) {
      state.blocks.push({ type: 'image', url: image[2], alt: image[1], caption: image[1] || undefined })
      i += 1
      continue
    }

    // 段落（连续非空行合并）
    const buffer: string[] = []
    while (i < lines.length) {
      const current = lines[i]
      const t = current.trim()
      if (!t) break
      if (isBlockStart(t)) break
      buffer.push(current)
      i += 1
    }
    const text = joinParagraphLines(buffer)
    if (text) state.blocks.push({ type: 'paragraph', segments: parseInline(text) })
  }

  return state.blocks
}

/** 判断一行是否会开启一个新的块，用于终止段落聚合 */
function isBlockStart(line: string): boolean {
  return (
    /^```/.test(line)
    || /^#{1,4}\s/.test(line)
    || /^>\s?/.test(line)
    || /^:::\s*[a-zA-Z]/.test(line)
    || /^!\[/.test(line)
    || /^@(video|file|repo|api)\s*:/i.test(line)
    || /^(-{3,}|\*{3,}|_{3,})$/.test(line)
    || line.startsWith('|')
    || line.startsWith('$$')
    || /^([-*+]|\d+[.)])\s+/.test(line)
  )
}

/** 读取围栏代码体 */
function readFence(lines: string[], start: number, language: string) {
  void language
  const code: string[] = []
  let i = start + 1
  while (i < lines.length && !/^```/.test(lines[i].trim())) {
    code.push(lines[i])
    i += 1
  }
  return { code: trimBlank(code).join('\n'), nextIndex: i + 1 }
}

/** 读取 `$$ … $$` 块（支持单行 `$$x$$`） */
function readBlock(lines: string[], start: number, marker: string) {
  const first = lines[start].trim()
  const inlineClose = first.indexOf(marker, marker.length)
  if (inlineClose > -1) {
    return { value: first.slice(marker.length, inlineClose).trim(), nextIndex: start + 1 }
  }
  const buffer: string[] = [first.slice(marker.length)]
  let i = start + 1
  while (i < lines.length && lines[i].trim().indexOf(marker) === -1) {
    buffer.push(lines[i])
    i += 1
  }
  if (i < lines.length) {
    const closeIndex = lines[i].trim().indexOf(marker)
    buffer.push(lines[i].slice(0, closeIndex))
  }
  return { value: trimBlank(buffer).join('\n').trim(), nextIndex: i + 1 }
}

/** 挂载代码块，处理 @api 指令配对 */
function pushCode(state: ParseState, language: string, info: string, code: string) {
  const lang = (language || '').toLowerCase()

  if (lang === 'mermaid') {
    state.blocks.push({ type: 'mermaid', code })
    return
  }

  const filename = info.trim() || undefined

  if (state.pendingApi) {
    state.blocks.push({
      type: 'api',
      method: state.pendingApi.method,
      path: state.pendingApi.path,
      language: lang || 'json',
      code,
    })
    state.pendingApi = null
    return
  }

  state.blocks.push({ type: 'code', language: lang || 'text', filename, code })
}

/** 解析列表（含任务列表、缩进续行） */
function readList(lines: string[], start: number): { block: ArticleBlock; nextIndex: number } {
  const items: ListItem[] = []
  let ordered = /^\d+[.)]\s+/.test(lines[start].trim())
  let task = /^[-*+]\s+\[[ xX]\]\s+/.test(lines[start].trim())
  let i = start

  while (i < lines.length) {
    const trimmed = lines[i].trim()
    if (!trimmed) {
      // 列表内允许一个空行，后接同类型列表项
      const next = lines[i + 1]?.trim() ?? ''
      if (/^([-*+]|\d+[.)])\s+/.test(next)) {
        i += 1
        continue
      }
      break
    }

    const taskItem = /^[-*+]\s+\[([ xX])\]\s+(.*)$/.exec(trimmed)
    if (taskItem) {
      items.push({ segments: parseInline(taskItem[2].trim()), checked: taskItem[1].toLowerCase() === 'x' })
      i += 1
      continue
    }

    const listItem = /^(?:[-*+]|\d+[.)])\s+(.*)$/.exec(trimmed)
    if (listItem) {
      if (/^\d+[.)]\s+/.test(trimmed)) ordered = true
      items.push({ segments: parseInline(listItem[1].trim()) })
      i += 1
      continue
    }

    // 缩进续行：并入上一项
    if (/^\s{2,}\S/.test(lines[i]) && items.length) {
      const last = items[items.length - 1]
      const text = last.segments.map((s) => s.text).join('')
      last.segments = parseInline(`${text}${needSpace(text.slice(-1), trimmed.slice(0, 1)) ? ' ' : ''}${trimmed}`)
      i += 1
      continue
    }

    break
  }

  if (items.length === 1) task = items[0].checked !== undefined
  else task = items.every((item) => item.checked !== undefined)

  return { block: { type: 'list', ordered, task, items }, nextIndex: i }
}

/** 解析表格：首行为表头，`|---|` 分隔行忽略 */
function buildTable(tableLines: string[]): ArticleBlock | null {
  const rows = tableLines
    .map((line) => line.replace(/^\||\|$/g, '').split('|').map((cell) => cell.trim()))
    .filter((cells) => !cells.every((cell) => /^:?-{2,}:?$/.test(cell)))

  if (rows.length === 0) return null
  const header = rows.shift() ?? []
  const width = header.length
  const normalized = rows.map((cells) => {
    const out = cells.slice(0, width)
    while (out.length < width) out.push('')
    return out
  })

  return { type: 'table', header, rows: normalized }
}

function splitOnce(value: string, sep: string): [string, string | undefined] {
  const index = value.indexOf(sep)
  if (index === -1) return [value, undefined]
  return [value.slice(0, index), value.slice(index + sep.length)]
}

function trimBlank(lines: string[]): string[] {
  const out = [...lines]
  while (out.length && !out[0].trim()) out.shift()
  while (out.length && !out[out.length - 1].trim()) out.pop()
  return out
}

// ------------------------------------------------------------------ 派生数据

/** 提取 H2 / H3 生成目录 */
export function extractToc(blocks: ArticleBlock[]): TocItem[] {
  return blocks
    .filter((block): block is HeadingBlock => block.type === 'heading' && block.level !== 4)
    .map((block) => ({
      id: block.id,
      level: block.level as 2 | 3,
      text: block.text,
      sectionNo: block.sectionNo,
    }))
}

/** 按中英文混合估算阅读时长（分钟，至少 1） */
export function estimateReadTime(blocks: ArticleBlock[]): number {
  let cjk = 0
  let words = 0
  const scan = (text: string) => {
    for (const char of text) {
      if (CJK.test(char)) cjk += 1
    }
    words += text.replace(/[\u4e00-\u9fa5]/g, ' ').split(/\s+/).filter(Boolean).length
  }

  const walk = (list: InlineSegment[] | undefined) => {
    list?.forEach((segment) => scan(segment.text))
  }

  blocks.forEach((block) => {
    if (block.type === 'paragraph' || block.type === 'quote' || block.type === 'callout') walk(block.segments)
    else if (block.type === 'heading') scan(block.text)
    else if (block.type === 'list') block.items.forEach((item) => walk(item.segments))
    else if (block.type === 'code' || block.type === 'mermaid' || block.type === 'api') words += 40
    else if (block.type === 'table') block.rows.forEach((row) => row.forEach(scan))
  })

  const minutes = cjk / 380 + words / 220
  return Math.max(1, Math.round(minutes))
}

/** 截断 blocks（未解锁试读用） */
export function truncateBlocks(blocks: ArticleBlock[], max: number): ArticleBlock[] {
  return blocks.slice(0, max)
}
