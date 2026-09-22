<template>
  <div class="profile-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">MY ACCOUNT</p><h2>个人中心<br><em>{{ displayName }}</em></h2></div>
      <p class="heading-aside">
        角色：<strong>{{ roleLabel }}</strong><br>
        {{ roleHint }}
      </p>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!userInfo" class="empty-state">登录状态已失效，请重新登录。</div>
    <template v-else>
      <div class="profile-grid">
        <!-- 账号信息 + 编辑入口 -->
        <section class="profile-panel">
          <h3>账号信息</h3>
          <dl class="info-list">
            <div><dt>用户名</dt><dd>{{ userInfo.username }}</dd></div>
            <div><dt>昵称</dt><dd>{{ userInfo.nickname || '未设置' }}</dd></div>
            <div><dt>邮箱</dt><dd>{{ userInfo.email || '未设置' }}</dd></div>
            <div><dt>简介</dt><dd>{{ userInfo.bio || '未设置' }}</dd></div>
            <div><dt>角色</dt><dd>{{ roleLabel }}</dd></div>
          </dl>
          <div class="panel-actions">
            <el-button class="button button-dark" @click="openProfileDialog">编辑资料 <span>↗</span></el-button>
            <el-button @click="openPasswordDialog">修改密码</el-button>
            <el-button @click="$router.push('/subscriptions')">我的订阅</el-button>
            <el-button @click="handleLogout">退出登录</el-button>
          </div>
          <p v-if="actionMessage" :class="messageType === 'error' ? 'error-text' : 'success-text'">
            {{ actionMessage }}
          </p>
        </section>

        <!-- 创作者身份：申请 -> 待审核 -> 通过/驳回 -->
        <section class="profile-panel">
          <h3>创作者身份</h3>

          <template v-if="isCreator">
            <p class="creator-state is-ok">✓ 你已经是创作者，可以发布内容、创建订阅套餐。</p>
            <div class="panel-actions">
              <el-button class="button button-dark" @click="$router.push('/creator')">
                进入创作者工作台 <span>↗</span>
              </el-button>
            </div>
          </template>

          <template v-else-if="application && application.status === 'PENDING'">
            <p class="creator-state is-pending">
              ⏳ 申请已提交（{{ application.createTime || '刚刚' }}），等待管理员审核。
            </p>
            <p v-if="application.intro" class="creator-intro">申请说明：{{ application.intro }}</p>
          </template>

          <template v-else>
            <p v-if="application && application.status === 'REJECTED'" class="creator-state is-rejected">
              ✗ 上次申请未通过：{{ application.rejectReason || '管理员未填写原因' }}
            </p>
            <p v-else class="creator-state">
              还不是创作者。提交申请后由管理员审核，通过即可发布内容并创建订阅套餐。
            </p>
            <div class="panel-actions">
              <el-button class="button button-dark" @click="applyDialog = true">
                {{ application && application.status === 'REJECTED' ? '重新申请' : '申请成为创作者' }} <span>↗</span>
              </el-button>
            </div>
          </template>
        </section>

        <section class="profile-panel">
          <h3>可以做什么</h3>
          <ul class="ability-list">
            <li v-for="item in abilities" :key="item.text" :class="{ disabled: !item.enabled }">
              <span>{{ item.enabled ? '✓' : '·' }}</span>
              <div><strong>{{ item.text }}</strong><small>{{ item.hint }}</small></div>
            </li>
          </ul>
          <div class="sub-summary">
            <p class="eyebrow">SUBSCRIPTION</p>
            <p v-if="activeSubCount > 0">
              当前有 <strong>{{ activeSubCount }}</strong> 个生效中的订阅，可阅读对应创作者的订阅专属内容。
            </p>
            <p v-else>还没有生效中的订阅，付费内容只能看到试读片段。</p>
          </div>
        </section>
      </div>

      <section class="favorites">
        <div class="panel-head">
          <h3>我的收藏</h3>
          <span>{{ favoriteTotal }} 份</span>
        </div>
        <div v-if="!favorites.length" class="empty-state">还没有收藏任何内容。</div>
        <div v-else class="fav-grid">
          <article v-for="item in favorites" :key="item.id" class="fav-card"
                   @click="$router.push(`/content/${item.id}`)">
            <span class="fav-type">{{ item.contentType }}</span>
            <h4>{{ item.title }}</h4>
            <p>{{ item.summary || '一份正在持续更新的数字内容。' }}</p>
            <div class="fav-foot">
              <span>{{ item.accessType === 'FREE' ? '免费' : '订阅' }}</span>
              <span>{{ item.viewCount || 0 }} 次阅读</span>
            </div>
          </article>
        </div>
      </section>

      <section class="favorites">
        <div class="panel-head">
          <h3>阅读历史</h3>
          <span>{{ historyTotal }} 篇</span>
        </div>
        <div v-if="!history.length" class="empty-state">还没有阅读记录。</div>
        <ul v-else class="history-list">
          <li v-for="h in history" :key="h.id" @click="$router.push(`/content/${h.contentId}`)">
            <div class="history-main">
              <strong>{{ h.contentTitle }}</strong>
              <small>{{ h.contentType }} · {{ h.lastReadTime }}</small>
            </div>
            <div class="progress-bar"><i :style="{ width: `${h.progress}%` }"></i></div>
            <span class="history-pct">{{ h.progress }}%</span>
          </li>
        </ul>
      </section>

      <section class="favorites">
        <div class="panel-head">
          <h3>我的评论</h3>
          <span>{{ commentTotal }} 条</span>
        </div>
        <div v-if="!comments.length" class="empty-state">还没有发表过评论。</div>
        <ul v-else class="my-comment-list">
          <li v-for="c in comments" :key="c.id">
            <div class="comment-head">
              <RouterLink :to="`/content/${c.contentId}`">{{ c.contentTitle || ('内容 #' + c.contentId) }}</RouterLink>
              <span>{{ c.createTime }}</span>
            </div>
            <p>{{ c.body }}</p>
            <a class="comment-del" @click.prevent="removeMyComment(c)">删除</a>
          </li>
        </ul>
      </section>
    </template>

    <!-- 编辑资料 -->
    <el-dialog v-model="profileDialog" title="编辑资料" width="460px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="profileForm.nickname" maxlength="50" placeholder="展示给其他人的名字" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" maxlength="100" placeholder="选填" />
        </el-form-item>
        <el-form-item label="头像地址">
          <el-input v-model="profileForm.avatar" maxlength="255" placeholder="选填，填图片 URL" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="profileForm.bio" type="textarea" :rows="3" maxlength="300" show-word-limit
                    placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialog = false">取消</el-button>
        <el-button class="button button-dark" :loading="saving" @click="submitProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码 -->
    <el-dialog v-model="passwordDialog" title="修改密码" width="420px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog = false">取消</el-button>
        <el-button class="button button-dark" :loading="saving" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>

    <!-- 申请成为创作者 -->
    <el-dialog v-model="applyDialog" title="申请成为创作者" width="460px">
      <p class="dialog-hint">
        提交后由管理员审核，通过后你就能发布内容、创建订阅套餐。写清楚你打算发布什么，能提高通过率。
      </p>
      <el-input v-model="applyIntro" type="textarea" :rows="4" maxlength="500" show-word-limit
                placeholder="例如：我打算发布前端工程与 Java 后端的技术文章，有 3 年相关经验。" />
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button class="button button-dark" :loading="applying" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { applyCreator, getMyCreatorApplication } from '@/api/creator'
import { changeMyPassword, updateMyProfile } from '@/api/user'
import { deleteComment, myComments, myFavorites, myHistory } from '@/api/content'
import { mySubscriptions } from '@/api/subscription'
import { useUserStore } from '@/stores/user'
import type { Comment, ContentItem, CreatorApplication, ReadingHistory } from '@/api/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const saving = ref(false)
const applying = ref(false)
const actionMessage = ref('')
const messageType = ref<'success' | 'error'>('success')

