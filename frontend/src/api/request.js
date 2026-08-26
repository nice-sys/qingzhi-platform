import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import router from '@/router'

/**
 * Axios 基础实例
 * 后端统一返回 Result<T> = { code:number, message:string, data:T }
 *   code=1 成功，其他 code 一律视为失败 reject
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

/* ============================================================
 * 请求拦截：自动注入 Bearer Token
 * ============================================================ */
request.interceptors.request.use(
  (config) => {
    const user = useUserStore()
    if (user.token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = 'Bearer ' + user.token
    }
    return config
  },
  (err) => Promise.reject(err)
)

/* ============================================================
 * 响应拦截：
 *  1. HTTP 401 → 清 token + 跳登录
 *  2. 正常 2xx → 解包 Result<T>：
 *     - code=1 成功 → resolve(data)
 *     - 其他 code → ElMessage + reject({code, message})
 *  3. 其他 HTTP 错误 → 统一提示
 * ============================================================ */
request.interceptors.response.use(
  (resp) => {
    const ct = resp.headers && resp.headers['content-type'] ? String(resp.headers['content-type']) : ''
    // 非 JSON（如文件下载流）直接返回，不走 Result 解包
    if (!ct.includes('application/json')) {
      return resp
    }

    const body = resp.data
    if (!body || typeof body !== 'object') {
      return body
    }

    // 后端 Result 三大件：{code, message, data}
    if ('code' in body && 'message' in body) {
      const { code, message, data } = body
      if (code === 1 || code === '1') {
        return data === undefined ? null : data
      }
      // code=1001/1002 未登录 & token 过期 → 视同 401
      if (code === 1001 || code === 1002) {
        const user = useUserStore()
        user.logout()
        ElMessage.error(message || '登录已失效，请重新登录')
        router.push('/login').catch(() => {})
        return Promise.reject({ code, message: message || '未登录' })
      }
      ElMessage.error(message || '请求失败')
      return Promise.reject({ code, message: message || '请求失败' })
    }

    return body
  },
  (err) => {
    const user = useUserStore()
    if (err && err.response) {
      const { status, data } = err.response
      if (status === 401) {
        user.logout()
        ElMessage.error('登录已失效，请重新登录')
        router.push('/login').catch(() => {})
        return Promise.reject({ code: 1001, message: '未登录' })
      }
      const msg = (data && data.message) || httpStatusMessage(status) || err.message || '网络请求失败'
      ElMessage.error(msg)
      return Promise.reject({ code: status, message: msg })
    }
    ElMessage.error(err && err.message ? err.message : '网络异常，请检查网络')
    return Promise.reject(err)
  }
)

function httpStatusMessage(status) {
  switch (status) {
    case 400: return '请求参数错误'
    case 403: return '没有访问权限'
    case 404: return '请求的资源不存在'
    case 500: return '服务器内部错误'
    case 502: return '网关错误'
    case 503: return '服务不可用'
    case 504: return '网关超时'
    default: return null
  }
}

/* ============================================================
 * 辅助：下载文件（GET/POST，不经过 Result 解包）
 *   respType = 'blob' / 'arraybuffer'
 * 返回 { blob, fileName } 便于前端 a.click 下载
 * ============================================================ */
export async function downloadFile(config = {}) {
  const { method = 'GET', url, params, data, filenameHint } = config
  const resp = await request.request({
    method,
    url,
    params,
    data,
    responseType: 'blob'
  })
  const blob = resp.data instanceof Blob ? resp.data : new Blob([resp.data])
  // 从 Content-Disposition 取 filename=xxx（支持 filename*=UTF-8''xxx）
  let fileName = filenameHint || 'download'
  const cd = resp.headers && resp.headers['content-disposition']
  if (cd) {
    const m1 = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(cd)
    if (m1 && m1[1]) {
      try { fileName = decodeURIComponent(m1[1]) } catch (_) {}
    } else {
      const m2 = /filename\s*=\s*"?([^";]+)"?/i.exec(cd)
      if (m2 && m2[1]) fileName = m2[1]
    }
  }
  return { blob, fileName }
}

/**
 * 直接触发浏览器下载（便捷方式）
 */
export async function triggerDownload(config = {}) {
  const { blob, fileName } = await downloadFile(config)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export default request
