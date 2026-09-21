<template>
  <div class="skill-detail content-width">
    <nav class="crumbs">
      <RouterLink to="/">首页</RouterLink>
      <span>/</span>
      <RouterLink to="/skills">Skills</RouterLink>
      <span>/</span>
      <span class="crumb-current">{{ name || '详情' }}</span>
    </nav>

    <div v-if="loading" class="empty-state">正在加载 Skill…</div>
    <div v-else-if="error" class="empty-state">{{ error }}</div>
    <template v-else>
      <!-- 头部 -->
      <header class="skill-hero">
        <span class="hero-icon">{{ skill.icon }}</span>
        <div class="hero-main">
          <h1>{{ skill.name }}</h1>
          <p class="hero-summary">{{ skill.summary }}</p>
          <div class="hero-meta">
            <span class="chip">{{ skill.version }}</span>
            <span class="chip">★ {{ formatStars(skill.stars) }}</span>
            <span class="chip">↓ {{ skill.downloads.toLocaleString() }}</span>
            <span class="chip" :class="skill.accessType === 'FREE' ? 'is-free' : 'is-member'">
              {{ skill.accessType === 'FREE' ? '免费' : '会员解锁' }}
            </span>
          </div>
        </div>
        <div class="hero-actions">
          <el-button
            class="install-button"
            :disabled="skill.locked"
            @click="handleInstall"
          >
            ↓ {{ skill.locked ? '会员解锁后可安装' : '安装' }}
          </el-button>
          <a class="ghost-button" :href="skill.officialUrl" target="_blank" rel="noopener">官网</a>
        </div>
      </header>

      <!-- 详情 / 评论 -->
      <div class="tabs">
        <span :class="{ active: tab === 'detail' }" @click="tab = 'detail'">详情</span>
        <span :class="{ active: tab === 'comments' }" @click="tab = 'comments'">
          评论 <small>{{ skill.comments.length }}</small>
        </span>
      </div>

      <div v-if="tab === 'detail'" class="detail-layout">
        <article class="detail-main">
          <section class="detail-section">
            <h2>🧩 功能特点</h2>
            <ul class="feature-list">
              <li v-for="feature in skill.features" :key="feature">
                <i class="tick">✓</i><span>{{ feature }}</span>
              </li>
            </ul>
          </section>

          <section class="detail-section">
            <h2>📖 为什么收录</h2>
            <p class="detail-text">{{ skill.whyIncluded }}</p>
          </section>

          <section class="detail-section">
            <h2>🚀 快速上手</h2>
            <!-- 未解锁：只给解锁引导，步骤不下发（数据层已经置空） -->
            <div v-if="skill.locked" class="locked-banner">
              <strong>这个 Skill 需要会员解锁</strong>
              <p>{{ skill.lockReason }}</p>
              <el-button class="button button-dark" @click="$router.push('/plans')">
                查看订阅方案 <span>↗</span>
              </el-button>
            </div>
            <ol v-else class="step-list">
              <li v-for="(step, index) in skill.quickStart" :key="step.title">
                <span class="step-index">{{ index + 1 }}</span>
                <div>
                  <strong>{{ step.title }}</strong>
                  <p v-if="step.detail">{{ step.detail }}</p>
                </div>
              </li>
            </ol>
          </section>

          <section class="detail-section">
            <h2>💻 安装</h2>
            <!--
              遮罩只是交互提示，不是安全边界：和内容库一样，真正的权限必须由
              服务端判断。接入后端时，锁住就不该把安装命令下发到前端。
            -->
            <div class="code-block" :class="{ 'is-locked': skill.locked }">
              <code>{{ skill.locked ? maskedCommand : skill.installCommand }}</code>
              <button v-if="!skill.locked" class="copy-button" type="button" @click="copyCommand">
                复制
              </button>
              <span v-else class="code-lock">🔒 会员解锁后可见</span>
            </div>
          </section>
        </article>

        <!-- 右侧信息栏 -->
        <aside class="detail-aside">
          <div class="side-card">
            <h3>基本信息</h3>
            <dl>
              <div><dt>作者</dt><dd>{{ skill.author }}</dd></div>
              <div><dt>许可证</dt><dd>{{ skill.license }}</dd></div>
              <div><dt>版本</dt><dd>{{ skill.version }}</dd></div>
              <div><dt>大小</dt><dd>{{ skill.size }}</dd></div>
              <div><dt>更新时间</dt><dd>{{ skill.updatedAt }}</dd></div>
            </dl>
          </div>

          <div class="side-card">
            <h3>提交信息</h3>
            <dl>
              <div><dt>提交人</dt><dd>{{ skill.submitter }}</dd></div>
              <div><dt>提交时间</dt><dd>{{ skill.submitTime }}</dd></div>
            </dl>
          </div>

          <div class="side-card">
            <h3>安全评级</h3>
            <div class="security-badge">
              <span class="shield">🛡</span>
              <div>
                <strong>{{ skill.securityLabel }}</strong>
                <small>{{ securityDots }}</small>
              </div>
            </div>
          </div>

          <div class="side-card">
            <h3>兼容平台</h3>
            <div class="tag-row">
              <span v-for="p in skill.platforms" :key="p" class="tag is-plain">{{ p }}</span>
            </div>
          </div>

          <div class="side-card">
            <h3>标签</h3>
            <div class="tag-row">
              <span v-for="t in skill.tags" :key="t" class="tag">{{ t }}</span>
            </div>
          </div>

          <div class="side-card">
            <h3>团队协作</h3>
            <dl>
              <div><dt>维护者</dt><dd>{{ skill.team.maintainers }}</dd></div>
              <div><dt>贡献者</dt><dd>{{ skill.team.contributors }}</dd></div>
              <div><dt>未解决 Issue</dt><dd>{{ skill.team.openIssues }}</dd></div>
              <div><dt>最近提交</dt><dd>{{ skill.team.lastCommit }}</dd></div>
            </dl>
          </div>
        </aside>
      </div>

      <!-- 评论 -->
      <div v-else class="comments-pane">
        <div v-if="!skill.comments.length" class="empty-state">还没有评论。</div>
        <ul v-else class="comment-list">
          <li v-for="c in skill.comments" :key="c.id">
            <span class="comment-avatar">{{ c.user.slice(0, 1) }}</span>
            <div class="comment-body">
              <div class="comment-head">
                <strong>{{ c.user }}</strong>
                <span>{{ c.time }}</span>
              </div>
              <p>{{ c.body }}</p>
            </div>
          </li>
        </ul>
        <p class="aside-note">
          评论目前是 mock 数据，等 Skill 有了后端接口再接入真实的发表与删除。
        </p>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { formatStars, getSkill } from '../../api/skill'
