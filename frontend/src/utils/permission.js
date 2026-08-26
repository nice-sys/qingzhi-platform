/**
 * 角色 & 权限工具（与后端 RoleEnum 100% 对齐）
 * ADMIN=0, TEACHER=1, STUDENT=2
 */

export const ROLE = Object.freeze({
  ADMIN: 0,
  TEACHER: 1,
  STUDENT: 2
})

export const ROLE_NAME = Object.freeze({
  [ROLE.ADMIN]: '管理员',
  [ROLE.TEACHER]: '教师',
  [ROLE.STUDENT]: '学生'
})

export const ROLE_TAG_COLOR = Object.freeze({
  [ROLE.ADMIN]: 'var(--qz-role-admin)',
  [ROLE.TEACHER]: 'var(--qz-role-teacher)',
  [ROLE.STUDENT]: 'var(--qz-role-student)'
})

/** 审核状态（与 ReviewStatusEnum 对齐） */
export const REVIEW = Object.freeze({
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2
})

export const REVIEW_NAME = Object.freeze({
  [REVIEW.PENDING]: '待审核',
  [REVIEW.APPROVED]: '已通过',
  [REVIEW.REJECTED]: '已拒绝'
})

export const REVIEW_TAG_TYPE = Object.freeze({
  [REVIEW.PENDING]: 'warning',
  [REVIEW.APPROVED]: 'success',
  [REVIEW.REJECTED]: 'danger'
})

/* -------- 角色判断 -------- */
export const isAdmin   = (role) => Number(role) === ROLE.ADMIN
export const isTeacher = (role) => Number(role) === ROLE.TEACHER
export const isStudent = (role) => Number(role) === ROLE.STUDENT
export const hasRole   = (role, target) => Number(role) === Number(target)

/**
 * 多角色权限匹配（任一命中即可）
 * @param {number|string} role 当前用户角色 code
 * @param {Array<number>} allowed 允许的角色 code 列表
 */
export function roleMatchesAny(role, allowed = []) {
  if (!allowed || !allowed.length) return true
  return allowed.includes(Number(role))
}
