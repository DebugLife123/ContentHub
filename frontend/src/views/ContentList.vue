<template>
  <div class="list-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">CONTENT LIBRARY</p><h2>内容库<br><em>全部已发布内容</em></h2></div>
      <p class="heading-aside">共 {{ total }} 份内容<br>支持按分类、类型与关键词筛选。</p>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索标题或摘要"
        clearable
        class="filter-keyword"
        @keyup.enter="applyFilters"
        @clear="applyFilters"
      />
      <el-select v-model="filters.categoryId" placeholder="全部分类" clearable class="filter-select" @change="applyFilters">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filters.contentType" placeholder="全部类型" clearable class="filter-select" @change="applyFilters">
        <el-option v-for="t in contentTypes" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-button class="button button-dark" @click="applyFilters">筛选</el-button>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!items.length" class="empty-state">没有符合条件的内容。</div>
    <div v-else class="content-grid">
      <article v-for="item in items" :key="item.id" class="content-card" @click="$router.push(`/content/${item.id}`)">
        <div class="content-cover" :class="themeOf(item.id)">
          <span class="cover-type">{{ item.contentType }}</span>
          <strong>{{ coverText(item.title) }}</strong>
          <span class="cover-arrow">↗</span>
        </div>
        <div class="content-meta">
          <span>{{ item.categoryName || '未分类' }}</span>
          <span>{{ item.viewCount || 0 }} 次阅读</span>
        </div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.summary || '一份正在持续更新的数字内容。' }}</p>
        <div class="content-foot">
          <span>{{ item.accessType === 'FREE' ? '免费阅读' : '订阅后阅读' }}</span>
          <span>♡ {{ item.likeCount || 0 }}</span>
        </div>
      </article>
    </div>

    <div class="pager">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :current-page="filters.pageNum"
        :page-size="filters.pageSize"
        background
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { pageContents } from '../api/content'
import { listCategories } from '../api/category'
import type { Category, ContentItem } from '../api/types'

const route = useRoute()

const loading = ref(true)
const items = ref<ContentItem[]>([])
const total = ref(0)
const categories = ref<Category[]>([])

const contentTypes = [
  { label: '技术文章', value: 'ARTICLE' },
  { label: '系列教程', value: 'TUTORIAL' },
  { label: '电子书', value: 'EBOOK' },
  { label: '视频课程', value: 'VIDEO' },
  { label: 'PDF', value: 'PDF' },
  { label: '代码模板', value: 'CODE' },
  { label: 'Prompt', value: 'PROMPT' },
  { label: '数据集', value: 'DATASET' },
  { label: '专栏', value: 'COLUMN' },
]

const filters = reactive({
  pageNum: 1,
  pageSize: 9,
  categoryId: null as number | null,
  contentType: '' as string,
  keyword: '',
})

const themes = ['theme-orange', 'theme-lilac', 'theme-ink', 'theme-yellow']
function themeOf(id: number) {
  return themes[id % themes.length]
}
function coverText(title: string) {
  return (title || 'CONTENT').toUpperCase().split(' ').slice(0, 3).join('\n')
}

async function load() {
  loading.value = true
  try {
    const res = await pageContents({
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      categoryId: filters.categoryId,
      contentType: filters.contentType || undefined,
      keyword: filters.keyword || undefined,
    })
    if (res.data.success) {
      items.value = res.data.data.list
      total.value = res.data.data.total
    }
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 改筛选条件时回到第一页，否则会停在一个空页上 */
function applyFilters() {
  filters.pageNum = 1
  load()
}

function handlePageChange(page: number) {
  filters.pageNum = page
  load()
}

onMounted(async () => {
  // 支持从首页分类点进来：/contents?categoryId=2
  const categoryId = route.query.categoryId
  if (categoryId) {
    const parsed = Number(categoryId)
    if (!Number.isNaN(parsed)) filters.categoryId = parsed
  }
  try {
    const res = await listCategories()
    if (res.data.success) categories.value = res.data.data
  } catch {
    categories.value = []
  }
  await load()
})
</script>

<style scoped>
.list-page {
  padding: 60px 0 20px;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  padding: 22px 0 30px;
}
.filter-keyword {
  width: 260px;
}
.filter-select {
  width: 170px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 38px 0 10px;
}
</style>
