<template>
  <section class="art-comments">
    <div class="art-comments-head">
      <h2>讨论</h2>
      <span>{{ total }} 条评论</span>
    </div>

    <div class="art-comment-form">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        :placeholder="userStore.isLoggedIn ? '写下你的看法…' : '登录后即可评论'"
      />
      <div class="art-comment-submit">
        <el-button class="button button-dark" :loading="posting" @click="submit">
          {{ userStore.isLoggedIn ? '发表评论' : '登录后评论' }} <span>↗</span>
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="art-comment-empty">正在加载评论…</div>
    <div v-else-if="loadError" class="art-comment-empty" role="alert"><p>{{ loadError }}</p><el-button @click="load">重试</el-button></div>
    <div v-else-if="!comments.length" class="art-comment-empty">还没有评论，来做第一个。</div>

    <ul v-else class="art-comment-list">
      <li v-for="comment in comments" :key="comment.id" class="art-comment">
        <span class="art-avatar">{{ initialOf(comment) }}</span>
        <div>
          <div class="art-comment-head">
            <strong>{{ comment.username || `用户 #${comment.userId}` }}</strong>
            <time>{{ comment.createTime }}</time>
          </div>
          <p class="art-comment-body">{{ comment.body }}</p>
          <div class="art-comment-foot">
            <button type="button" @click="reply(comment)">回复</button>
            <button
              v-if="comment.canDelete"
              class="is-danger"
              type="button"
              @click="remove(comment)"
            >{{ deletingId === comment.id ? '处理中…' : '删除' }}</button>
          </div>
        </div>
      </li>
    </ul>

    <div v-if="total > comments.length" class="art-comment-pager">
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :current-page="page"
        :page-size="pageSize"
        background
        @current-change="handlePage"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createComment, deleteComment, listComments } from '@/api/content'
import { useUserStore } from '@/stores/user'
import type { Comment } from '@/api/types'

const props = defineProps<{ contentId: number }>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const comments = ref<Comment[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(5)
const draft = ref('')
const posting = ref(false)
const loading = ref(false)
const loadError = ref('')
const deletingId = ref<number | null>(null)

function initialOf(comment: Comment): string {
  return (comment.username || 'U').slice(0, 1).toUpperCase()
}

async function load() {
  if (!props.contentId) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await listComments(props.contentId, page.value, pageSize.value)
    if (res.data.success) {
      comments.value = res.data.data.list
      total.value = res.data.data.total
    } else {
      loadError.value = res.data.message || '评论加载失败，请重试'
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    loadError.value = err.response?.data?.message || '评论加载失败，请重试'
  } finally {
    loading.value = false
  }
}

function handlePage(next: number) {
  page.value = next
  load()
}

async function submit() {
  if (posting.value) return
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!draft.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  posting.value = true
  try {
    const res = await createComment(props.contentId, draft.value.trim())
    if (res.data.success) {
      draft.value = ''
      page.value = 1
      await load()
      ElMessage.success('评论已发表')
    } else {
      ElMessage.error(res.data.message || '发表失败')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '发表失败')
  } finally {
    posting.value = false
  }
}

/** 回复 = 把 @对方 预填进输入框，保持轻量 */
function reply(comment: Comment) {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  draft.value = `@${comment.username || `用户 #${comment.userId}`} `
}

async function remove(comment: Comment) {
  if (deletingId.value !== null) return
  deletingId.value = comment.id
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
  } catch {
    deletingId.value = null
    return
  }
  try {
    const res = await deleteComment(comment.id)
    if (res.data.success) {
      ElMessage.success('已删除')
      await load()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '删除失败')
  } finally {
    deletingId.value = null
  }
}

onMounted(load)
</script>
