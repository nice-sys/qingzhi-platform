<template>
  <div class="register-page h-full w-full">
    <div class="register-bg h-full w-full flex-center">
      <el-card class="register-card" shadow="hover">
        <div class="card-title mb-8">欢迎注册</div>
        <div class="card-sub mb-24 text-muted">教师用「工号」，学生用「学号」注册；管理员账号不可自行注册</div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
        >
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="账号（学号/工号）" prop="username">
                <el-input v-model="form.username" placeholder="字母/数字 3-30 位" clearable maxlength="30" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="角色" prop="role">
                <el-select v-model="form.role" class="w-full" placeholder="请选择身份">
                  <el-option label="教师" :value="1" />
                  <el-option label="学生" :value="2" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="密码" prop="password">
                <el-input v-model="form.password" type="password" show-password
                          placeholder=">=8位，含数字+字母" maxlength="32" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" show-password
                          placeholder="再输一遍密码" maxlength="32" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="姓名（选填）">
            <el-input v-model="form.name" placeholder="请输入姓名" maxlength="50" />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="手机号（选填）" prop="phone">
                <el-input v-model="form.phone" placeholder="11 位中国大陆手机号" maxlength="11" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱（选填）" prop="email">
                <el-input v-model="form.email" placeholder="example@school.edu.cn" maxlength="100" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="院系（选填）">
                <el-input v-model="form.department" placeholder="如：计算机学院" maxlength="100" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="专业/班级（选填，学生建议填）">
                <el-input v-model="form.major" placeholder="如：软件工程2301" maxlength="100" />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="flex-between mt-8">
            <div class="text-sm">
              已有账号？<router-link to="/login" class="text-primary">返回登录 →</router-link>
            </div>
            <el-button type="primary" size="large" :loading="loading" @click="doRegister">
              提交注册
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import {
  buildUsernameValidator,
  buildPasswordValidator,
  buildPhoneValidator,
  buildEmailValidator
} from '@/utils/validate'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  role: 2,
  password: '',
  confirmPassword: '',
  name: '',
  phone: '',
  email: '',
  department: '',
  major: ''
})

const confirmPasswordValidator = (_r, v, cb) => {
  if (!v) return cb(new Error('请再次输入密码'))
  if (v !== form.password) return cb(new Error('两次输入的密码不一致'))
  cb()
}

const rules = {
  username:        [{ validator: buildUsernameValidator(), trigger: 'blur' }],
  role:            [{ required: true, message: '请选择身份', trigger: 'change' }],
  password:        [{ validator: buildPasswordValidator(), trigger: 'blur' }],
  confirmPassword: [{ validator: confirmPasswordValidator, trigger: 'blur' }],
  phone:           [{ validator: buildPhoneValidator(false), trigger: 'blur' }],
  email:           [{ validator: buildEmailValidator(false), trigger: 'blur' }]
}

async function doRegister() {
  try {
    await formRef.value.validate()
  } catch (_) { return }
  loading.value = true
  try {
    const data = await register({ ...form })
    // 后端注册成功：返回 {token, userInfo}（有 token 才免登录进系统；老版本仅返回 userInfo 时仍走手动登录）
    if (data && data.token) {
      const { useUserStore } = await import('@/stores/userStore')
      useUserStore().setLoginData(data.token, data.userInfo)
      ElMessage.success('注册成功，欢迎加入青知')
      router.replace('/dashboard')
      return
    }
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
  } catch (_) {} finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  background: linear-gradient(135deg, #1a2a3a 0%, #26403d 50%, #2f7a6b 100%);
}
.register-bg {
  padding: 40px 20px;
}
.register-card {
  width: 760px;
  max-width: 100%;
  border-radius: 12px;
}
.card-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--qz-text-primary);
}
.card-sub  { font-size: 13px; }
.text-muted{ color: var(--qz-text-secondary); }
.text-primary { color: var(--qz-primary); }
.text-sm { font-size: 13px; }
.w-full { width: 100%; }
.mb-8  { margin-bottom: 8px; }
.mb-24 { margin-bottom: 24px; }
.mt-8  { margin-top: 8px; }
</style>
