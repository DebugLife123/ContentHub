<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / SKILL"
      :title="isEdit ? '编辑 Skill' : '新增 Skill'"
      :accent="isEdit ? '修改后前台会同步' : '新建后默认为草稿'"
      :description="isEdit ? '当前状态：' + statusLabel + '。上架 / 下架请在列表页操作。' : '保存后状态为「草稿」，需在列表页上架才会出现在前台。'"
    >
      <template #actions>
        <RouterLink to="/admin/skills" class="back-link">← 返回 Skill 商城管理</RouterLink>
      </template>
    </AdminPageHeader>

    <p v-if="loading" class="admin-loading">LOADING…</p>

    <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" class="skill-form">
      <h3 class="form-section">基本信息</h3>
      <div class="form-grid">
        <el-form-item label="名称" prop="name" class="span-2">
          <el-input v-model="form.name" maxlength="100" placeholder="如：brainstorming" />
        </el-form-item>
        <el-form-item label="图标（emoji）">
          <el-input v-model="form.icon" maxlength="16" placeholder="如：💡" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" clearable class="full-width">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问类型">
          <el-radio-group v-model="form.accessType">
            <el-radio value="FREE">免费</el-radio>
            <el-radio value="MEMBER">会员解锁</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="form.version" maxlength="50" placeholder="如：v6.2.0" />
        </el-form-item>
        <el-form-item label="简介" prop="summary" class="span-2">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit
                    placeholder="一句话说清这个 Skill 做什么" />
        </el-form-item>
        <el-form-item label="为什么收录" class="span-2">
          <el-input v-model="form.whyIncluded" type="textarea" :rows="2" maxlength="1000" show-word-limit
                    placeholder="详情页「为什么收录」一节的内容" />
        </el-form-item>
      </div>

      <h3 class="form-section">上游信息</h3>
      <div class="form-grid">
        <el-form-item label="作者">
          <el-input v-model="form.author" maxlength="100" placeholder="如：obra" />
        </el-form-item>
        <el-form-item label="仓库（owner/name）">
          <el-input v-model="form.repo" maxlength="200" placeholder="如：obra/superpowers" />
        </el-form-item>
        <el-form-item label="官网地址" class="span-2">
          <el-input v-model="form.officialUrl" maxlength="500" placeholder="https://github.com/..." />
        </el-form-item>
        <el-form-item label="安装命令" class="span-2">
          <el-input v-model="form.installCommand" maxlength="500" placeholder="git clone https://github.com/..." />
        </el-form-item>
        <el-form-item label="星数（列表排序依据）">
          <el-input-number v-model="form.stars" :min="0" :max="9999999" />
        </el-form-item>
        <el-form-item label="下载 / 安装次数">
          <el-input-number v-model="form.downloads" :min="0" :max="9999999" />
        </el-form-item>
        <el-form-item label="许可证">
          <el-input v-model="form.license" maxlength="50" placeholder="如：MIT" />
        </el-form-item>
        <el-form-item label="体积">
          <el-input v-model="form.size" maxlength="50" placeholder="如：4.0 MB" />
        </el-form-item>
        <el-form-item label="安全评级（0-5）">
          <el-input-number v-model="form.securityLevel" :min="0" :max="5" />
        </el-form-item>
        <el-form-item label="安全评级文案">
          <el-input v-model="form.securityLabel" maxlength="50" placeholder="如：4 级安全认证" />
        </el-form-item>
        <el-form-item label="提交人">
          <el-input v-model="form.submitter" maxlength="100" placeholder="详情页「提交信息」展示用" />
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker v-model="form.submitTime" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择日期" class="full-width" />
        </el-form-item>
      </div>

      <h3 class="form-section">详情页内容</h3>
      <el-form-item label="功能特点（一行一条）">
        <el-input v-model="form.featuresText" type="textarea" :rows="4"
                  placeholder="在进行任何创造性工作之前必须使用此技能&#10;提供可复用的 Skill 能力" />
      </el-form-item>

      <el-form-item label="快速上手步骤">
        <div class="step-editor">
          <div v-for="(step, index) in form.quickStart" :key="index" class="step-row">
            <span class="step-index">{{ index + 1 }}</span>
            <el-input v-model="step.title" placeholder="步骤标题" maxlength="100" />
            <el-input v-model="step.detail" placeholder="补充说明（可空）" maxlength="300" />
            <a class="step-remove" @click.prevent="form.quickStart.splice(index, 1)">删除</a>
          </div>
          <el-button size="small" @click="form.quickStart.push({ title: '', detail: '' })">添加一步</el-button>
        </div>
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="兼容平台">
          <el-select v-model="form.platforms" multiple filterable allow-create default-first-option
                     placeholder="输入后回车添加" class="full-width" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple filterable allow-create default-first-option
                     placeholder="输入后回车添加" class="full-width" />
        </el-form-item>
      </div>

      <h3 class="form-section">团队协作</h3>
      <div class="form-grid">
        <el-form-item label="维护者数量">
          <el-input-number v-model="form.teamMaintainers" :min="0" :max="99999" />
        </el-form-item>
        <el-form-item label="贡献者数量">
          <el-input-number v-model="form.teamContributors" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="未解决 Issue">
          <el-input-number v-model="form.teamOpenIssues" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="最近提交日期">
          <el-date-picker v-model="form.teamLastCommit" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择日期" class="full-width" />
        </el-form-item>
      </div>

      <div class="form-actions">
        <el-button class="button button-dark" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建 Skill' }} <span>↗</span>
        </el-button>
        <el-button @click="$router.push('/admin/skills')">返回列表</el-button>
        <span v-if="message" class="admin-flash" :class="messageType === 'error' ? 'is-error' : 'is-ok'">{{ message }}</span>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createSkill,
  getSkillForAdmin,
  listAllSkillCategories,
  updateSkill,
} from '@/api/skill'
import type { SkillCategory, SkillPayload, SkillStatus } from '@/api/types'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const route = useRoute()
const router = useRouter()

