/**
 * v-permission 自定义指令：按当前用户角色显示/隐藏元素
 * 用法：
 *   <el-button v-permission="[0]">仅管理员可见</el-button>
 *   <el-button v-permission="[1,2]">教师/学生可见</el-button>
 *   <el-button v-permission="'admin'">文字别名也可以：admin/teacher/student</el-button>
 */
import { useUserStore } from '@/stores/userStore'
import { ROLE, roleMatchesAny } from '@/utils/permission'

const aliasMap = {
  admin: ROLE.ADMIN,
  teacher: ROLE.TEACHER,
  student: ROLE.STUDENT
}

function normalize(value) {
  if (value == null) return []
  if (Array.isArray(value)) {
    return value
      .map(v => typeof v === 'string' && aliasMap[v] != null ? aliasMap[v] : Number(v))
      .filter(v => !isNaN(v))
  }
  if (typeof value === 'string') {
    if (aliasMap[value] != null) return [aliasMap[value]]
    return [Number(value)].filter(v => !isNaN(v))
  }
  if (typeof value === 'number') return [value]
  return []
}

function setVisible(el, visible) {
  if (visible) {
    el.style.display = el.__qzOriginDisplay || ''
  } else {
    if (!('__qzOriginDisplay' in el)) {
      el.__qzOriginDisplay = el.style.display
    }
    el.style.display = 'none'
  }
}

export const permissionDirective = {
  mounted(el, binding) {
    const user = useUserStore()
    const allowed = normalize(binding.value)
    if (allowed.length === 0) return
    setVisible(el, roleMatchesAny(user.role, allowed))
  },
  updated(el, binding) {
    if (binding.value === binding.oldValue) return
    const user = useUserStore()
    const allowed = normalize(binding.value)
    if (allowed.length === 0) {
      setVisible(el, true)
      return
    }
    setVisible(el, roleMatchesAny(user.role, allowed))
  }
}

export function installPermissionDirective(app) {
  app.directive('permission', permissionDirective)
}

export default permissionDirective
