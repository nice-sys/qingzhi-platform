import request from './request'

/**
 * 收藏模块（教师/学生）
 * 对齐：FavoriteController -> /api/favorite/*
 */

/**
 * 收藏资源
 * POST /api/favorite/add   body: { resourceId: Long }
 */
export function addFavorite(resourceId) {
  return request.post('/favorite/add', { resourceId })
}

/**
 * 取消收藏资源
 * POST /api/favorite/remove   body: { resourceId: Long }
 */
export function removeFavorite(resourceId) {
  return request.post('/favorite/remove', { resourceId })
}

/**
 * 检查某资源是否已被当前用户收藏
 * GET /api/favorite/check?resourceId=xxx
 * 返回：{ favorited: boolean }
 */
export function checkFavorite(resourceId) {
  return request.get('/favorite/check', { params: { resourceId } })
}

/**
 * 我的收藏列表（按收藏时间倒序分页）
 * GET /api/favorite/my
 * @param {Object} params { keyword, course, pageNum, pageSize, page, size }
 */
export function myFavorites(params = {}) {
  const p = translatePage(params)
  return request.get('/favorite/my', { params: p })
}
/** 别名：便于在 views 中统一命名（listXXX/分页） */
export function listMyFavorites(params = {}) { return myFavorites(params) }

export default {
  addFavorite,
  removeFavorite,
  checkFavorite,
  myFavorites,
  listMyFavorites
}

/** 统一：前端用 {page,size}，后端用 {pageNum,pageSize} */
function translatePage(p = {}) {
  const out = { ...p }
  if (typeof out.page === 'number' && out.pageNum === undefined) out.pageNum = out.page
  if (typeof out.size === 'number' && out.pageSize === undefined) out.pageSize = out.size
  return out
}
