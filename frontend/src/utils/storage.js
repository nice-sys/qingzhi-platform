/**
 * localStorage 封装（带 JSON 序列化 + 过期时间）
 */

const PREFIX = 'qz_'

function fullKey(key) {
  return PREFIX + key
}

export const storage = {
  set(key, value, expireSec = 0) {
    const wrap = {
      v: value,
      e: expireSec > 0 ? Date.now() + expireSec * 1000 : 0
    }
    try {
      localStorage.setItem(fullKey(key), JSON.stringify(wrap))
    } catch (e) {
      console.warn('[storage] set failed:', key, e)
    }
  },

  get(key, defaultValue = null) {
    try {
      const raw = localStorage.getItem(fullKey(key))
      if (!raw) return defaultValue
      const wrap = JSON.parse(raw)
      if (wrap.e > 0 && Date.now() > wrap.e) {
        localStorage.removeItem(fullKey(key))
        return defaultValue
      }
      return wrap.v
    } catch (e) {
      return defaultValue
    }
  },

  remove(key) {
    localStorage.removeItem(fullKey(key))
  },

  clearAll() {
    const keys = []
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k && k.startsWith(PREFIX)) keys.push(k)
    }
    keys.forEach(k => localStorage.removeItem(k))
  }
}

export default storage
