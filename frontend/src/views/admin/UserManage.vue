<template>
  <div class="qz-page page-user-manage" v-permission="'admin'">
    <!-- Toolbar -->
    <el-card class="qz-card mb-16" shadow="never">
      <el-form :model="query" inline label-position="right" size="default">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名/姓名/手机/邮箱" clearable style="width:240px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width:120px">
            <el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width:120px">
            <el-option label="正常" :value="USER_STATUS.NORMAL" />
            <el-option label="封禁" :value="USER_STATUS.LOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">
            <el-icon><Search /></el-icon>&nbsp;搜索
          </el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="openCreate">
            <el-icon><Plus /></el-icon>&nbsp;新增用户
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card class="qz-card" shadow="never">
      <EmptyState :total="total" desc="没有匹配的用户">
        <template #list>
          <el-table :data="list" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="账号/姓名" width="200">
              <template #default="{ row }">
                <div class="flex items-center gap-8">
                  <div class="user-mini-avatar">
                    <img
                      v-if="resolveAvatar(row)"
                      :src="resolveAvatar(row)"
                      class="mini-img"
                      @error="onAvatarError(row)"
                      alt=""
                    />
                    <el-avatar v-else :size="26" :style="{ backgroundColor: roleColor(row.role) }">
                      {{ (row.name || row.username || 'U').charAt(0) }}
                    </el-avatar>
                  </div>
                  <div>
                    <div><b>{{ row.username }}</b></div>
                    <div class="text-muted text-xs">{{ row.name || '-' }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :color="roleColor(row.role)">{{ ROLE_NAME[row.role] || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="department" label="院系" min-width="140" show-overflow-tooltip />
            <el-table-column prop="major" label="专业/班级" min-width="140" show-overflow-tooltip />
            <el-table-column prop="phone" label="手机" width="140" />
            <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === USER_STATUS.NORMAL ? 'success' : 'danger'" effect="light">
                  {{ USER_STATUS_NAME[row.status] || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt || row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-popconfirm v-if="row.role !== ROLE.ADMIN"
                  :title="`将重置【${row.username}】的密码为 ${RESET_DEFAULT_PASSWORD}`"
                  @confirm="resetPwd(row)"
                >
                  <template #reference>
                    <el-button link type="primary">重置密码</el-button>
                  </template>
                </el-popconfirm>
                <el-button
                  v-if="row.role !== ROLE.ADMIN"
                  link
                  :type="row.status === USER_STATUS.NORMAL ? 'warning' : 'success'"
                  @click="row.status === USER_STATUS.NORMAL ? lock(row) : unlock(row)"
                >{{ row.status === USER_STATUS.NORMAL ? '封禁' : '解禁' }}</el-button>
                <el-popconfirm
                  v-if="row.role !== ROLE.ADMIN"
                  :title="`确认删除用户【${row.username}】？该操作不可恢复`"
                  @confirm="del(row)"
                >
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            :total="total"
            :current="query.page"
            :size="query.size"
            @change="({ page, size }) => { query.page = page; query.size = size; fetch() }"
          />
        </template>
      </EmptyState>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="formMode === 'create' ? '新增用户' : '编辑用户'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        size="default"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" :disabled="formMode === 'edit'" placeholder="学号/工号" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-select v-model="form.role" placeholder="请选择角色" style="width:100%">
                <el-option
                  v-for="o in editableRoleOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              v-if="formMode === 'create'"
              label="初始密码"
              prop="password"
            >
              <el-input v-model="form.password" type="password" show-password placeholder="默认 Qz123456" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="选填" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机" prop="phone">
              <el-input v-model="form.phone" placeholder="11 位手机号（选填）" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="邮箱（选填）" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="院系" prop="department">
              <el-input v-model="form.department" placeholder="选填" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业/班级" prop="major">
              <el-input v-model="form.major" placeholder="学生可选填" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="USER_STATUS.NORMAL">正常</el-radio>
                <el-radio :value="USER_STATUS.LOCKED">封禁</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider v-if="formMode === 'create'" />
        <div v-if="formMode === 'create'" class="text-muted text-sm tip-box">
          <p>💡 初始密码如留空，将使用系统默认密码：<b class="text-primary">{{ RESET_DEFAULT_PASSWORD }}</b></p>
          <p>账号仅支持 3-30 位字母或数字，创建后不可修改。</p>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">
          {{ formMode === 'create' ? '确认创建' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'
import {
  listUsers, createUser, updateUser,
  adminResetPassword, adminLockUser, adminUnlockUser, adminDeleteUser
} from '@/api/admin'
import { ROLE_NAME } from '@/utils/permission'
import {
  ROLE, ROLE_OPTIONS, USER_STATUS, USER_STATUS_NAME, RESET_DEFAULT_PASSWORD
} from '@/utils/constants'
import {
  buildUsernameValidator,
  buildPasswordValidator,
  buildPhoneValidator,
  buildEmailValidator
} from '@/utils/validate'
import { formatDateTime } from '@/utils/format'

const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', role: '', status: '' })
const formatTime = formatDateTime

const editableRoleOptions = computed(() =>
  ROLE_OPTIONS.filter(o => Number(o.value) !== ROLE.ADMIN)
)

const dialogVisible = ref(false)
const formMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  role: ROLE.STUDENT,
  password: '',
  name: '',
  phone: '',
  email: '',
  department: '',
  major: '',
  status: USER_STATUS.NORMAL
})
const brokenAvatarCache = reactive({})

const formRules = {
  username: [ buildUsernameValidator() ],
  password: [ buildPasswordValidator() ],
  role: [ { required: true, message: '请选择角色', trigger: 'change' } ],
  status: [ { required: true, message: '请选择账号状态', trigger: 'change' } ],
  phone:  [ buildPhoneValidator(false) ],
  email:  [ buildEmailValidator(false) ]
}

function roleColor(r) {
  return Number(r) === ROLE.ADMIN ? 'var(--qz-role-admin)'
       : Number(r) === ROLE.TEACHER ? 'var(--qz-role-teacher)'
       : 'var(--qz-role-student)'
}

function resolveAvatar(row) {
  if (!row) return ''
  if (brokenAvatarCache[row.id]) return ''
  return row.avatarUrl || row.avatarURL || row.avatar || ''
}
function onAvatarError(row) {
  if (row) brokenAvatarCache[row.id] = true
}

async function fetch() {
  try {
    const d = await listUsers({ ...query, status: query.status === '' ? undefined : query.status })
    list.value  = d.list  || []
    total.value = d.total || 0
  } catch (e) {
    ElMessage.warning(e?.message || '获取用户列表失败，请稍后重试')
  }
}
function reset() { query.keyword=''; query.role=''; query.status=''; query.page=1; fetch() }

function openCreate() {
  formMode.value = 'create'
  Object.assign(form, {
    id: null,
    username: '',
    role: ROLE.STUDENT,
    password: '',
    name: '',
    phone: '',
    email: '',
    department: '',
    major: '',
    status: USER_STATUS.NORMAL
  })
  dialogVisible.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    username: row.username || '',
    role: row.role ?? ROLE.STUDENT,
    password: '',
    name: row.name || '',
    phone: row.phone || '',
    email: row.email || '',
    department: row.department || '',
    major: row.major || '',
    status: row.status ?? USER_STATUS.NORMAL
  })
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (_) { return }

  submitting.value = true
  try {
    if (formMode.value === 'create') {
      const body = {
        username: form.username.trim(),
        role: form.role,
        password: form.password || RESET_DEFAULT_PASSWORD,
        name: form.name || null,
        phone: form.phone || null,
        email: form.email || null,
        department: form.department || null,
        major: form.major || null,
        status: form.status
      }
      await createUser(body)
      ElMessage.success(`已创建用户 ${body.username}，初始密码：${body.password}`)
    } else {
      const body = {
        id: form.id,
        name: form.name || null,
        role: form.role,
        phone: form.phone || null,
        email: form.email || null,
        department: form.department || null,
        major: form.major || null,
        status: form.status
      }
      await updateUser(body.id, body)
      ElMessage.success('用户信息已更新')
    }
    dialogVisible.value = false
    fetch()
  } catch (e) {
    ElMessage.warning(e?.message || (formMode.value === 'create' ? '创建失败，请检查账号是否重复' : '保存失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function resetPwd(r) {
  try {
    await adminResetPassword({ userId: r.id, newPassword: RESET_DEFAULT_PASSWORD })
    ElMessage.success(`密码已重置为 ${RESET_DEFAULT_PASSWORD}，请提醒用户尽快修改`)
  } catch (e) {
    ElMessage.warning(e?.message || '重置密码失败，请稍后重试')
  }
}
async function lock(r) {
  try { await adminLockUser(r.id); ElMessage.success('已封禁'); fetch() }
  catch (e) { ElMessage.warning(e?.message || '封禁失败（后端接口暂未开启或权限不足）') }
}
async function unlock(r) {
  try { await adminUnlockUser(r.id); ElMessage.success('已解禁'); fetch() }
  catch (e) { ElMessage.warning(e?.message || '解禁失败，请稍后重试') }
}
async function del(r) {
  try { await adminDeleteUser(r.id); ElMessage.success('已删除'); fetch() }
  catch (e) { ElMessage.warning(e?.message || '删除失败，请稍后重试') }
}

onMounted(fetch)
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.ml-8  { margin-left: 8px; }
.gap-8 { gap: 8px; }
.flex { display: flex; }
.items-center { align-items: center; }
.text-muted { color: var(--qz-text-secondary); }
.text-primary { color: var(--qz-primary); }
.text-sm { font-size: 13px; }
.text-xs { font-size: 12px; }
.tip-box { background: var(--el-fill-color-light); padding: 10px 14px; border-radius: 6px; }
.tip-box p { margin: 0; line-height: 1.8; }
.user-mini-avatar { flex-shrink: 0; }
.user-mini-avatar .mini-img {
  width: 26px; height: 26px; border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}
</style>
