import request from './request'

/**
 * 普通用户自用接口（个人中心）
 * 对齐：UserController -> /api/user/*
 */

/** 查询当前登录用户信息 */
export function getUserInfo() {
  return request.get('/user/info')
}

/** 修改/补充当前登录用户信息（动态更新，仅非空字段生效） */
export function updateProfile(data = {}) {
  return request.post('/user/info', data)
}

/** 修改当前登录用户密码（需要旧密码） */
export function changePassword(data = {}) {
  return request.post('/user/password', data)
}

/**
 * 上传/更换个人头像（后端暂无实现时会 404，前端先保留入口）
 * POST /api/user/avatar   FormData: { file }
 * 后端约定返回：{ avatarUrl: String, avatar: String }
 */
export function updateAvatar(file, onProgress) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/user/avatar', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export default {
  getUserInfo,
  updateProfile,
  changePassword,
  updateAvatar
}
