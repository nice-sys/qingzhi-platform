import { defineStore } from 'pinia'
import { storage } from '@/utils/storage'
import { ROLE, ROLE_NAME, isAdmin, isTeacher, isStudent } from '@/utils/permission'

const KEY_TOKEN   = 'token'
const KEY_USER    = 'user_info'
const KEY_REMEMBER_UNAME = 'remember_uname'

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
    isStudent: (s) => isStudent(s.userInfo?.role),
    /** localStorage 里记住的用户名（用于登录页回填，不自动登录） */
    rememberedUsername: () => storage.get(KEY_REMEMBER_UNAME, '')
  },

  actions: {
    /**
     * 登录成功后保存 token + userInfo（来自后端 LoginResponse.data = {token, userInfo}）
     * @param {string} token
     * @param {object} userInfo
     * @param {boolean} rememberMe 是否勾选「记住我」：true=把用户名单独保存到 localStorage（不存密码，也不自动登录）；false=清空保存的用户名
     */
    setLoginData(token, userInfo, rememberMe = false) {
      this.token = token || ''
      this.userInfo = userInfo || null
      storage.set(KEY_TOKEN, this.token)
      storage.set(KEY_USER, this.userInfo)
      const uname = (this.userInfo && this.userInfo.username) ? this.userInfo.username : ''
      if (rememberMe && uname) {
        storage.set(KEY_REMEMBER_UNAME, uname)
      } else {
        storage.remove(KEY_REMEMBER_UNAME)
      }
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

    /** 返回 localStorage 里保存的记住我用户名（若有），用于登录页自动回填用户名 */
    getRememberedUsername() {
      return storage.get(KEY_REMEMBER_UNAME, '') || ''
    },

    /** 登出：清空内存 + localStorage（无论 token 是否在有效期内，立即清 token/userInfo/remember_uname 三项） */
    logout() {
      this.token = ''
      this.userInfo = null
      storage.remove(KEY_TOKEN)
      storage.remove(KEY_USER)
      storage.remove(KEY_REMEMBER_UNAME)
    }
  }
})

export default useUserStore
