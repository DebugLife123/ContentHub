<template>
  <div class="skill-page content-width">
    <div class="section-heading">
      <div>
        <p class="eyebrow">SKILL MARKETPLACE</p>
        <h2>Skill 商城<br /><em>按 GitHub 星数排序</em></h2>
      </div>
      <p class="heading-aside">
        共 {{ total }} 个 Skill<br />
        <template v-if="isMember">会员已激活，全部内容可安装。</template>
        <template v-else>标「会员」的 Skill 需订阅后解锁。</template>
      </p>
    </div>

    <!-- 分类选项 -->
    <div class="category-row">
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
        placeholder="搜索 Skill 名称、作者或标签"
        clearable
        class="filter-keyword"
        @keyup.enter="applyFilters"
        @clear="applyFilters"
      />
      <el-select v-model="filters.sort" class="filter-sort" @change="applyFilters">
        <el-option label="星数优先" value="stars" />
        <el-option label="最近更新" value="updated" />
        <el-option label="按名称" value="name" />
      </el-select>
      <el-button class="button button-dark" @click="applyFilters">筛选</el-button>
    </div>

    <div v-if="loading" class="empty-state">正在加载 Skill…</div>
    <div v-else-if="!items.length" class="empty-state">没有符合条件的 Skill。</div>
    <div v-else class="skill-grid">
      <article v-for="item in items" :key="item.id" class="skill-card" @click="open(item.id)">
        <div class="skill-head">
          <span class="skill-icon">{{ item.icon }}</span>
          <div class="skill-title">
            <strong>{{ item.name }}</strong>
            <small>{{ item.repo }}</small>
          </div>
          <span class="access-badge" :class="item.accessType === 'FREE' ? 'is-free' : 'is-member'">
            {{ item.accessType === 'FREE' ? '免费' : '会员' }}
          </span>
        </div>

        <p class="skill-summary">{{ item.summary }}</p>

        <div class="skill-platforms">
          <span v-for="p in item.platforms.slice(0, 3)" :key="p">{{ p }}</span>
        </div>

        <div class="skill-foot">
          <span class="stars">★ {{ formatStars(item.stars) }}</span>
          <span>{{ item.version }}</span>
          <span>{{ item.updatedAt }}</span>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listSkillCategories, pageSkills, formatStars } from '../../api/skill'
import { useMembershipStore } from '@/stores/membership'
import { useUserStore } from '@/stores/user'
import type { SkillCategory, SkillItem } from '../../api/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const membership = useMembershipStore()

const loading = ref(true)
const items = ref<SkillItem[]>([])
const total = ref(0)
const categories = ref<SkillCategory[]>([])

/** 未登录时会员状态一律按 false 处理，避免退出后残留上一个账号的状态 */
const isMember = computed(() => userStore.isLoggedIn && membership.isMember)

const filters = reactive({
  categoryId: 'all',
  keyword: '',
  sort: 'stars' as 'stars' | 'updated' | 'name',
})

async function load() {
  loading.value = true
  try {
    const res = await pageSkills({
      categoryId: filters.categoryId,
      keyword: filters.keyword || undefined,
      sort: filters.sort,
    })
    items.value = res.list
    total.value = res.total
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  load()
}

function selectCategory(id: string) {
  filters.categoryId = id
  // 同步到地址栏：分类可以被分享，浏览器后退也能回到上一个分类
  router.replace({ path: '/skills', query: id === 'all' ? {} : { categoryId: id } })
  load()
}

function open(id: string) {
  router.push(`/skills/${id}`)
}

onMounted(async () => {
  // 支持从外部带分类进来：/skills?categoryId=dev
  const fromQuery = route.query.categoryId
  if (typeof fromQuery === 'string' && fromQuery) {
    filters.categoryId = fromQuery
  }
  try {
    categories.value = await listSkillCategories()
  } catch {
    categories.value = []
  }
  await membership.ensureLoaded()
  await load()
})
</script>

<style scoped>
.skill-page {
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
  width: 300px;
}
.filter-sort {
  width: 150px;
}
.category-row span {
  user-select: none;
}
.skill-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.skill-card {
  border: 1px solid var(--line);
  padding: 22px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: border-color 0.25s, transform 0.25s;
}
.skill-card:hover {
  border-color: var(--ink);
  transform: translateY(-4px);
}
.skill-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.skill-icon {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: #f3e7cf;
}
.skill-title {
  flex: 1;
  min-width: 0;
}
.skill-title strong {
  display: block;
  font-size: 16px;
  letter-spacing: -0.03em;
  word-break: break-all;
}
.skill-title small {
  display: block;
  margin-top: 5px;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  word-break: break-all;
}
.access-badge {
  flex: 0 0 auto;
  font: 10px 'DM Mono', monospace;
  padding: 3px 8px;
  border: 1px solid var(--line);
}
.access-badge.is-free {
  color: #4f6b28;
  border-color: #b6c98a;
  background: rgba(201, 212, 99, 0.22);
}
.access-badge.is-member {
  color: #a8481f;
  border-color: #e8b394;
  background: rgba(241, 125, 71, 0.12);
}
.skill-summary {
  margin: 0;
  font-size: 13px;
  line-height: 1.75;
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.skill-platforms {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.skill-platforms span {
  font: 9px 'DM Mono', monospace;
  color: var(--muted);
  border: 1px solid var(--line);
  padding: 3px 7px;
}
.skill-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--line);
  margin-top: auto;
  padding-top: 13px;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
}
.skill-foot .stars {
  color: var(--ink);
}
</style>