const contentId = computed(() => {
  const raw = route.params.id
  return raw === undefined ? null : Number(raw)
})
const isEdit = computed(() => contentId.value !== null)

const STATUS_LABEL: Record<SkillStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已上架',
  OFFLINE: '已下架',
}

const loading = ref(true)
const saving = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const status = ref<SkillStatus>('DRAFT')
const statusLabel = computed(() => STATUS_LABEL[status.value] ?? status.value)
const categories = ref<SkillCategory[]>([])
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  icon: '',
  categoryId: null as number | null,
  summary: '',
  whyIncluded: '',
  author: '',
  repo: '',
  officialUrl: '',
  installCommand: '',
  stars: 0,
  version: '',
  license: '',
  size: '',
  downloads: 0,
  securityLevel: 0,
  securityLabel: '',
  submitter: '',
  submitTime: '',
  accessType: 'FREE' as 'FREE' | 'MEMBER',
  featuresText: '',
  quickStart: [] as { title: string; detail: string }[],
  platforms: [] as string[],
  tags: [] as string[],
  teamMaintainers: 0,
  teamContributors: 0,
  teamOpenIssues: 0,
  teamLastCommit: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  summary: [{ required: true, message: '请输入简介', trigger: 'blur' }],
}

function flash(text: string, type: 'success' | 'error' = 'success') {
  message.value = text
  messageType.value = type
  setTimeout(() => (message.value = ''), 3000)
}

function errText(e: unknown, fallback: string) {
  const err = e as { message?: string; response?: { data?: { message?: string } } }
  return err.response?.data?.message || err.message || fallback
}

/** 空字符串要转成 null：后端 submitTime / teamLastCommit 是 LocalDate，收到 '' 会解析失败 */
function orNull(v: string) {
  return v && v.trim() ? v.trim() : null
}

