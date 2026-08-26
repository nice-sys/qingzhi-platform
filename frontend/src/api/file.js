import request, { triggerDownload } from './request'

/**
 * 文件模块（上传/秒传/下载）
 * 对齐：FileController -> /api/file/*
 */

/**
 * 上传文件（multipart/form-data）
 * ⚠️ 限频：同一用户 1 分钟最多 6 次（后端 RateLimit 控制）
 * POST /api/file/upload   body: FormData(file)
 * 返回：{ fileStorageId, fileName, fileSize, fileExt, fileHash, filePath, hitQuickUpload }
 */
export function uploadFile(file, onProgress) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/file/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

/**
 * 按 fileStorageId 下载文件（下载 UUID 命名文件；一般业务推荐使用 resource.downloadResource 下原始文件名）
 * GET /api/file/download/{id}
 */
export function downloadFileById(id, filenameHint) {
  return triggerDownload({
    method: 'GET',
    url: `/file/download/${id}`,
    filenameHint
  })
}

export default {
  uploadFile,
  downloadFileById
}
