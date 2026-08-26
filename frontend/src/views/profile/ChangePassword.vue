<template>
  <div class="qz-page page-change-pwd">
    <el-card class="qz-card mx-auto" style="max-width:640px" shadow="never">
      <template #header><strong>🔐 修改密码</strong></template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        size="default"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password maxlength="32" placeholder="请输入当前登录密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password maxlength="32" placeholder=">=8位，必须包含数字+字母" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password maxlength="32" placeholder="再输一遍新密码" />
        </el-form-item>

        <div class="tips text-muted text-sm mb-16">
          ⚠️ 为了安全，密码建议包含：大小写字母、数字、特殊字符中 3 种以上；修改成功后需重新登录。
        </div>

        <div class="text-right">
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" :loading="saving" @click="submit">提交修改</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { buildPasswordValidator } from '@/utils/validate'
import { changePassword } from '@/api/user'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const user = useUserStore()
const formRef = ref(null)
const saving = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const confirmValidator = (_r, v, cb) => {
  if (!v) return cb(new Error('请再次输入新密码'))
  if (v !== form.newPassword) return cb(new Error('两次输入的新密码不一致'))
  cb()
}
const oldPwdNewDifferent = (_r, v, cb) => {
  if (v && v === form.oldPassword) return cb(new Error('新密码不能与原密码相同'))
  cb()
}

const rules = {
  oldPassword:     [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword:     [
    { validator: buildPasswordValidator(), trigger: 'blur' },
    { validator: oldPwdNewDifferent,  trigger: 'blur' }
  ],
  confirmPassword: [{ validator: confirmValidator, trigger: 'blur' }]
}

function reset() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value && formRef.value.clearValidate()
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch (_) { return }
  saving.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    user.logout()
    setTimeout(() => router.replace('/login'), 500)
  } catch (_) {} finally { saving.value = false }
}
</script>

<style scoped>
.mx-auto { margin-left: auto; margin-right: auto; }
.text-muted { color: var(--qz-text-secondary); }
.text-sm    { font-size: 13px; }
.text-right { text-align: right; }
.mb-16 { margin-bottom: 16px; }
.tips  { line-height: 1.8; }
</style>
