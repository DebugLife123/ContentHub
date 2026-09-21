<template>
  <div class="art-editor">
    <!-- 工具栏 -->
    <div class="art-editor-toolbar">
      <div class="art-editor-tools">
        <button type="button" title="章节标题 H2" @click="wrapLine('## ')">H2</button>
        <button type="button" title="小节标题 H3" @click="wrapLine('### ')">H3</button>
        <span class="art-editor-sep" />
        <button type="button" title="粗体" @click="wrapSelection('**', '**', '强调文字')"><b>B</b></button>
        <button type="button" title="行内代码" @click="wrapSelection('`', '`', 'code')"><i>`code`</i></button>
        <button type="button" title="链接" @click="insertLink">a↗</button>
        <span class="art-editor-sep" />
        <button type="button" title="引用" @click="wrapLine('> ')">&gt;</button>
        <button type="button" title="无序列表" @click="wrapLine('- ')">•</button>
        <button type="button" title="有序列表" @click="wrapLine('1. ')">1.</button>
        <button type="button" title="任务项" @click="wrapLine('- [ ] ')">☑</button>
        <span class="art-editor-sep" />
        <button type="button" title="代码块" @click="insertBlock('```java UserService.java\n\n```', 25)">{ }</button>
        <button type="button" title="表格" @click="insertBlock('| 列 A | 列 B | 列 C |\n| --- | --- | --- |\n|  |  |  |')">▦</button>
        <button type="button" title="图片" @click="insertBlock('![图片说明](https://)')">img</button>
        <el-dropdown trigger="click" @command="insertCallout">
          <button type="button" title="提示框" @click.prevent>:::⌄</button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="tip">💡 Tip · 最佳实践</el-dropdown-item>
              <el-dropdown-item command="info">ℹ️ Info · 补充说明</el-dropdown-item>
              <el-dropdown-item command="warning">⚠️ Warning · 注意事项</el-dropdown-item>
              <el-dropdown-item command="danger">⛔ Important · 风险警示</el-dropdown-item>
              <el-dropdown-item command="success">✅ Success · 正确示范</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button type="button" title="分隔线" @click="insertBlock('---')">—</button>
        <el-dropdown trigger="click" @command="insertDirective">
          <button type="button" title="扩展指令" @click.prevent>@⌄</button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="video">🎬 @video: 视频地址</el-dropdown-item>
              <el-dropdown-item command="file">📄 @file: 名称 | 附件地址</el-dropdown-item>
              <el-dropdown-item command="repo">▣ @repo: owner/name | 描述</el-dropdown-item>
              <el-dropdown-item command="api">⇄ @api: METHOD /path + 代码块</el-dropdown-item>
              <el-dropdown-item command="math">∑ $$ 数学公式 $$</el-dropdown-item>
              <el-dropdown-item command="mermaid">📊 ```mermaid 图表</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="art-editor-side">
        <span class="art-editor-stats">{{ stats }}</span>
        <div class="art-editor-mode">
          <button
            type="button"
            :class="{ 'is-active': mode === 'write' }"
            @click="mode = 'write'"
          >编写</button>
          <button
            type="button"
            :class="{ 'is-active': mode === 'preview' }"
            @click="mode = 'preview'"
          >预览</button>
        </div>
      </div>
    </div>

    <!-- 编写区 -->
    <textarea
      v-show="mode === 'write'"
      ref="textareaRef"
      class="art-editor-input"
      :value="text"
      :placeholder="placeholder"
      spellcheck="false"
      @input="onInput"
    />

    <!-- 预览区：与读者端完全相同的渲染管线 -->
    <div v-show="mode === 'preview'" class="art-editor-preview">
      <p v-if="!text.trim()" class="art-editor-empty">正文为空，先在「编写」里写点什么。</p>
      <template v-else>
        <p class="art-editor-preview-note">
          预览效果与读者端一致 · {{ tocCount }} 个章节
        </p>
        <ArticleBody :blocks="previewBlocks" />
      </template>
    </div>

    <!-- 语法速查 -->
    <details class="art-editor-cheatsheet">
      <summary>语法速查</summary>
      <table>
        <tbody>
          <tr><td><code>## 标题</code></td><td>章节（自动编号 + 进入目录）</td></tr>
          <tr><td><code>### 小节</code></td><td>小节标题</td></tr>
          <tr><td><code>**粗体**</code> / <code>`代码`</code> / <code>[文字](url)</code></td><td>行内元素</td></tr>
          <tr><td><code>```java 文件名</code></td><td>代码块（工具栏 / 行号 / 复制）</td></tr>
          <tr><td><code>&gt; 引用</code></td><td>引用块</td></tr>
          <tr><td><code>- 列表</code> / <code>1. 列表</code> / <code>- [ ] 任务</code></td><td>三种列表</td></tr>
          <tr><td><code>| A | B |</code>（次行 <code>| --- |</code>）</td><td>表格</td></tr>
          <tr><td><code>![说明](url)</code></td><td>图片（自动编号 + 点击放大）</td></tr>
          <tr><td><code>:::tip|info|warning|danger|success 标题</code> … <code>:::</code></td><td>提示框</td></tr>
          <tr><td><code>---</code></td><td>分隔线</td></tr>
          <tr><td><code>@video: url</code></td><td>视频（直链 / 嵌入页）</td></tr>
          <tr><td><code>@file: 名称 | url</code></td><td>附件下载行</td></tr>
          <tr><td><code>@repo: owner/name | 描述</code></td><td>GitHub 仓库卡</td></tr>
          <tr><td><code>@api: GET /path</code> + 紧跟代码块</td><td>API 示例</td></tr>
          <tr><td><code>$$ 公式 $$</code></td><td>数学公式</td></tr>
          <tr><td><code>```mermaid</code></td><td>图表源码（渲染引擎预留）</td></tr>
        </tbody>
      </table>
    </details>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { estimateReadTime, extractToc, parseArticleBody } from '@/utils/articleParser'
import ArticleBody from './ArticleBody.vue'

const props = withDefaults(defineProps<{
  /** 兼容后端可能返回的 null（老数据正文为空） */
  modelValue?: string | null
  placeholder?: string
}>(), {
  modelValue: '',
  placeholder: '',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

/** 正文源文本（统一为非空字符串） */
const text = computed(() => props.modelValue ?? '')

const mode = ref<'write' | 'preview'>('write')
const textareaRef = ref<HTMLTextAreaElement>()

/** 预览与读者端共用同一条解析管线，保证「所见即所得」 */
const previewBlocks = computed(() => parseArticleBody(text.value))
const tocCount = computed(() => extractToc(previewBlocks.value).filter((t) => t.level === 2).length)

const stats = computed(() => {
  const chars = text.value.length
  if (!chars) return ''
  return `${chars} 字 · 约 ${estimateReadTime(previewBlocks.value)} min`
})

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
}

// ------------------------------------------------------------------ 插入逻辑

/** 包裹选区（粗体 / 行内代码） */
function wrapSelection(prefix: string, suffix: string, placeholder: string) {
  const el = textareaRef.value
  if (!el) return
  const { selectionStart: start, selectionEnd: end, value } = el
  const selected = value.slice(start, end) || placeholder
  const next = `${value.slice(0, start)}${prefix}${selected}${suffix}${value.slice(end)}`
  emit('update:modelValue', next)
  restoreCaret(el, start + prefix.length, start + prefix.length + selected.length)
}

/** 行首前缀（标题 / 引用 / 列表） */
function wrapLine(prefix: string) {
  const el = textareaRef.value
  if (!el) return
  const { selectionStart: start, value } = el
  const lineStart = value.lastIndexOf('\n', start - 1) + 1
  const next = `${value.slice(0, lineStart)}${prefix}${value.slice(lineStart)}`
  emit('update:modelValue', next)
  restoreCaret(el, start + prefix.length)
}

/** 插入独立块（前后自动补空行），caretOffset 定位光标到块内 */
function insertBlock(snippet: string, caretOffset?: number) {
  const el = textareaRef.value
  if (!el) return
  const { selectionStart: start, value } = el
  const before = value.slice(0, start)
  const after = value.slice(start)
  const lead = before === '' || before.endsWith('\n\n') ? '' : before.endsWith('\n') ? '\n' : '\n\n'
  const tail = after === '' || after.startsWith('\n') ? '' : '\n'
  const insertion = `${lead}${snippet}\n`
  const next = `${before}${insertion}${tail}${after}`
  emit('update:modelValue', next)
  const caret = start + lead.length + (caretOffset ?? snippet.length)
  restoreCaret(el, caret)
}

function insertLink() {
  const el = textareaRef.value
  if (!el) return
  const { selectionStart: start, selectionEnd: end, value } = el
  const selected = value.slice(start, end) || '链接文字'
  const snippet = `[${selected}](https://)`
  const next = `${value.slice(0, start)}${snippet}${value.slice(end)}`
  emit('update:modelValue', next)
  // 光标定位到 url 处，方便直接粘贴
  const urlStart = start + selected.length + 3
  restoreCaret(el, urlStart, urlStart + 8)
}

