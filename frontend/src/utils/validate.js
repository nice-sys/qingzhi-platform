/**
 * 前端正则校验工具
 * ⚠️ 三个正则与后端 Constants.PASSWORD_REGEX / PHONE_REGEX / EMAIL_REGEX 100% 对齐
 */

// 密码：>=8位，必须同时包含数字 + 字母
export const PASSWORD_REGEX = /^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$/
// 手机号：中国大陆 11 位，1[3-9] 开头
export const PHONE_REGEX = /^1[3-9]\d{9}$/
// 邮箱
export const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/
// 学号/工号：由字母和数字组成（纯数字学号/含字母工号都能过）
export const USERNAME_REGEX = /^[A-Za-z0-9]{3,30}$/

/* -------- Element Plus 表单校验器 -------- */

export function buildUsernameValidator(message = '请输入3-30位字母或数字的账号') {
  return (_rule, value, cb) => {
    if (!value || !value.trim()) return cb(new Error('账号不能为空'))
    if (!USERNAME_REGEX.test(value.trim())) return cb(new Error(message))
    cb()
  }
}

export function buildPasswordValidator(message = '密码至少8位，须同时包含数字和字母') {
  return (_rule, value, cb) => {
    if (!value) return cb(new Error('密码不能为空'))
    if (!PASSWORD_REGEX.test(value)) return cb(new Error(message))
    cb()
  }
}

export function buildPhoneValidator(required = false) {
  return (_rule, value, cb) => {
    if (!value) {
      return required ? cb(new Error('手机号不能为空')) : cb()
    }
    if (!PHONE_REGEX.test(String(value).trim())) {
      return cb(new Error('请输入11位正确的手机号'))
    }
    cb()
  }
}

export function buildEmailValidator(required = false) {
  return (_rule, value, cb) => {
    if (!value) {
      return required ? cb(new Error('邮箱不能为空')) : cb()
    }
    const trimmed = String(value).trim()
    if (trimmed.length > 100) return cb(new Error('邮箱长度不能超过100'))
    if (!EMAIL_REGEX.test(trimmed)) return cb(new Error('请输入正确的邮箱格式'))
    cb()
  }
}

/* -------- 布尔型校验 -------- */

export const isValidPassword = (v) => !!v && PASSWORD_REGEX.test(v)
export const isValidPhone    = (v) => !!v && PHONE_REGEX.test(String(v).trim())
export const isValidEmail    = (v) => !!v && EMAIL_REGEX.test(String(v).trim())
export const isValidUsername = (v) => !!v && USERNAME_REGEX.test(String(v).trim())