import { useMembershipStore } from '@/stores/membership'
import { useUserStore } from '@/stores/user'
import type { SkillDetail } from '../../api/types'

const route = useRoute()
const userStore = useUserStore()
const membership = useMembershipStore()

const loading = ref(true)
const error = ref('')
const tab = ref<'detail' | 'comments'>('detail')

const EMPTY_SKILL: SkillDetail = {
  id: '',
  name: '',
  icon: '🧩',
  categoryId: '',
  summary: '',
  author: '',
  repo: '',
  stars: 0,
  version: '',
  accessType: 'FREE',
  platforms: [],
  tags: [],
  updatedAt: '',
  license: '',
  size: '',
  downloads: 0,
  securityLevel: 0,
  securityLabel: '',
  submitter: '',
  submitTime: '',
  officialUrl: '',
  installCommand: '',
  features: [],
  whyIncluded: '',
  quickStart: [],
  team: { maintainers: 0, contributors: 0, openIssues: 0, lastCommit: '' },
  comments: [],
}

const skill = ref<SkillDetail>(EMPTY_SKILL)
const name = computed(() => skill.value.name)

const maskedCommand = 'git clone https://github.com/••••••/••••••.git'
const securityDots = computed(() =>
  '●'.repeat(skill.value.securityLevel) + '○'.repeat(Math.max(0, 5 - skill.value.securityLevel))
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    // 强制刷新一次会员状态：用户可能刚从订阅页回来
    await membership.ensureLoaded(true)
    const isMember = userStore.isLoggedIn && membership.isMember
    skill.value = await getSkill(String(route.params.id), isMember)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Skill 加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

async function handleInstall() {
  if (skill.value.locked) {
    return
  }
  try {
    await navigator.clipboard.writeText(skill.value.installCommand)
    ElMessage.success('安装命令已复制到剪贴板')
  } catch {
    // 非 HTTPS 或浏览器不给剪贴板权限时，退化成提示，不假装成功
    ElMessage.warning('浏览器未授予剪贴板权限，请手动复制下方命令')
  }
}

async function copyCommand() {
  try {
    await navigator.clipboard.writeText(skill.value.installCommand)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('浏览器未授予剪贴板权限，请手动选中复制')
  }
}

onMounted(load)
</script>

<style scoped>
.skill-detail {
  padding: 34px 0 20px;
}
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  margin-bottom: 26px;
}
.crumbs a {
  color: var(--muted);
  text-decoration: none;
}
.crumbs a:hover {
  color: var(--ink);
}
.crumb-current {
  color: var(--ink);
}

/* ---------- 头部 ---------- */
.skill-hero {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 28px;
}
.hero-icon {
  width: 72px;
  height: 72px;
  flex: 0 0 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  background: #f3e7cf;
}
.hero-main {
  flex: 1;
  min-width: 0;
}
.hero-main h1 {
  font-size: clamp(30px, 3.4vw, 44px);
  line-height: 1.05;
  letter-spacing: -0.06em;
  margin: 0;
}
.hero-summary {
  max-width: 620px;
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.85;
  color: var(--muted);
}
.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}
.chip {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  border: 1px solid var(--line);
  padding: 4px 9px;
}
.chip.is-free {
  color: #4f6b28;
  border-color: #b6c98a;
  background: rgba(201, 212, 99, 0.22);
}
.chip.is-member {
  color: #a8481f;
  border-color: #e8b394;
  background: rgba(241, 125, 71, 0.12);
}
.hero-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 0 0 auto;
}
.install-button {
  background: #2f7d78 !important;
  border: 0 !important;
  border-radius: 0 !important;
  color: #fff !important;
  padding: 13px 26px !important;
  font: 500 12px 'DM Mono', monospace !important;
}
.install-button.is-disabled,
.install-button[disabled] {
  background: var(--line) !important;
  color: var(--muted) !important;
}
.ghost-button {
  text-align: center;
  border: 1px solid var(--line);
  padding: 10px 26px;
  font: 500 11px 'DM Mono', monospace;
  color: var(--muted);
  text-decoration: none;
}
.ghost-button:hover {
  color: var(--ink);
  border-color: var(--ink);
}