function insertCallout(variant: string) {
  insertBlock(`:::${variant} 标题\n内容\n:::`)
}

function insertDirective(name: string) {
  const snippets: Record<string, string> = {
    video: '@video: https://',
    file: '@file: 附件名称 | https://',
    repo: '@repo: owner/name | 一句话描述',
    api: '@api: GET /api/example\n```json\n{\n  "code": 0\n}\n```',
    math: '$$ E = mc^2 $$',
    mermaid: '```mermaid\nflowchart LR\n  A --> B\n```',
  }
  insertBlock(snippets[name] ?? '')
}

/** 更新值后还原光标（等 Vue 把新值写回 textarea）；预览态下点工具栏自动切回编写 */
function restoreCaret(el: HTMLTextAreaElement, start: number, end = start) {
  mode.value = 'write'
  requestAnimationFrame(() => {
    el.focus()
    el.setSelectionRange(start, end)
  })
}
</script>

<style scoped>
.art-editor {
  border: 1px solid var(--line);
  background: var(--paper);
  width: 100%;
}

.art-editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 10px;
  border-bottom: 1px solid var(--line);
  background: var(--art-surface, #edeae2);
}

.art-editor-tools {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;

  & button {
    border: 0;
    background: transparent;
    padding: 5px 8px;
    font: 500 11px 'DM Mono', monospace;
    color: var(--ink);
    cursor: pointer;
    border-radius: 3px;

    &:hover { background: rgba(23, 23, 23, 0.07); }

    & i, & b { font-style: normal; font-weight: 500; }
  }
}

