<template>
  <div class="login-page h-full w-full">
    <div class="login-bg h-full w-full flex-center">
      <!-- 左侧品牌介绍 -->
      <div class="login-left hidden lg:block">
        <div class="brand-title mb-16">青知共享平台</div>
        <p class="brand-sub mb-8">开放 · 共享 · 共建学习资源生态</p>
        <ul class="brand-points">
          <li>📚 发布学习资料，共建资源库</li>
          <li>🔍 按课程 / 关键词快速检索</li>
          <li>⭐️ 一键收藏，随时回看</li>
          <li>🛡️ 管理员审核制，保障质量</li>
        </ul>
      </div>

      <!-- 右侧登录卡片 -->
      <el-card class="login-card" shadow="hover">
        <div class="card-title mb-24">欢迎登录</div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="doLogin"
        >
          <el-form-item label="账号（学号 / 工号 / Admin）" prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" clearable maxlength="30" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码（>=8位，含数字+字母）"
              show-password
              maxlength="32"
              @keyup.enter="doLogin"
            />
          </el-form-item>

          <div class="card-tips mb-16 text-muted text-sm">
            💡 管理员初始账号：<code>{{ DEFAULT_ADMIN.username }} / {{ DEFAULT_ADMIN.password }}</code>
          </div>

          <el-button
            type="primary"
            class="w-full"
            size="large"
            :loading="loading"
            @click="doLogin"
          >登 录</el-button>

          <div class="to-register mt-16 text-right text-sm">
            还没有账号？
            <router-link to="/register" class="text-primary">立即注册 →</router-link>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/userStore'
import { buildUsernameValidator, buildPasswordValidator } from '@/utils/validate'
import { DEFAULT_ADMIN } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: DEFAULT_ADMIN.username,
  password: DEFAULT_ADMIN.password
})

const rules = {
  username: [{ validator: buildUsernameValidator(), trigger: 'blur' }],
  password: [{ validator: buildPasswordValidator(), trigger: 'blur' }]
}

async function doLogin() {
  try {
    await formRef.value.validate()
  } catch (_) { return }
  loading.value = true
  try {
    const data = await login({ username: form.username.trim(), password: form.password })
    // 后端 LoginResponse: { token: String, userInfo: UserInfoResponse }
    user.setLoginData(data && data.token, data && data.userInfo)
    ElMessage.success('登录成功')
    const redirect = (route.query && route.query.redirect) || '/dashboard'
    router.replace(redirect)
  } catch (_e) {
    // 统一错误已由 request interceptor 提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  background: linear-gradient(135deg, #1a2a3a 0%, #26403d 50%, #2f7a6b 100%);
}
.login-bg {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 80px;
  padding: 40px;
}
.login-left {
  color: #fff;
  max-width: 420px;
}
.brand-title {
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 4px;
}
.brand-sub {
  font-size: 16px;
  opacity: .85;
}
.brand-points {
  list-style: none;
  padding: 0;
  font-size: 15px;
  line-height: 2.2;
  opacity: .95;
}
.login-card {
  width: 420px;
  border-radius: 12px;
}
.card-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--qz-text-primary);
}
.text-muted { color: var(--qz-text-secondary); }
.text-sm { font-size: 13px; }
.text-right { text-align: right; }
.text-primary { color: var(--qz-primary); }
.mb-8  { margin-bottom: 8px; }
.mb-16 { margin-bottom: 16px; }
.mb-24 { margin-bottom: 24px; }
.mt-16 { margin-top: 16px; }
.w-full { width: 100%; }
@media (max-width: 1024px) {
  .login-bg { flex-direction: column; gap: 32px; }
  .login-card { width: 100%; max-width: 420px; }
}
</style>
