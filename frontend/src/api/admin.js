import request from './request'

/**
 * 管理后台接口（仅管理员 role=0 可用）
 * 对齐：AdminController -> /api/admin/*
 */

/* 分页字段翻译 */
function translatePage(p = {}) {
  const out = { ...p }
  if (typeof out.page === 'number' && out.pageNum === undefined) out.pageNum = out.page
  if (typeof out.size === 'number' && out.pageSize === undefined) out.pageSize = out.size
  return out
}

/* ==============================================================================
 * 1. 用户管理
 * ============================================================================== */

export function listUsers(params = {}) {
  return request.get('/admin/users', { params: translatePage(params) })
}
export function getUserDetail(userId) {
  return request.get(`/admin/users/${userId}`)
}
export function adminResetPassword(data = {}) {
  return request.post('/admin/users/reset-password', data)
}

/** 解锁用户（清空失败次数 + 恢复状态） */
export function unlockUser(userId) {
  return request.post(`/admin/users/${userId}/unlock`)
}
/** 页面调用别名：adminUnlockUser / adminLockUser（lock 后端暂未实现，先做占位） */
export function adminUnlockUser(userId) { return unlockUser(userId) }
export function adminLockUser(userId) {
  // 后端暂无 lock 端点，POST 404 也 OK，先让前端可编译；后续补 Controller
  return request.post(`/admin/users/${userId}/lock`)
}

/** 管理员删除用户 */
export function deleteUser(userId) {
  return request.delete(`/admin/users/${userId}`)
}
/** 页面调用别名：adminDeleteUser */
export function adminDeleteUser(userId) { return deleteUser(userId) }

/** 创建单个用户 */
export function createUser(data = {}) {
  return request.post('/admin/users', data)
}
/** 管理员改用户任意字段（不含密码） */
export function updateUser(userId, data = {}) {
  const body = typeof userId === 'object' ? userId : { id: userId, ...(data || {}) }
  return request.put(`/admin/users/${body.id || userId}`, body)
}

/**
 * Excel 批量导入用户
 * POST /api/admin/users/import   FormData(file)
 */
export function importUsersByExcel(file, onProgress) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/admin/users/import', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}
/** 页面调用别名：importUsers(file, progressCb) */
export function importUsers(file, onProgress) { return importUsersByExcel(file, onProgress) }

/* ==============================================================================
 * 2. 资源管理 & 审核
 * ============================================================================== */

/** 全量资源列表（任意状态） */
export function listAllResources(params = {}) {
  return request.get('/admin/resources', { params: translatePage(params) })
}
export function getResourceById(resourceId) {
  return request.get(`/admin/resources/${resourceId}`)
}

/**
 * 统一审核接口（通过或拒绝）
 * POST /api/admin/resources/review
 * body: { resourceId, approve: true通过 / false拒绝, rejectReason?拒绝时必填 }
 *   ⚠️ 后端真实字段：approve 是 Boolean（true/false），不是 reviewStatus 数字！
 *      @NotNull 校验 approve：不传直接抛「审核动作不能为空」
 */
export function reviewResource(data = {}) {
  return request.post('/admin/resources/review', data)
}

/** 页面调用：审核通过 */
export function reviewPassResource(resourceId) {
  return reviewResource({ resourceId, approve: true })
}
/** 页面调用：审核拒绝（带拒绝原因） */
export function reviewRejectResource({ resourceId, reviewRemark }) {
  return reviewResource({
    resourceId,
    approve: false,
    rejectReason: reviewRemark || '不符合资源规范，请修改后重新提交'
  })
}

/** 管理员删除资源（同时释放引用计数） */
export function deleteResource(resourceId) {
  return request.delete(`/admin/resources/${resourceId}`)
}
/** 页面调用别名 */
export function adminDeleteResource(resourceId) { return deleteResource(resourceId) }

export default {
  listUsers,
  getUserDetail,
  adminResetPassword,
  unlockUser, adminUnlockUser, adminLockUser,
  deleteUser, adminDeleteUser,
  createUser,
  updateUser,
  importUsersByExcel, importUsers,
  listAllResources,
  getResourceById,
  reviewResource, reviewPassResource, reviewRejectResource,
  deleteResource, adminDeleteResource
}
