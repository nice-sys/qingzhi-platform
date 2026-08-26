<template>
  <div class="qz-page page-profile-info">
    <el-row :gutter="16">
      <!-- 基本信息展示 -->
      <el-col :span="8">
        <el-card class="qz-card user-card" shadow="never">
          <div class="flex-col items-center text-center">
            <!-- 头像上传 -->
            <el-upload
              class="avatar-uploader"
              action=""
              :show-file-list="false"
              :auto-upload="false"
              :before-upload="beforeAvatar"
              accept="image/*"
              :on-change="onAvatarFile"
            >
              <div class="avatar-wrap" :style="{ background: bg }">
                <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" />
                <el-icon v-else :size="40" class="avatar-placeholder"><User /></el-icon>
                <div v-if="!avatarUploading" class="avatar-mask">
                  <el-icon><Camera /></el-icon>
                  <span class="ml-4">点击更换</span>
                </div>
                <div v-else class="avatar-mask loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span class="ml-4">上传中...</span>
                </div>
              </div>
            </el-upload>

            <div class="text-muted text-xs mt-8">
              支持 JPG / PNG / GIF，单张 <= {{ formatSize(AVATAR_MAX) }}
            </div>

            <h3 class="name mt-12 mb-4">{{ info.name || info.username || '-' }}</h3>
            <el-tag size="large" effect="plain" :color="bg">
              {{ roleName }}
            </el-tag>
            <p class="text-muted mt-16 mb-0">
              {{ info.department || info.major || '暂未填写院系/专业' }}
            </p>
          </div>

          <el-divider />

          <ul class="info-list">
            <li><span>账号</span><b>{{ info.username || '-' }}</b></li>
            <li><span>ID</span><b>{{ info.id || '-' }}</b></li>
            <li><span>手机</span><b>{{ info.phone || '-' }}</b></li>
            <li><span>邮箱</span><b class="truncate" :title="info.email || ''">{{ info.email || '-' }}</b></li>
            <li><span>院系</span><b>{{ info.department || '-' }}</b></li>
            <li><span>专业/班级</span><b>{{ info.major || '-' }}</b></li>
            <li><span>注册时间</span><b>{{ formatDateTime(info.createdAt) }}</b></li>
          </ul>
        </el-card>
      </el-col>

      <!-- 编辑表单 -->
      <el-col :span="16">
        <el-card class="qz-card" shadow="never">
          <template #header>
            <div class="flex-between">
              <strong>✏️ 完善个人信息</strong>
              <el-button link type="primary" @click="fetchInfo" :loading="loading">
                <el-icon><Refresh /></el-icon>&nbsp;刷新
              </el-button>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" size="default">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="姓名">
                  <el-input v-model="form.name" maxlength="50" placeholder="选填，真实姓名" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="角色">
                  <el-input :model-value="roleName" disabled />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号（选填）" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="form.email" maxlength="100" placeholder="example@school.edu.cn（选填）" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="院系">
                  <el-input v-model="form.department" maxlength="100" placeholder="如：计算机学院（选填）" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="专业/班级">
                  <el-input v-model="form.major" maxlength="100" placeholder="如：软件工程2301（选填）" />
                </el-form-item>
              </el-col>
            </el-row>

            <div class="text-right">
              <el-button @click="reset">取消</el-button>
              <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
            </div>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import { getUserInfo, updateProfile, updateAvatar } from '@/api/user'
import { ROLE_NAME } from '@/utils/permission'
import { buildPhoneValidator, buildEmailValidator } from '@/utils/validate'
import { formatDateTime, formatFileSize } from '@/utils/format'

const AVATAR_MAX = 2 * 1024 * 1024 // 2MB
const formatSize = formatFileSize

const user = useUserStore()
const loading = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const formRef = ref(null)

const info = ref({})
const form = reactive({
  name: '',
  phone: '',
  email: '',
  department: '',
  major: ''
})
const rules = {
  phone: [{ validator: buildPhoneValidator(false), trigger: 'blur' }],
  email: [{ validator: buildEmailValidator(false), trigger: 'blur' }]
}

const bg = computed(() => {
  const r = user.role
  return r === 0 ? 'var(--qz-role-admin)'
       : r === 1 ? 'var(--qz-role-teacher)'
       : 'var(--qz-role-student)'
})
const roleName = computed(() => ROLE_NAME[user.role] || '未登录用户')

