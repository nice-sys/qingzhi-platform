import request from './request'

/**
 * 认证相关接口
 * 对齐：AuthController -> /api/auth/*
 */

/**
 * 注册（教师/学生自行注册；管理员不可注册）
 * POST /api/auth/register
 */
export function register(data = {}) {
  return request.post('/auth/register', data)
}

/**
 * 登录（管理员/教师/学生通用）
 * POST /api/auth/login
 * 返回：{token: String, userInfo: {id, username, name, role, ...}}
 */
export function login(data = {}) {
  return request.post('/auth/login', data)
}

export default {
  register,
  login
}
