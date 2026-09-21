<template>
  <div class="creator-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">CREATOR STUDIO / PROFILE</p><h2>创作者资料<br><em>{{ form.displayName || '未命名' }}</em></h2></div>
      <p class="heading-aside">
        公开显示在内容页与订阅页。<br>
        已发布 <strong>{{ profile?.publishedCount ?? 0 }}</strong> 篇，共 <strong>{{ profile?.contentCount ?? 0 }}</strong> 篇内容。
      </p>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" class="creator-form">
      <el-form-item label="创作者展示名" prop="displayName">
        <el-input v-model="form.displayName" maxlength="60" show-word-limit />
      </el-form-item>
      <el-form-item label="创作者介绍">
        <el-input v-model="form.intro" type="textarea" :rows="4" maxlength="1000" show-word-limit
                  placeholder="介绍一下你会分享什么内容" />
      </el-form-item>
      <div class="form-actions">
        <el-button class="button button-dark" :loading="saving" @click="submit">保存 <span>↗</span></el-button>
        <el-button @click="$router.push('/creator')">返回工作台</el-button>
        <span v-if="error" class="error-text">{{ error }}</span>
        <span v-if="success" class="success-text">{{ success }}</span>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getMyCreatorProfile, updateCreatorProfile } from '@/api/creator'
import type { CreatorProfile } from '@/api/types'

const formRef = ref<FormInstance>()
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const success = ref('')
const profile = ref<CreatorProfile | null>(null)

const form = reactive({ displayName: '', intro: '' })

const rules: FormRules = {
  displayName: [{ required: true, message: '请输入创作者展示名', trigger: 'blur' }],
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await updateCreatorProfile({ displayName: form.displayName, intro: form.intro })
    if (res.data.success) {
      profile.value = res.data.data
      success.value = '已保存'
      setTimeout(() => (success.value = ''), 2500)
    } else {
      error.value = res.data.message || '保存失败'
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    error.value = err.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getMyCreatorProfile()
    if (res.data.success) {
      profile.value = res.data.data
      form.displayName = res.data.data.displayName
      form.intro = res.data.data.intro ?? ''
    } else {
      error.value = res.data.message || '加载失败'
    }
  } catch {
    error.value = '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.creator-page { padding: 60px 0 30px; }
.creator-form { margin-top: 30px; max-width: 720px; }
.form-actions { display: flex; align-items: center; gap: 14px; margin-top: 10px; }
.success-text { color: #68863d; font-size: 12px; }
</style>
