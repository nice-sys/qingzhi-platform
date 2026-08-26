import request, { triggerDownload } from './request'

/**
 * 资源模块（普通用户侧）
 * 对齐：ResourceController -> /api/resource/*
 */

/* 前后端分页字段翻译：前端统一用 {page,size}，后端约定 {pageNum,pageSize} */
function translatePage(p = {}) {
  const out = { ...p }
  if (typeof out.page === 'number' && out.pageNum === undefined) out.pageNum = out.page
  if (typeof out.size === 'number' && out.pageSize === undefined) out.pageSize = out.size
  return out
}

/**
 * 发布资源（教师/学生）→ 状态自动 0-待审核
 * POST /api/resource/publish
 */
export function publishResource(data = {}) {
  return request.post('/resource/publish', data)
}

/**
 * 修改资源（本人）
 * 前端调用：updateResource(id, data)
 * POST /api/resource/update   body: { id, ...fields }
 */
export function updateResource(idOrData, data = null) {
  const body = (typeof idOrData === 'object' && idOrData && data === null)
    ? idOrData
    : { id: idOrData, ...(data || {}) }
  return request.post('/resource/update', body)
}

/**
 * 删除资源（本人）
 * POST /api/resource/delete   body: { id: Long }
 */
export function deleteResource(id) {
  return request.post('/resource/delete', { id })
}

/**
 * 公开资源列表（仅已通过的资源，所有登录用户可见）
 * GET /api/resource/list
 */
export function listResource(params = {}) {
  return request.get('/resource/list', { params: translatePage(params) })
}
/** 别名：listResources（如有需要） */
export const listResources = listResource

/**
 * 资源详情（按可见性规则）
 * GET /api/resource/{id}
 */
export function getResourceDetail(id) {
  return request.get(`/resource/${id}`)
}
/** 别名：页面侧常用 resourceDetail(id) */
export function resourceDetail(id) { return getResourceDetail(id) }

/**
 * 下载资源（使用原始文件名）
 * GET /api/resource/{id}/download → 流
 */
export function downloadResource(id, filenameHint) {
  return triggerDownload({
    method: 'GET',
    url: `/resource/${id}/download`,
    filenameHint
  })
}

/**
 * 我的资源列表（本人发布的所有状态资源，分页）
 * GET /api/resource/my
 */
export function myResources(params = {}) {
  return request.get('/resource/my', { params: translatePage(params) })
}
/** 别名：listMyResources(pageQuery) */
export function listMyResources(params = {}) { return myResources(params) }

export default {
  publishResource,
  updateResource,
  deleteResource,
  listResource,
  listResources,
  getResourceDetail,
  resourceDetail,
  downloadResource,
  myResources,
  listMyResources
}