.art-editor-sep {
  width: 1px;
  height: 16px;
  background: var(--line);
  margin: 0 6px;
}

.art-editor-side {
  display: flex;
  align-items: center;
  gap: 14px;
}

.art-editor-stats {
  font: 400 10px 'DM Mono', monospace;
  color: var(--muted);
}

.art-editor-mode {
  display: flex;
  border: 1px solid var(--line);

  & button {
    border: 0;
    background: transparent;
    padding: 4px 12px;
    font: 500 11px 'DM Mono', monospace;
    color: var(--muted);
    cursor: pointer;

    &.is-active {
      background: var(--ink);
      color: #fff;
    }
  }
}

.art-editor-input {
  display: block;
  width: 100%;
  min-height: 420px;
  border: 0;
  outline: none;
  resize: vertical;
  padding: 18px 20px;
  background: var(--paper);
  color: var(--art-text, #26241f);
  font: 400 14px/1.9 'DM Mono', 'Microsoft YaHei', monospace;
  box-sizing: border-box;
}

.art-editor-preview {
  min-height: 420px;
  padding: 22px 26px;
  background: var(--paper);
}

.art-editor-preview-note {
  margin: 0 0 24px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line);
  font: 400 11px 'DM Mono', monospace;
  color: var(--muted);
}

.art-editor-empty {
  padding: 60px 0;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}

.art-editor-cheatsheet {
  border-top: 1px solid var(--line);
  font-size: 12px;

  & summary {
    padding: 8px 12px;
    font: 500 11px 'DM Mono', monospace;
    letter-spacing: 0.08em;
    color: var(--muted);
    cursor: pointer;
    user-select: none;

    &:hover { color: var(--ink); }
  }

  & table {
    width: 100%;
    border-collapse: collapse;
    font-family: Manrope, 'Microsoft YaHei', sans-serif;
  }

  & td {
    padding: 7px 12px;
    border-top: 1px solid var(--line);
    color: var(--art-text, #26241f);
    line-height: 1.6;
  }

  & td:first-child { width: 55%; }

  & code {
    padding: 1px 5px;
    border-radius: 3px;
    background: var(--art-code-inline-bg, #eae4d7);
    font: 400 11.5px 'DM Mono', monospace;
    color: var(--art-accent-deep, #a03d12);
    white-space: nowrap;
  }
}
</style>
