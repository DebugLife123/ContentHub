<template>
  <div class="sub-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">SUBSCRIPTION PLANS</p><h2>订阅方案<br><em>解锁创作者的全部内容</em></h2></div>
      <p class="heading-aside">
        订阅后即可阅读该创作者的订阅专属内容。<br>
        到期后会自动重新锁定。
      </p>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="loadError" class="empty-state" role="alert">
      <p>{{ loadError }}</p>
      <el-button @click="load">重试</el-button>
    </div>
    <div v-else-if="!plans.length" class="empty-state">暂时还没有上架的订阅方案。</div>
    <div v-else class="plan-grid">
      <article v-for="plan in plans" :key="plan.id" class="plan-card">
        <p class="eyebrow">BY {{ plan.creatorName || ('CREATOR #' + plan.creatorId) }}</p>
        <h3>{{ plan.name }}</h3>
        <p class="plan-price"><em>¥{{ Number(plan.price).toFixed(2) }}</em> / {{ plan.durationDays }} 天</p>
        <p class="plan-desc">{{ plan.description || '订阅后可阅读该创作者的全部订阅内容。' }}</p>
        <p class="plan-meta">{{ plan.subscriberCount ?? 0 }} 人已订阅</p>
        <el-button
          class="button button-dark full-button"
          :loading="payingId === plan.id"
          :disabled="payingId !== null"
          @click="buy(plan)"
        >
          {{ isLoggedIn ? '立即订阅' : '登录后订阅' }} <span>↗</span>
        </el-button>
      </article>
    </div>

    <p v-if="message" class="result-text">{{ message }}</p>

    <div class="sub-note">
      <p class="eyebrow">HOW IT WORKS</p>
      <p>
        本项目第一阶段不接真实支付渠道，「立即订阅」即视为<strong>模拟支付成功</strong>，
        后端会在事务里创建订阅记录并计算起止时间；对同一创作者的重复购买会自动<strong>续期</strong>而不是产生两条并行订阅。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listPlans } from '@/api/plan'
import { newIdempotencyKey, payMock } from '@/api/subscription'
import { useUserStore } from '@/stores/user'
import type { SubscriptionPlan } from '@/api/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const loadError = ref('')
const payingId = ref<number | null>(null)
const paymentKey = ref<{ planId: number; key: string } | null>(null)
const plans = ref<SubscriptionPlan[]>([])
const message = ref('')
const requestedCreatorId = computed(() => {
  const value = Number(route.query.creatorId)
  return Number.isFinite(value) && value > 0 ? value : null
})

const isLoggedIn = computed(() => userStore.isLoggedIn)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await listPlans()
    if (!res.data.success) throw new Error(res.data.message || '订阅方案加载失败')
    plans.value = requestedCreatorId.value
      ? res.data.data.filter((plan) => plan.creatorId === requestedCreatorId.value)
      : res.data.data
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : '订阅方案加载失败，请重试'
  } finally {
    loading.value = false
  }
}

async function buy(plan: SubscriptionPlan) {
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (payingId.value !== null) return
  payingId.value = plan.id
  message.value = ''
  try {
    if (paymentKey.value?.planId !== plan.id) {
      paymentKey.value = { planId: plan.id, key: newIdempotencyKey() }
    }
    const res = await payMock(plan.id, paymentKey.value.key)
    if (res.data.success) {
      paymentKey.value = null
      const sub = res.data.data
      message.value = `模拟支付成功：${sub.planName}，有效期至 ${sub.endTime}（剩余 ${sub.remainingDays} 天）`
      ElMessage.success('订阅成功')
      const returnTo = route.query.returnTo
      if (typeof returnTo === 'string' && /^\/content\/\d+(?:[?#].*)?$/.test(returnTo)) {
        await router.push(returnTo)
      } else {
        await load()
      }
    } else {
      ElMessage.error(res.data.message || '订阅失败')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '订阅失败')
  } finally {
    payingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.sub-page { padding: 60px 0 40px; }
.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-top: 34px;
}
.plan-card {
  border: 1px solid var(--line);
  padding: 26px;
  display: flex;
  flex-direction: column;
}
.plan-card h3 { font-size: 24px; letter-spacing: -0.05em; margin: 0 0 12px; }
.plan-price { margin: 0 0 14px; }
.plan-price em { font-size: 34px; font-style: normal; font-weight: 700; letter-spacing: -0.05em; }
.plan-desc { color: var(--muted); font-size: 13px; line-height: 1.7; flex: 1; }
.plan-meta { font: 10px 'DM Mono', monospace; color: var(--muted); margin: 14px 0 16px; }
.result-text {
  margin-top: 24px;
  padding: 14px 18px;
  border-left: 3px solid #68863d;
  background: rgba(104, 134, 61, 0.08);
  font-size: 13px;
}
.sub-note {
  margin-top: 46px;
  border-top: 1px solid var(--line);
  padding-top: 26px;
  max-width: 720px;
}
.sub-note p { color: var(--muted); font-size: 13px; line-height: 1.9; }
</style>