const favorites = ref<ContentItem[]>([])
const favoriteTotal = ref(0)
const activeSubCount = ref(0)
const history = ref<ReadingHistory[]>([])
const historyTotal = ref(0)
const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const application = ref<CreatorApplication | null>(null)

const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '访客')
const isCreator = computed(() => userStore.hasRole('CREATOR', 'ADMIN'))

const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', CREATOR: '创作者', ADMIN: '管理员' }
  return userStore.role ? map[userStore.role] ?? userStore.role : '未登录'
})

const roleHint = computed(() => {
  if (userStore.hasRole('ADMIN')) return '拥有平台全部管理权限'
  if (userStore.hasRole('CREATOR')) return '可以发布与管理自己的内容'
  return '可以浏览免费内容、收藏与订阅'
})

const abilities = computed(() => [
  { text: '浏览免费内容', enabled: true, hint: '所有访客都可以' },
  { text: '收藏内容', enabled: true, hint: '登录后即可' },
  { text: '订阅创作者', enabled: true, hint: '模拟支付，即时生效' },
  { text: '发布与编辑内容', enabled: isCreator.value, hint: '需要创作者身份（管理员审核后获得）' },
  { text: '审核内容 / 管理分类', enabled: userStore.hasRole('ADMIN'), hint: '需要管理员身份' },
])

