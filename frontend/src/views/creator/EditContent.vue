<template>
  <div class="edit-page content-width">
    <div class="section-heading">
      <div>
        <p class="eyebrow">CREATOR STUDIO / {{ isEdit ? 'EDIT' : 'NEW' }}</p>
        <h2>{{ isEdit ? '编辑内容' : '发布新内容' }}<br><em>{{ isEdit ? form.title || '未命名' : '写点什么' }}</em></h2>
      </div>
      <p class="heading-aside">
        保存后为草稿，需在「我的内容」里提交审核。<br>
        管理员通过后才会出现在内容库。
      </p>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" class="edit-form">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="一句话说清这份内容是什么" maxlength="200" show-word-limit />
      </el-form-item>

      <div class="form-row">
        <el-form-item label="内容类型" prop="contentType">
          <el-select v-model="form.contentType" placeholder="选择类型">
            <el-option v-for="t in contentTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" clearable>
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问方式">
          <el-select v-model="form.accessType">
            <el-option label="免费阅读" value="FREE" />
            <el-option label="订阅后阅读" value="SUBSCRIBED" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前状态">
          <!-- 编辑不改状态：状态只能通过「提交审核 / 下架 / 管理员审核」流转，
               否则编辑一篇已发布文章会把它悄悄降级成草稿 -->
          <span class="status-readonly">{{ statusLabel(currentStatus) }}</span>
        </el-form-item>
      </div>

      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit
                  placeholder="列表页展示的简介" />
      </el-form-item>

      <el-form-item label="正文">
        <el-input v-model="form.body" type="textarea" :rows="12" placeholder="支持 Markdown" />
      </el-form-item>

      <el-form-item label="附件地址（PDF / 视频 / 数据集等）">
        <el-input v-model="form.fileUrl" placeholder="可留空" />
      </el-form-item>

      <div class="form-actions">
        <el-button class="button button-dark" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建内容' }} <span>↗</span>
        </el-button>
        <el-button @click="$router.push('/creator')">返回工作台</el-button>
        <span v-if="error" class="error-text">{{ error }}</span>
        <span v-if="success" class="success-text">{{ success }}</span>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { createContent, getMyContent, updateContent } from '@/api/content'
import { listCategories } from '@/api/category'
import type { Category, ContentPayload, ContentStatus } from '@/api/types'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const success = ref('')
const categories = ref<Category[]>([])

const contentId = computed(() => {
  const raw = route.params.id
  return raw ? Number(raw) : null
})
const isEdit = computed(() => contentId.value !== null)

const contentTypes = [
  { label: '技术文章', value: 'ARTICLE' },
  { label: '系列教程', value: 'TUTORIAL' },
  { label: '电子书', value: 'EBOOK' },
  { label: '视频课程', value: 'VIDEO' },
  { label: 'PDF', value: 'PDF' },
  { label: '数据集', value: 'DATASET' },
]

const form = reactive<ContentPayload>({
  title: '',
  summary: '',
  cover: '',
  contentType: 'ARTICLE',
  categoryId: null,
  body: '',
  fileUrl: '',
  accessType: 'FREE',
})

const currentStatus = ref<ContentStatus>('DRAFT')
const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', OFFLINE: '已下架',
}
function statusLabel(s: string) {
  return STATUS_LABELS[s] ?? s
}

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const res = isEdit.value && contentId.value !== null
      ? await updateContent(contentId.value, form)
      : await createContent(form)

    if (!res.data.success) {
      error.value = res.data.message || '保存失败'
      return
    }
    success.value = isEdit.value ? '已保存' : '已创建'
    router.push('/creator')
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    error.value = err.response?.data?.message || '保存失败，请稍后重试。'
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const res = await listCategories()
    if (res.data.success) categories.value = res.data.data
  } catch {
    categories.value = []
  }

  if (isEdit.value && contentId.value !== null) {
    try {
      // 用 /contents/mine/{id}：草稿也能回显（公开详情接口只看已发布）
      const res = await getMyContent(contentId.value)
      if (res.data.success) {
        const data = res.data.data
        Object.assign(form, {
          title: data.title,
          summary: data.summary ?? '',
          cover: data.cover ?? '',
          contentType: data.contentType,
          categoryId: data.categoryId ?? null,
          body: data.body ?? '',
          fileUrl: data.fileUrl ?? '',
          accessType: data.accessType,
        })
        currentStatus.value = data.status
      } else {
        error.value = res.data.message || '内容不存在'
      }
    } catch {
      error.value = '无法加载该内容（可能不是你的内容）'
    }
  }
  loading.value = false
})
</script>

<style scoped>
.edit-page {
  padding: 60px 0 30px;
}
.edit-form {
  margin-top: 30px;
  max-width: 900px;
}
.form-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
.form-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
}
.success-text {
  color: #68863d;
  font-size: 12px;
}
.status-readonly {
  font: 11px 'DM Mono', monospace;
  padding: 3px 9px;
  border: 1px solid var(--line);
  color: var(--muted);
}
@media (max-width: 800px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
