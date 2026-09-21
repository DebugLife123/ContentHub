<template>
  <div class="auth-page content-width">
    <div class="auth-intro">
      <p class="eyebrow">JOIN CONTENTHUB</p>
      <h1>从今天起，<br><em>开始积累。</em></h1>
      <p>注册后即可浏览免费内容、收藏与评论。想发布内容可在创作者工作台申请创作者身份。</p>
    </div>

    <el-card class="auth-card" shadow="never">
      <p class="eyebrow">CREATE ACCOUNT</p>
      <h2>创建账号</h2>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（3~60 个字符）" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码（至少 6 位）" size="large" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="确认密码"
            size="large"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button class="button button-dark submit-button" size="large" native-type="submit" :loading="loading">
          注册并登录 <span>↗</span>
        </el-button>
      </el-form>
      <p class="auth-hint">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="success" class="success-text">{{ success }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const error = ref('')
const success = ref('')

const form = reactive({ username: '', password: '', confirmPassword: '' })

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 60, message: '长度需在 3~60 个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 60, message: '长度需在 6~60 个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await register(form.username, form.password, form.confirmPassword)
    if (!res.data.success) {
      error.value = res.data.message || '注册失败'
      return
    }
    success.value = '注册成功，正在为你登录…'
    // 注册后直接登录，省去用户再输一次
    await userStore.login(form.username, form.password)
    router.push('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '服务暂不可用，请确认后端已启动。'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-hint a {
  color: var(--ink);
  text-decoration: underline;
}
.success-text {
  color: #68863d;
  font-size: 12px;
}
</style>
