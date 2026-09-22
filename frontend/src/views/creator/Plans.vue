<template>
  <div class="plans-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">CREATOR STUDIO / PLANS</p><h2>订阅套餐<br><em>定价与周期</em></h2></div>
      <p class="heading-aside">
        读者购买后即可阅读你的全部订阅内容。<br>
        已有订阅记录的套餐不能删除，只能下架。
      </p>
    </div>

    <div class="toolbar">
      <el-button class="button button-dark" @click="openCreate">新增套餐 <span>↗</span></el-button>
      <span v-if="message" :class="messageType === 'error' ? 'error-text' : 'success-text'">{{ message }}</span>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <table v-else class="plans-table">
      <thead>
        <tr>
          <th>名称</th><th>价格</th><th>周期</th><th>状态</th><th>订阅人数</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td class="cell-name">{{ item.name }}<small v-if="item.description">{{ item.description }}</small></td>
          <td>¥ {{ Number(item.price).toFixed(2) }}</td>
          <td>{{ item.durationDays }} 天</td>
          <td>
            <span class="status-tag" :class="item.status === 'ACTIVE' ? 'status-on' : 'status-off'">
              {{ item.status === 'ACTIVE' ? '上架' : '下架' }}
            </span>
          </td>
          <td>{{ item.subscriberCount ?? 0 }}</td>
          <td class="cell-actions">
            <a @click.prevent="openEdit(item)">编辑</a>
            <a class="danger" @click.prevent="handleDelete(item)">删除</a>
          </td>
        </tr>
      </tbody>
    </table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑套餐' : '新增套餐'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="如：Pro 月度会员" />
        </el-form-item>
        <el-form-item label="套餐描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="价格（元）" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="有效天数" prop="durationDays">
          <el-input-number v-model="form.durationDays" :min="1" :max="3650" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">上架</el-radio>
            <el-radio value="INACTIVE">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="button button-dark" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createPlan, deletePlan, listMyPlans, updatePlan } from '@/api/plan'
import type { SubscriptionPlan } from '@/api/types'

const loading = ref(true)
const saving = ref(false)
const items = ref<SubscriptionPlan[]>([])
const dialogVisible = ref(false)
const editing = ref<SubscriptionPlan | null>(null)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  description: '',
  price: 29,
  durationDays: 30,
  status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  durationDays: [{ required: true, message: '请输入有效天数', trigger: 'blur' }],
}

function flash(text: string, type: 'success' | 'error' = 'success') {
  message.value = text
  messageType.value = type
  setTimeout(() => (message.value = ''), 3000)
}

async function load() {
  loading.value = true
  try {
    const res = await listMyPlans()
    if (res.data.success) items.value = res.data.data
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', description: '', price: 29, durationDays: 30, status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(item: SubscriptionPlan) {
  editing.value = item
  Object.assign(form, {
    name: item.name,
    description: item.description ?? '',
    price: Number(item.price),
    durationDays: item.durationDays,
    status: item.status,
  })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = { ...form, description: form.description || null }
    const res = editing.value
      ? await updatePlan(editing.value.id, payload)
      : await createPlan(payload)
    if (res.data.success) {
      dialogVisible.value = false
      flash(editing.value ? '已保存' : '已新增')
      await load()
    } else {
      flash(res.data.message || '保存失败', 'error')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    flash(err.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function handleDelete(item: SubscriptionPlan) {
  try {
    await ElMessageBox.confirm(`确定删除套餐「${item.name}」吗？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
  } catch {
    return
  }
  try {
    const res = await deletePlan(item.id)
    if (res.data.success) {
      flash('已删除')
      await load()
    } else {
      flash(res.data.message || '删除失败', 'error')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    flash(err.response?.data?.message || '删除失败', 'error')
  }
}

onMounted(load)
</script>

<style scoped>
.plans-page { padding: 60px 0 30px; }
.toolbar { display: flex; align-items: center; gap: 18px; padding: 20px 0 8px; }
/* 刻意不叫 admin-table：管理端有同名全局类（styles/admin-system.scss），
   用独立类名避免创作者端被管理端样式意外影响 */
.plans-table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 12px; }
.plans-table th {
  text-align: left; font: 10px 'DM Mono', monospace; color: var(--muted);
  padding: 10px 8px; border-bottom: 1px solid var(--line);
}
.plans-table td { padding: 13px 8px; border-bottom: 1px solid var(--line); }
.cell-name { font-weight: 600; }
.cell-name small { display: block; margin-top: 4px; font-weight: 400; color: var(--muted); }
.cell-actions a { margin-right: 12px; cursor: pointer; text-decoration: underline; }
.cell-actions a.danger { color: #c54a32; }
.status-tag { font: 10px 'DM Mono', monospace; padding: 2px 7px; border: 1px solid var(--line); }
.status-on { color: #68863d; border-color: #68863d; }
.status-off { color: var(--muted); }
.success-text { color: #68863d; font-size: 12px; }
</style>
