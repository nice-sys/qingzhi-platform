import { defineStore } from 'pinia'
import { storage } from '@/utils/storage'
import { ROLE, ROLE_NAME, isAdmin, isTeacher, isStudent } from '@/utils/permission'

const KEY_TOKEN = 'token'
const KEY_USER  = 'user_info'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: storage.get(KEY_TOKEN, ''),
    userInfo: storage.get(KEY_USER, null)
  }),

  getters: {
    isLoggedIn: (s) => !!s.token && s.token.length > 0,
    role: (s) => (s.userInfo && s.userInfo.role != null) ? s.userInfo.role : null,
    roleName: (s) => {
      const r = (s.userInfo && s.userInfo.role != null) ? s.userInfo.role : null
      return r != null && ROLE_NAME[r] != null ? ROLE_NAME[r] : ''
    },
    username: (s) => s.userInfo?.username || '',
    name: (s) => s.userInfo?.name || s.userInfo?.username || '',
    userId: (s) => s.userInfo?.id || null,
    isAdmin: (s) => isAdmin(s.userInfo?.role),
    isTeacher: (s) => isTeacher(s.userInfo?.role),
    isStudent: (s) => isStudent(s.userInfo?.role)
  },

  actions: {
    /**
     * 登录成功后保存 token + userInfo（来自后端 LoginResponse.data = {token, userInfo}）
     */
    setLoginData(token, userInfo) {
      this.token = token || ''
      this.userInfo = userInfo || null
      storage.set(KEY_TOKEN, this.token)
      storage.set(KEY_USER, this.userInfo)
    },

    /** 登录后刷新 userInfo（如个人信息修改后） */
    setUserInfo(userInfo) {
      this.userInfo = userInfo || null
      storage.set(KEY_USER, this.userInfo)
    },

    /** 更新部分 userInfo 字段 */
    patchUserInfo(partial = {}) {
      if (!this.userInfo) this.userInfo = {}
      this.userInfo = { ...this.userInfo, ...partial }
      storage.set(KEY_USER, this.userInfo)
    },

    /** 登出：清空内存 + localStorage */
    logout() {
      this.token = ''
      this.userInfo = null
      storage.remove(KEY_TOKEN)
      storage.remove(KEY_USER)
    }
  }
})

export default useUserStore
