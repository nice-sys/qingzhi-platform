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
 *  3. ⚠️ responseType='blob' 场景：若后端返回的仍是 JSON（BusinessException → Result.fail JSON），
 *     先把 Blob -> text -> JSON 解析，识别失败 reject 真实 message，避免最终下载 "undefined.txt"
 *  4. 其他 HTTP 错误 → 统一提示
 * ============================================================ */
request.interceptors.response.use(
  async (resp) => {
    const ct = resp.headers && resp.headers['content-type'] ? String(resp.headers['content-type']) : ''
    const respType = (resp.config && resp.config.responseType) || ''

    // ① responseType='blob' / 'arraybuffer'：先判断是不是后端返回了 JSON 错误
    if (respType === 'blob' || respType === 'arraybuffer') {
      // ct 明确是 JSON → 100% 是后端错误 Result JSON，读成文本解析
      if (ct.includes('application/json')) {
        try {
          const text = await blobToText(resp.data)
          let body
          try { body = JSON.parse(text) } catch (_) { body = null }
          if (body && typeof body === 'object' && 'code' in body && 'message' in body) {
            const { code, message, data: _d } = body
            if (code === 1 || code === '1') {
              return dataLikeResponse(_d, resp)
            }
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
        } catch (_parseErr) {
          // 解析失败，当普通 blob 返回
        }
      }
      // 非 JSON 或解析失败：直接返回整个 axios resp（downloadFile 会取 resp.data + resp.headers）
      return resp
    }

    // ② 非 JSON（如文件下载流直接走 respType 分支；这里兜底）直接返回 resp
    if (!ct.includes('application/json')) {
      return resp
    }

    const body = resp.data
    if (!body || typeof body !== 'object') {
      return body
    }

    // ③ 后端 Result 三大件：{code, message, data}
    if ('code' in body && 'message' in body) {
      const { code, message, data } = body
      if (code === 1 || code === '1') {
        // 兼容后端 PageResult：字段名为 records / pageNum / pageSize，
        // 但前端列表页统一读 d.list / d.total（total 名一致，只需补 list = records）
        if (data && typeof data === 'object' && Array.isArray(data.records)) {
          if (!('list' in data)) {
            try {
              Object.defineProperty(data, 'list', {
                get() { return this.records },
                enumerable: true,
                configurable: true
              })
            } catch (_) {
              data.list = data.records
            }
          }
        }
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
      // blob 场景下后端返回的是 JSON 错误，err.data 仍是 Blob，尝试解析文本
      let msg = (data && data.message) ? data.message : null
      if (!msg && data instanceof Blob) {
        blobToText(data).then(txt => {
          try { const b = JSON.parse(txt); if (b && b.message) ElMessage.error(b.message) } catch (_) {}
        }).catch(() => {})
      }
      if (!msg) msg = httpStatusMessage(status) || err.message || '网络请求失败'
      ElMessage.error(msg)
      return Promise.reject({ code: status, message: msg })
    }
    ElMessage.error(err && err.message ? err.message : '网络异常，请检查网络')
    return Promise.reject(err)
  }
)

/**
 * 把 Blob / ArrayBuffer 读成 UTF-8 文本（Promise 包装）
 */
function blobToText(blob) {
  return new Promise((resolve, reject) => {
    if (typeof Blob !== 'undefined' && blob instanceof Blob) {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result || ''))
      reader.onerror = () => reject(reader.error || new Error('blob read failed'))
      reader.readAsText(blob, 'utf-8')
      return
    }
    // 兜底：若不是 Blob（比如本来就是字符串）
    try { resolve(String(blob ?? '')) } catch (e) { reject(e) }
  })
}

/**
 * blob 场景解包 data：保持和正常响应一致，但把 headers 藏在私有属性里供 downloadFile 读取
 */
function dataLikeResponse(data, resp) {
  if (data && typeof data === 'object' && resp && resp.headers) {
    try { Object.defineProperty(data, '__respHeaders', { value: resp.headers, enumerable: false, configurable: true }) } catch (_) {}
  }
  return data
}

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
 * 辅助：下载文件（GET/POST，统一经过响应拦截器）
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

  // resp 可能是 3 种类型：
  //   A) 整个 axios response 对象（非 JSON 正常流 / 错误 JSON 不匹配）→ 取 resp.data + resp.headers
  //   B) Blob / ArrayBuffer（直接返回的 body）→ 直接当 blob
  //   C) 普通对象（dataLikeResponse 包装过的）→ 理论上不会进入触发下载，直接抛错
  let blob
  if (resp instanceof Blob || (typeof ArrayBuffer !== 'undefined' && resp instanceof ArrayBuffer)) {
    blob = resp instanceof Blob ? resp : new Blob([resp])
  } else if (resp && resp.data instanceof Blob) {
    blob = resp.data
  } else if (resp && typeof resp.data !== 'undefined' && resp.data !== null) {
    blob = new Blob([resp.data])
  } else {
    // 兜底：如果连 data 都没有（比如拦截器返回了奇怪的东西），内容至少不是 "undefined" 这个字符串
    blob = new Blob([typeof resp === 'string' ? resp : (resp ? JSON.stringify(resp) : '')])
  }

  // 从 Content-Disposition 取 filename=xxx（支持 filename*=UTF-8''xxx）
  let fileName = filenameHint || 'download'
  const headers = (resp && resp.headers) || (resp && resp.__respHeaders) || null
  const cd = headers && headers['content-disposition']
  if (cd) {
    const m1 = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(cd)
    if (m1 && m1[1]) {
      try { fileName = decodeURIComponent(m1[1]) } catch (_) {}
    } else {
      const m2 = /filename\s*=\s*"?([^";]+)"?/i.exec(cd)
      if (m2 && m2[1]) fileName = m2[1]
    }
  }

  // 文件名无扩展名 + filenameHint 带扩展名时，兜底使用 hint（优先保留响应里返回的文件名）
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