function flash(text: string, type: 'success' | 'error' = 'success') {
  actionMessage.value = text
  messageType.value = type
  setTimeout(() => (actionMessage.value = ''), 4000)
}

function errText(e: unknown, fallback: string) {
  const err = e as { message?: string; response?: { data?: { message?: string } } }
  return err.response?.data?.message || err.message || fallback
}

// ---------------------------------------------------------------- 编辑资料

const profileDialog = ref(false)
const profileFormRef = ref<FormInstance>()
const profileForm = reactive({ nickname: '', email: '', avatar: '', bio: '' })
const profileRules: FormRules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

function openProfileDialog() {
  profileForm.nickname = userStore.userInfo?.nickname || ''
  profileForm.email = userStore.userInfo?.email || ''
  profileForm.avatar = userStore.userInfo?.avatar || ''
  profileForm.bio = userStore.userInfo?.bio || ''
  profileDialog.value = true
}

async function submitProfile() {
  if (profileFormRef.value) {
    const valid = await profileFormRef.value.validate().catch(() => false)
    if (!valid) return
  }
  saving.value = true
  try {
    const res = await updateMyProfile({ ...profileForm })
    if (res.data.success) {
      profileDialog.value = false
      await userStore.fetchCurrentUser()
      flash('资料已保存')
    } else {
      flash(res.data.message || '保存失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '保存失败'), 'error')
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 修改密码

const passwordDialog = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码长度需在 6-32 位之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

function openPasswordDialog() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialog.value = true
}

async function submitPassword() {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const res = await changeMyPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    if (res.data.success) {
      passwordDialog.value = false
      ElMessage.success('密码已修改，下次登录请使用新密码')
    } else {
      flash(res.data.message || '修改失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '修改失败'), 'error')
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 创作者申请

const applyDialog = ref(false)
const applyIntro = ref('')

async function submitApply() {
  applying.value = true
  try {
    const res = await applyCreator(applyIntro.value.trim() || undefined)
    if (res.data.success) {
      applyDialog.value = false
      applyIntro.value = ''
      application.value = res.data.data
      flash('申请已提交，等待管理员审核')
    } else {
      flash(res.data.message || '提交失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '提交失败'), 'error')
  } finally {
    applying.value = false
  }
}

// ---------------------------------------------------------------- 其它

async function removeMyComment(c: Comment) {
  const res = await deleteComment(c.id)
  if (res.data.success) {
    ElMessage.success('已删除')
    const again = await myComments(1, 10)
    if (again.data.success) {
      comments.value = again.data.data.list
      commentTotal.value = again.data.data.total
    }
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

async function handleLogout() {
  await userStore.logout()
  router.push('/')
}

onMounted(async () => {
  await userStore.fetchCurrentUser()

  // 已经是创作者就不用再问申请状态了
  if (!isCreator.value) {
    try {
      const res = await getMyCreatorApplication()
      if (res.data.success) application.value = res.data.data
    } catch { /* 忽略 */ }
  }

  try {
    const fav = await myFavorites(1, 6)
    if (fav.data.success) {
      favorites.value = fav.data.data.list
      favoriteTotal.value = fav.data.data.total
    }
  } catch { /* 忽略：收藏列表失败不影响主页 */ }
  try {
    const subs = await mySubscriptions(1, 50)
    if (subs.data.success) {
      activeSubCount.value = subs.data.data.list.filter((s) => s.valid).length
    }
  } catch { /* 忽略 */ }
  try {
    const his = await myHistory(1, 10)
    if (his.data.success) {
      history.value = his.data.data.list
      historyTotal.value = his.data.data.total
    }
  } catch { /* 忽略 */ }
  try {
    const cms = await myComments(1, 10)
    if (cms.data.success) {
      comments.value = cms.data.data.list
      commentTotal.value = cms.data.data.total
    }
  } catch { /* 忽略 */ }
  loading.value = false
})
</script>

<style scoped>
.profile-page { padding: 70px 0 30px; }
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-top: 34px; }
.profile-panel { border: 1px solid var(--line); padding: 26px; }
.profile-panel h3 { font-size: 20px; letter-spacing: -0.04em; margin: 0 0 20px; }
.info-list { margin: 0 0 22px; }
.info-list > div { display: flex; justify-content: space-between; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.info-list dt { color: var(--muted); font: 11px 'DM Mono', monospace; }
.info-list dd { margin: 0; text-align: right; word-break: break-all; }
.panel-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.creator-state { font-size: 13.5px; line-height: 1.8; color: var(--muted); margin: 0 0 18px; }
.creator-state.is-ok { color: #4f6b28; }
.creator-state.is-pending { color: #a8481f; }
.creator-state.is-rejected { color: #c54a32; }
.creator-intro {
  margin: -8px 0 18px;
  padding: 12px 14px;
  background: #eeebe4;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--muted);
}
.dialog-hint { margin: 0 0 14px; font-size: 13px; line-height: 1.7; color: var(--muted); }
.ability-list { list-style: none; margin: 0; padding: 0; }
.ability-list li { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.ability-list li.disabled { color: var(--muted); }
.ability-list li span { width: 16px; }
.ability-list small { display: block; color: var(--muted); font: 10px 'DM Mono', monospace; margin-top: 5px; }
.sub-summary { margin-top: 20px; }
.sub-summary p { color: var(--muted); font-size: 13px; line-height: 1.8; }
.favorites { margin-top: 34px; border: 1px solid var(--line); padding: 26px; }
.favorites .panel-head { display: flex; justify-content: space-between; align-items: baseline; }
.favorites h3 { font-size: 20px; letter-spacing: -0.04em; margin: 0; }
.favorites .panel-head span { font: 10px 'DM Mono', monospace; color: var(--muted); }
.fav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 18px; margin-top: 20px; }
.fav-card { border: 1px solid var(--line); padding: 18px; cursor: pointer; }
.fav-card:hover { transform: translateY(-3px); transition: transform 0.2s; }
.fav-type { font: 10px 'DM Mono', monospace; color: var(--muted); }
.fav-card h4 { margin: 10px 0 8px; font-size: 16px; letter-spacing: -0.03em; }
.fav-card p { margin: 0; color: var(--muted); font-size: 12px; line-height: 1.6; }
.fav-foot { display: flex; justify-content: space-between; margin-top: 14px; font: 10px 'DM Mono', monospace; color: var(--muted); }
.success-text { color: #68863d; font-size: 12px; margin-top: 12px; }
.history-list, .my-comment-list { list-style: none; margin: 20px 0 0; padding: 0; }
.history-list li {
  display: grid;
  grid-template-columns: 1fr 140px 44px;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
}
.history-main strong { display: block; font-size: 14px; }
.history-main small { display: block; margin-top: 5px; font: 10px 'DM Mono', monospace; color: var(--muted); }
.progress-bar { height: 4px; background: var(--line); }
.progress-bar i { display: block; height: 100%; background: var(--orange); }
.history-pct { font: 10px 'DM Mono', monospace; color: var(--muted); text-align: right; }
.my-comment-list li { position: relative; padding: 14px 0; border-bottom: 1px solid var(--line); }
.my-comment-list .comment-head { display: flex; justify-content: space-between; font: 10px 'DM Mono', monospace; color: var(--muted); }
.my-comment-list p { margin: 8px 0 0; line-height: 1.7; font-size: 14px; }
.my-comment-list .comment-del { position: absolute; right: 0; bottom: 12px; font: 10px 'DM Mono', monospace; color: #c54a32; cursor: pointer; text-decoration: underline; }
@media (max-width: 800px) {
  .profile-grid { grid-template-columns: 1fr; }
}
</style>