/* 取 avatar：后端字段可能叫 avatar / avatarUrl / avatarURL */
const avatarUrl = computed(() => {
  const i = info.value && Object.keys(info.value).length
    ? info.value
    : (user.userInfo || {})
  return i.avatarUrl || i.avatarURL || i.avatar || ''
})

function patch(v) {
  const o = v || {}
  info.value = o
  form.name = o.name || ''
  form.phone = o.phone || ''
  form.email = o.email || ''
  form.department = o.department || ''
  form.major = o.major || ''
}

async function fetchInfo() {
  loading.value = true
  try {
    const data = await getUserInfo()
    patch(data)
    user.setUserInfo(data)
  } catch (_) {} finally { loading.value = false }
}
function reset() { patch(info.value) }

async function save() {
  try {
    await formRef.value.validate()
  } catch (_) { return }
  saving.value = true
  try {
    const data = await updateProfile({ ...form })
    patch(data)
    user.setUserInfo(data)
    ElMessage.success('个人信息已更新')
  } catch (_) {} finally { saving.value = false }
}

/* -------------------- 头像上传 -------------------- */
function beforeAvatar(file) {
  if (!/^image\//.test(file.type || '')) {
    ElMessage.error('请选择图片格式（JPG / PNG / GIF）')
    return false
  }
  if (file.size > AVATAR_MAX) {
    ElMessage.error(`图片不能超过 ${formatSize(AVATAR_MAX)}`)
    return false
  }
  return true
}
async function onAvatarFile(file) {
  if (!beforeAvatar(file.raw || file)) return
  avatarUploading.value = true
  try {
    const data = await updateAvatar(file.raw || file)
    /* 后端未实现时 request 会 4xx/5xx 报错不会到这里；实现后返回 {avatarUrl, avatar...}
       同时写回 info + store，Header 也会响应式显示新头像 */
    const newUrl = data?.avatarUrl || data?.avatarURL || data?.avatar || ''
    if (newUrl) {
      const next = { ...(info.value || {}), avatarUrl: newUrl, avatar: newUrl }
      info.value = next
      user.setUserInfo(next)
      ElMessage.success('头像更新成功')
    } else {
      ElMessage.success('已上传')
    }
  } catch (_) {
    /* 后端暂未实现时给出友好提示，不会白屏 */
    ElMessage.warning('头像上传接口暂未开启，后端实现后即可使用')
  } finally { avatarUploading.value = false }
}

onMounted(() => {
  if (user.userInfo && user.userInfo.username) patch(user.userInfo)
  fetchInfo()
})
</script>

<style scoped>
.user-card .name { font-size: 20px; font-weight: 700; color: var(--qz-text-primary); }
.text-muted { color: var(--qz-text-secondary); }
.text-xs { font-size: 12px; }
.text-right { text-align: right; }
.mt-4  { margin-top: 4px; }
.mt-8  { margin-top: 8px; }
.mt-12 { margin-top: 12px; }
.mt-16 { margin-top: 16px; }
.mb-0  { margin-bottom: 0; }
.mb-4  { margin-bottom: 4px; }
.ml-4  { margin-left: 4px; }
.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar-uploader { display: block; }
.avatar-wrap {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  color: #fff;
  transition: box-shadow .2s ease, transform .2s ease;
  box-shadow: 0 4px 10px 0 rgba(0,0,0,0.1);
}
.avatar-wrap:hover { transform: translateY(-1px); box-shadow: 0 6px 14px 0 rgba(0,0,0,0.16); }
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  background: #fff;
}
.avatar-placeholder { color: rgba(255,255,255,0.9); }
.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.55);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity .2s ease;
}
.avatar-wrap:hover .avatar-mask,
.avatar-mask.loading { opacity: 1; }

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
  color: var(--qz-text-secondary);
}
.info-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dashed var(--qz-border-light);
  padding: 10px 4px;
  font-size: 13px;
}
.info-list li:last-child { border-bottom: none; }
.info-list li b {
  color: var(--qz-text-primary);
  font-weight: 500;
  max-width: 60%;
  text-align: right;
}
</style>
