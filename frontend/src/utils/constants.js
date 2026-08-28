/**
 * 前端统一枚举常量
 * 集中管理：课程分类、资源类型、角色、审核状态、默认密码等
 * 各页面散落的硬编码（ResourceFilter/Publish/UserManage/等）逐步替换成本文件里的常量
 */

/* ============================================================
 * 1. 角色 role — 与后端 Constants.java ROLE_* 完全一致
 * ============================================================ */
export const ROLE = Object.freeze({
  ADMIN:   0,
  TEACHER: 1,
  STUDENT: 2
})
export const ROLE_NAME = Object.freeze({
  [ROLE.ADMIN]:   '管理员',
  [ROLE.TEACHER]: '教师',
  [ROLE.STUDENT]: '学生'
})
export const ROLE_OPTIONS = Object.freeze([
  { label: '管理员', value: ROLE.ADMIN },
  { label: '教师',   value: ROLE.TEACHER },
  { label: '学生',   value: ROLE.STUDENT }
])

/* ============================================================
 * 2. 审核状态 reviewStatus — 对齐后端 REVIEW_*
 * ============================================================ */
export const REVIEW = Object.freeze({
  PENDING: 0, // 待审核
  PASS:    1, // 已通过
  REJECT:  2  // 已拒绝
})
export const REVIEW_NAME = Object.freeze({
  [REVIEW.PENDING]: '待审核',
  [REVIEW.PASS]:    '已通过',
  [REVIEW.REJECT]:  '已拒绝'
})
export const REVIEW_TAG_TYPE = Object.freeze({
  [REVIEW.PENDING]: 'warning',
  [REVIEW.PASS]:    'success',
  [REVIEW.REJECT]:  'danger'
})

/* ============================================================
 * 3. 用户封禁状态（对齐 init.sql：0-正常 1-锁定）
 * ============================================================ */
export const USER_STATUS = Object.freeze({ NORMAL: 0, LOCKED: 1 })
export const USER_STATUS_NAME = Object.freeze({ 0: '正常', 1: '封禁' })

/* ============================================================
 * 4. 课程分类（先维护一份前端枚举，后续可改成从后端拉）
 * ============================================================ */
export const CATEGORIES = Object.freeze([
  '公共基础课',
  '计算机类',
  '电子信息类',
  '机械类',
  '外语类',
  '经管类',
  '文法类',
  '数理类',
  '医药类',
  '艺术类',
  '其他'
])
export const CATEGORY_OPTIONS = CATEGORIES.map((c) => ({ label: c, value: c }))

/* ============================================================
 * 5. 资源类型 type：扩展名小写
 * ============================================================ */
export const RESOURCE_TYPE = Object.freeze({
  PDF:     'pdf',
  DOC:     'doc',
  DOCX:    'docx',
  PPT:     'ppt',
  PPTX:    'pptx',
  XLS:     'xls',
  XLSX:    'xlsx',
  IMAGE:   'image',
  VIDEO:   'video',
  ARCHIVE: 'archive',
  OTHER:   'other'
})
export const RESOURCE_TYPE_LABEL = Object.freeze({
  [RESOURCE_TYPE.PDF]:     'PDF 文档',
  [RESOURCE_TYPE.DOC]:     'Word 文档',
  [RESOURCE_TYPE.DOCX]:    'Word 文档',
  [RESOURCE_TYPE.PPT]:     'PPT 演示稿',
  [RESOURCE_TYPE.PPTX]:    'PPT 演示稿',
  [RESOURCE_TYPE.XLS]:     'Excel 表格',
  [RESOURCE_TYPE.XLSX]:    'Excel 表格',
  [RESOURCE_TYPE.IMAGE]:   '图片',
  [RESOURCE_TYPE.VIDEO]:   '视频',
  [RESOURCE_TYPE.ARCHIVE]: '压缩包',
  [RESOURCE_TYPE.OTHER]:   '其他'
})
export const RESOURCE_TYPE_OPTIONS = Object.freeze([
  { label: 'PDF 文档',   value: 'pdf' },
  { label: 'Word 文档',  value: 'doc' },
  { label: 'PPT 演示稿', value: 'ppt' },
  { label: 'Excel 表格', value: 'xls' },
  { label: '图片',       value: 'image' },
  { label: '视频',       value: 'video' },
  { label: '压缩包',     value: 'archive' },
  { label: '其他',       value: 'other' }
])

/* ============================================================
 * 6. 文件扩展名 -> type 映射（上传/下载场景自动识别）
 * ============================================================ */
