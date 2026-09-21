<template>
  <div class="auth-page content-width">
    <div class="auth-intro">
      <p class="eyebrow">WELCOME BACK</p>
      <h1>继续你的<br><em>知识旅程。</em></h1>
      <p>登录 ContentHub，访问你订阅的内容，也继续支持你喜欢的创作者。</p>
    </div>

    <el-card class="auth-card" shadow="never">
      <p class="eyebrow">MEMBER LOGIN</p>
      <h2>欢迎回来</h2>
      <el-form :model="form" @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            size="large"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button class="button button-dark submit-button" size="large" native-type="submit" :loading="loading">
          进入 ContentHub <span>↗</span>
        </el-button>
      </el-form>
      <p class="auth-hint">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
      <p class="auth-hint">演示账号：creator / 123456（创作者）、admin / 123456（管理员）、user / 123456（普通用户）</p>
      <p v-if="error" class="error-text">{{ error }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const error = ref('')
const form = reactive({ username: 'creator', password: '123456' })

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await userStore.login(form.username, form.password)
    // 登录后回到原本想去的页面（被路由守卫拦下来的那个）
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' ? redirect : '/')
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
</style>
