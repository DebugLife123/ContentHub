<template>
  <div class="list-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">CONTENT LIBRARY</p><h2>内容库<br><em>全部已发布内容</em></h2></div>
      <p class="heading-aside">共 {{ total }} 份内容<br>按栏目浏览，或直接搜索标题与摘要。</p>
    </div>

    <!-- 栏目：按内容形态划分，来自 GET /api/categories -->
    <div class="category-row">
      <span :class="{ active: filters.categoryId === null }" @click="selectCategory(null)">全部内容</span>
      <span
        v-for="category in categories"
        :key="category.id"
        :class="{ active: filters.categoryId === category.id }"
        @click="selectCategory(category.id)"
      >
        {{ category.name }}
      </span>
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
      <el-button class="button button-dark" @click="applyFilters">筛选</el-button>
    </div>

    <!--
      三态分离（改造前只有「正在加载…」和一句「没有符合条件的内容」）：
      请求失败以前被 catch 吞掉、把列表置空，于是「网络断了」和「确实没数据」
      在界面上长得一模一样，用户既不知道发生了什么，也没有重试的入口。
    -->
    <AppSkeleton v-if="loading" variant="cards" :count="6" />

    <AppError
      v-else-if="error"
      :message="error"
      :retrying="loading"
      @retry="load"
    />

    <AppEmpty
      v-else-if="!items.length"
      :filtered="hasFilter"
      @reset="resetFilters"
    />

    <div v-else class="content-grid">
      <article v-for="item in items" :key="item.id" class="content-card" @click="$router.push(`/content/${item.id}`)">
        <!-- 创作者上传过封面就用图片；没上传则回落到按 id 生成的色块 + 标题文字 -->
        <div v-if="item.cover" class="content-cover is-image">
          <img :src="item.cover" :alt="item.title">
          <span class="cover-type">{{ item.contentType }}</span>
          <span class="cover-arrow">↗</span>
        </div>
        <div v-else class="content-cover" :class="themeOf(item.id)">
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

    <div v-if="!loading && !error && items.length" class="pager">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { pageContents } from '../api/content'
import { listCategories } from '../api/category'
import type { Category, ContentItem } from '../api/types'
import AppSkeleton from '../components/state/AppSkeleton.vue'
import AppEmpty from '../components/state/AppEmpty.vue'
import AppError from '../components/state/AppError.vue'

const route = useRoute()

const loading = ref(true)
const error = ref('')
const items = ref<ContentItem[]>([])
const total = ref(0)
const categories = ref<Category[]>([])

const filters = reactive({
  pageNum: 1,
  pageSize: 9,
  categoryId: null as number | null,
  keyword: '',
})

/** 有筛选条件时，空结果要归因到「条件太窄」而不是「这里本来就没东西」 */
const hasFilter = computed(() => filters.categoryId !== null || filters.keyword.trim() !== '')

const themes = ['theme-orange', 'theme-lilac', 'theme-ink', 'theme-yellow']
function themeOf(id: number) {
  return themes[id % themes.length]
}
function coverText(title: string) {
  return (title || 'CONTENT').toUpperCase().split(' ').slice(0, 3).join('\n')
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await pageContents({
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      categoryId: filters.categoryId,
      keyword: filters.keyword || undefined,
    })
    if (res.data.success) {
      items.value = res.data.data.list
      total.value = res.data.data.total
    } else {
      items.value = []
      total.value = 0
      error.value = res.data.message || '服务端没有返回数据。'
    }
  } catch {
    // 失败时保留「加载失败」这个事实，不要伪装成空列表
    items.value = []
    total.value = 0
    error.value = '没能取到内容列表，请检查网络后重试。'
  } finally {
    loading.value = false
  }
}

/** 改筛选条件时回到第一页，否则会停在一个空页上 */
function applyFilters() {
  filters.pageNum = 1
  load()
}

/** 点击顶部栏目：切换分类并立刻重新查询 */
function selectCategory(id: number | null) {
  filters.categoryId = id
  applyFilters()
}

/** 空状态里的「清空筛选条件」 */
function resetFilters() {
  filters.categoryId = null
  filters.keyword = ''
  applyFilters()
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
  // 支持从文章标签点进来：/contents?keyword=Spring
  const keyword = route.query.keyword
  if (typeof keyword === 'string' && keyword) filters.keyword = keyword
  try {
    const res = await listCategories()
    if (res.data.success) categories.value = res.data.data
  } catch {
    // 栏目加载失败不影响主列表，静默降级为「只有全部内容」一个入口
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
  padding: 6px 0 30px;
}
.filter-keyword {
  width: 260px;
}
.category-row span {
  user-select: none;
}
/* 上传了封面的卡片：图片铺满，类型标签浮在左上角 */
.content-cover.is-image {
  position: relative;
  padding: 0;
  overflow: hidden;
}
.content-cover.is-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s;
}
.content-card:hover .content-cover.is-image img {
  transform: scale(1.04);
}
.content-cover.is-image .cover-type {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(245, 242, 236, 0.9);
  padding: 3px 8px;
}
.content-cover.is-image .cover-arrow {
  color: #fff;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.5);
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 38px 0 10px;
}
</style>
