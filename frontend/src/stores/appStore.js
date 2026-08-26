import { defineStore } from 'pinia'

/**
 * 全局 App 状态（侧边栏折叠、主题色等）
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    loadingCount: 0
  }),

  getters: {
    isLoading: (s) => s.loadingCount > 0
  },

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    setSidebarCollapsed(collapsed) {
      this.sidebarCollapsed = !!collapsed
    },
    pushLoading() {
      this.loadingCount++
    },
    popLoading() {
      if (this.loadingCount > 0) this.loadingCount--
    }
  }
})

export default useAppStore