/* ---------- Tabs ---------- */
.tabs {
  display: flex;
  gap: 30px;
  border-bottom: 1px solid var(--line);
  margin-top: 10px;
}
.tabs span {
  position: relative;
  padding: 16px 0;
  font-size: 14px;
  color: var(--muted);
  cursor: pointer;
  user-select: none;
}
.tabs span.active {
  color: var(--ink);
  font-weight: 600;
}
.tabs span.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--ink);
}
.tabs small {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
}

/* ---------- 布局 ---------- */
.detail-layout {
  display: grid;
  grid-template-columns: 1fr 290px;
  gap: 56px;
  padding-top: 34px;
  align-items: start;
}
.detail-section {
  margin-bottom: 42px;
}
.detail-section h2 {
  font-size: 19px;
  letter-spacing: -0.04em;
  margin: 0 0 18px;
}
.detail-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.95;
  color: #4a4842;
  max-width: 640px;
}

/* 功能特点 */
.feature-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px 28px;
}
.feature-list li {
  display: flex;
  gap: 10px;
  font-size: 13.5px;
  line-height: 1.7;
  color: #4a4842;
}
.feature-list .tick {
  color: #4f8a5b;
  font-style: normal;
  flex: 0 0 auto;
}

/* 快速上手 */
.step-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.step-list li {
  display: flex;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
}
.step-list li:last-child {
  border-bottom: 0;
}
.step-index {
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font: 10px 'DM Mono', monospace;
  background: var(--ink);
  color: #fff;
  margin-top: 2px;
}
.step-list strong {
  font-size: 14px;
}
.step-list p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--muted);
}

/* 安装 */
.code-block {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  background: #eeebe4;
  border: 1px solid var(--line);
  padding: 15px 18px;
}
.code-block code {
  flex: 1;
  font: 12px 'DM Mono', monospace;
  word-break: break-all;
  color: var(--ink);
}
.code-block.is-locked code {
  color: var(--muted);
  user-select: none;
}
.copy-button {
  flex: 0 0 auto;
  border: 0;
  background: var(--ink);
  color: #fff;
  font: 10px 'DM Mono', monospace;
  padding: 7px 14px;
  cursor: pointer;
}
.copy-button:hover {
  background: #000;
}
.code-lock {
  flex: 0 0 auto;
  font: 10px 'DM Mono', monospace;
  color: #a8481f;
}

/* 锁定提示 */
.locked-banner {
  border-left: 3px solid var(--orange);
  background: rgba(241, 125, 71, 0.08);
  padding: 18px 22px;
}
.locked-banner strong {
  display: block;
  margin-bottom: 6px;
}
.locked-banner p {
  margin: 0 0 16px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.8;
}

/* ---------- 右侧信息栏 ---------- */
.detail-aside {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.side-card {
  border: 1px solid var(--line);
  padding: 18px;
}
.side-card h3 {
  font-size: 14px;
  margin: 0 0 14px;
  letter-spacing: -0.02em;
}
.side-card dl {
  margin: 0;
}
.side-card dl > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  font-size: 12px;
}
.side-card dt {
  color: var(--muted);
  flex: 0 0 auto;
}
.side-card dd {
  margin: 0;
  text-align: right;
  word-break: break-all;
}
.security-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(201, 212, 99, 0.2);
  border: 1px solid #b6c98a;
  padding: 12px 14px;
}
.security-badge .shield {
  font-size: 18px;
}
.security-badge strong {
  display: block;
  font-size: 13px;
  color: #4f6b28;
}
.security-badge small {
  font: 10px 'DM Mono', monospace;
  color: #6f8a44;
  letter-spacing: 2px;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  border: 1px solid var(--line);
  padding: 3px 8px;
}
.tag.is-plain {
  background: #eeebe4;
  color: var(--ink);
}

/* ---------- 评论 ---------- */
.comments-pane {
  padding-top: 30px;
  max-width: 720px;
}
.comment-list {
  list-style: none;
  margin: 0 0 20px;
  padding: 0;
}
.comment-list li {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
}
.comment-avatar {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--ink);
  color: #fff;
  font: 12px 'DM Mono', monospace;
}
.comment-body {
  flex: 1;
}
.comment-head {
  display: flex;
  justify-content: space-between;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  margin-bottom: 8px;
}
.comment-head strong {
  color: var(--ink);
  font-size: 12px;
}
.comment-body p {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
    gap: 34px;
  }
  .skill-hero {
    flex-wrap: wrap;
  }
}
</style>