const EXT_TO_TYPE = {
  pdf: RESOURCE_TYPE.PDF,
  doc: RESOURCE_TYPE.DOC, docx: RESOURCE_TYPE.DOCX,
  ppt: RESOURCE_TYPE.PPT, pptx: RESOURCE_TYPE.PPTX,
  xls: RESOURCE_TYPE.XLS, xlsx: RESOURCE_TYPE.XLSX,
  jpg: RESOURCE_TYPE.IMAGE, jpeg: RESOURCE_TYPE.IMAGE,
  png: RESOURCE_TYPE.IMAGE, gif: RESOURCE_TYPE.IMAGE,
  bmp: RESOURCE_TYPE.IMAGE, webp: RESOURCE_TYPE.IMAGE,
  mp4: RESOURCE_TYPE.VIDEO, mov: RESOURCE_TYPE.VIDEO,
  avi: RESOURCE_TYPE.VIDEO, mkv: RESOURCE_TYPE.VIDEO,
  zip: RESOURCE_TYPE.ARCHIVE, rar: RESOURCE_TYPE.ARCHIVE,
  '7z': RESOURCE_TYPE.ARCHIVE, tar: RESOURCE_TYPE.ARCHIVE, gz: RESOURCE_TYPE.ARCHIVE
}
export function resolveTypeByFilename(name) {
  if (!name || !name.includes('.')) return RESOURCE_TYPE.OTHER
  const ext = name.split('.').pop().toLowerCase()
  return EXT_TO_TYPE[ext] || RESOURCE_TYPE.OTHER
}

/* ============================================================
 * 7. 资源类型 -> 图标/标签颜色
 * ============================================================ */
export const TYPE_TAG_TYPE = Object.freeze({
  [RESOURCE_TYPE.PDF]: 'danger',
  [RESOURCE_TYPE.DOC]: 'primary', [RESOURCE_TYPE.DOCX]: 'primary',
  [RESOURCE_TYPE.PPT]: 'warning', [RESOURCE_TYPE.PPTX]: 'warning',
  [RESOURCE_TYPE.XLS]: 'success', [RESOURCE_TYPE.XLSX]: 'success',
  [RESOURCE_TYPE.IMAGE]: 'info',
  [RESOURCE_TYPE.VIDEO]: 'warning',
  [RESOURCE_TYPE.ARCHIVE]: '',
  [RESOURCE_TYPE.OTHER]: 'info'
})
export const TYPE_ICON = Object.freeze({
  [RESOURCE_TYPE.PDF]:     'Document',
  [RESOURCE_TYPE.DOC]:     'Files',
  [RESOURCE_TYPE.DOCX]:    'Files',
  [RESOURCE_TYPE.PPT]:     'PictureFilled',
  [RESOURCE_TYPE.PPTX]:    'PictureFilled',
  [RESOURCE_TYPE.XLS]:     'Grid',
  [RESOURCE_TYPE.XLSX]:    'Grid',
  [RESOURCE_TYPE.IMAGE]:   'Picture',
  [RESOURCE_TYPE.VIDEO]:   'VideoCamera',
  [RESOURCE_TYPE.ARCHIVE]: 'Box',
  [RESOURCE_TYPE.OTHER]:   'Paperclip'
})

/* ============================================================
 * 8. 资源标签预设（发布页 allow-create select 默认项）
 * ============================================================ */
export const RESOURCE_TAG_PRESETS = Object.freeze([
  '期末复习', '考研', '课件PPT', '实验报告', '真题',
  '作业参考', '期中复习', '笔记', '课程设计', '毕业设计'
])
export const MAX_TAGS = 5

/* ============================================================
 * 9. 全局业务常量：重置默认密码 / 上传大小限制 / 限流提示
 * ============================================================ */
export const DEFAULT_ADMIN = Object.freeze({ username: 'Admin', password: 'Admin2026' })
export const RESET_DEFAULT_PASSWORD = 'Qz123456'
export const MAX_UPLOAD_SIZE = 200 * 1024 * 1024 // 200MB
export const EXCEL_IMPORT_MAX = 10 * 1024 * 1024  // 10MB

/* ============================================================
 * 10. Excel 导入模板列定义（顺序与后端保持一致）
 * ============================================================ */
export const EXCEL_TEMPLATE_COLUMNS = Object.freeze([
  { key: 'username',   label: '账号',   required: true,  example: '2023001001' },
  { key: 'password',   label: '密码',   required: true,  example: 'Qz123456' },
  { key: 'role',       label: '角色',   required: true,  example: '2（1=教师,2=学生）' },
  { key: 'name',       label: '姓名',   required: false, example: '张三' },
  { key: 'phone',      label: '手机',   required: false, example: '13800138000' },
  { key: 'email',      label: '邮箱',   required: false, example: 'zhangsan@edu.cn' },
  { key: 'department', label: '院系',   required: false, example: '计算机学院' },
  { key: 'major',      label: '专业/班级', required: false, example: '软件工程2301' }
])

export default {
  ROLE, ROLE_NAME, ROLE_OPTIONS,
  REVIEW, REVIEW_NAME, REVIEW_TAG_TYPE,
  USER_STATUS, USER_STATUS_NAME,
  CATEGORIES, CATEGORY_OPTIONS,
  RESOURCE_TYPE, RESOURCE_TYPE_LABEL, RESOURCE_TYPE_OPTIONS,
  resolveTypeByFilename, TYPE_TAG_TYPE, TYPE_ICON,
  RESOURCE_TAG_PRESETS, MAX_TAGS,
  DEFAULT_ADMIN, RESET_DEFAULT_PASSWORD, MAX_UPLOAD_SIZE, EXCEL_IMPORT_MAX,
  EXCEL_TEMPLATE_COLUMNS
}