function buildPayload(): SkillPayload {
  return {
    name: form.name.trim(),
    icon: orNull(form.icon),
    categoryId: form.categoryId,
    summary: orNull(form.summary),
    whyIncluded: orNull(form.whyIncluded),
    author: orNull(form.author),
    repo: orNull(form.repo),
    officialUrl: orNull(form.officialUrl),
    installCommand: orNull(form.installCommand),
    stars: form.stars,
    version: orNull(form.version),
    license: orNull(form.license),
    size: orNull(form.size),
    downloads: form.downloads,
    securityLevel: form.securityLevel,
    securityLabel: orNull(form.securityLabel),
    submitter: orNull(form.submitter),
    submitTime: orNull(form.submitTime),
    platforms: form.platforms,
    tags: form.tags,
    features: form.featuresText
      .split(/\r?\n/)
      .map((s) => s.trim())
      .filter(Boolean),
    quickStart: form.quickStart
      .filter((s) => s.title.trim())
      .map((s) => ({ title: s.title.trim(), detail: orNull(s.detail) })),
    teamMaintainers: form.teamMaintainers,
    teamContributors: form.teamContributors,
    teamOpenIssues: form.teamOpenIssues,
    teamLastCommit: orNull(form.teamLastCommit),
    accessType: form.accessType,
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = buildPayload()
    const res = isEdit.value && contentId.value !== null
      ? await updateSkill(contentId.value, payload)
      : await createSkill(payload)

    if (res.data.success) {
      router.push('/admin/skills')
    } else {
      flash(res.data.message || '保存失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '保存失败'), 'error')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const res = await listAllSkillCategories()
    if (res.data.success) categories.value = res.data.data
  } catch {
    categories.value = []
  }

  if (isEdit.value && contentId.value !== null) {
    try {
      const res = await getSkillForAdmin(contentId.value)
      if (!res.data.success) throw new Error(res.data.message || 'Skill 不存在')
      const s = res.data.data
      status.value = s.status
      form.name = s.name
      form.icon = s.icon || ''
      form.categoryId = s.categoryId ?? null
      form.summary = s.summary || ''
      form.whyIncluded = s.whyIncluded || ''
      form.author = s.author || ''
      form.repo = s.repo || ''
      form.officialUrl = s.officialUrl || ''
      form.installCommand = s.installCommand || ''
      form.stars = s.stars
      form.version = s.version || ''
      form.license = s.license || ''
      form.size = s.size || ''
      form.downloads = s.downloads
      form.securityLevel = s.securityLevel
      form.securityLabel = s.securityLabel || ''
      form.submitter = s.submitter || ''
      form.submitTime = s.submitTime || ''
      form.accessType = s.accessType
      form.featuresText = s.features.join('\n')
      form.quickStart = s.quickStart.map((q) => ({ title: q.title, detail: q.detail || '' }))
      form.platforms = [...s.platforms]
      form.tags = [...s.tags]
      form.teamMaintainers = s.team.maintainers ?? 0
      form.teamContributors = s.team.contributors ?? 0
      form.teamOpenIssues = s.team.openIssues ?? 0
      form.teamLastCommit = s.team.lastCommit || ''
    } catch (e) {
      flash(errText(e, 'Skill 加载失败'), 'error')
    }
  }

  loading.value = false
})
</script>

<style scoped>
/* 通用样式（页头 / 加载态 / 提示）统一在 styles/admin-system.scss，
   这里只保留本页特有的返回链接与表单布局。 */
.back-link {
  font: 11px 'DM Mono', monospace;
  color: var(--muted);
  text-decoration: none;
}
.form-section {
  font-size: 15px;
  letter-spacing: -0.02em;
  margin: 34px 0 18px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--line);
}
.form-section:first-of-type {
  margin-top: 22px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 24px;
}
.span-2 {
  grid-column: span 2;
}
.full-width {
  width: 100%;
}
.step-editor {
  width: 100%;
}
.step-row {
  display: grid;
  grid-template-columns: 22px 1fr 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}
.step-index {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font: 10px 'DM Mono', monospace;
  background: var(--ink);
  color: #fff;
}
.step-remove {
  font: 11px 'DM Mono', monospace;
  color: #c54a32;
  cursor: pointer;
  text-decoration: underline;
  white-space: nowrap;
}
.form-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 34px;
  padding-top: 24px;
  border-top: 1px solid var(--line);
}

@media (max-width: 800px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .span-2 {
    grid-column: span 1;
  }
  .step-row {
    grid-template-columns: 22px 1fr;
  }
}
</style>
