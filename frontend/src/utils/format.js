import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

dayjs.locale('zh-cn')

export { dayjs }

/**
 * 文件大小格式化：B -> KB/MB/GB，保留 2 位小数
 */
export function formatFileSize(bytes) {
  if (bytes == null || isNaN(bytes) || bytes < 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  const units = ['KB', 'MB', 'GB', 'TB']
  let size = bytes / 1024
  let i = 0
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(2) + ' ' + units[i]
}

/**
 * 日期时间格式化 YYYY-MM-DD HH:mm:ss
 */
export function formatDateTime(val, fallback = '-') {
  if (!val) return fallback
  const d = dayjs(val)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : fallback
}

/**
 * 日期格式化 YYYY-MM-DD
 */
export function formatDate(val, fallback = '-') {
  if (!val) return fallback
  const d = dayjs(val)
  return d.isValid() ? d.format('YYYY-MM-DD') : fallback
}

/**
 * 相对时间（刚刚 / N 分钟前 / N 小时前 / N 天前）
 */
export function formatRelative(val, fallback = '-') {
  if (!val) return fallback
  const d = dayjs(val)
  if (!d.isValid()) return fallback
  const diffSec = dayjs().diff(d, 'second')
  if (diffSec < 60) return '刚刚'
  if (diffSec < 3600) return Math.floor(diffSec / 60) + ' 分钟前'
  if (diffSec < 86400) return Math.floor(diffSec / 3600) + ' 小时前'
  if (diffSec < 86400 * 7) return Math.floor(diffSec / 86400) + ' 天前'
  return formatDate(val)
}

/**
 * 下载次数格式化（>9999 显示为 x.x 万）
 */
export function formatDownloadCount(n) {
  if (n == null || isNaN(n)) return 0
  if (n < 10000) return String(n)
  return (n / 10000).toFixed(1) + '万'
}
