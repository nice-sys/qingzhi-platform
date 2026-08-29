package com.qingzhi.demo.service.impl;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.entity.DailyUploadCount;
import com.qingzhi.demo.entity.FileStorage;
import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.enums.ReviewStatusEnum;
import com.qingzhi.demo.enums.RoleEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.mapper.DailyUploadCountMapper;
import com.qingzhi.demo.mapper.ResourceMapper;
import com.qingzhi.demo.mapper.UserMapper;
import com.qingzhi.demo.service.FileService;
import com.qingzhi.demo.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 资源服务实现（普通用户侧）
 * <p>对应 PRD 2.3 资源管理模块。
 */
@Service
public class ResourceServiceImpl implements ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceImpl.class);

    private final ResourceMapper resourceMapper;
    private final FileService fileService;
    private final UserMapper userMapper;
    private final DailyUploadCountMapper dailyUploadCountMapper;

    public ResourceServiceImpl(ResourceMapper resourceMapper, FileService fileService,
                               UserMapper userMapper, DailyUploadCountMapper dailyUploadCountMapper) {
        this.resourceMapper = resourceMapper;
        this.fileService = fileService;
        this.userMapper = userMapper;
        this.dailyUploadCountMapper = dailyUploadCountMapper;
    }

    /* ====================================================================================
     * 公共工具：保证 NOT NULL 列（course/fileName/filePath/fileSize/fileExt/fileHash）永不为 null
     * - resource.course / file_name / file_path 三列 DB NOT NULL 且无 DEFAULT
     * - fileSize / fileExt / fileHash 虽然 DB 允许 NULL，但统一设默认值避免前端显示 null
     * - description 是 TEXT 类型允许 NULL，但为了避免后续 updateById 空判断歧义也统一为空串
     * ==================================================================================== */
    private static void ensureNotNullColumns(Resource r) {
        if (r == null) return;
        if (r.getCourse()     == null) r.setCourse("");
        if (r.getFileName()   == null) r.setFileName("");
        if (r.getFilePath()   == null) r.setFilePath("");
        if (r.getFileExt()    == null) r.setFileExt("");
        if (r.getFileHash()   == null) r.setFileHash("");
        if (r.getFileSize()   == null) r.setFileSize(0L);
        if (r.getDescription()== null) r.setDescription("");
    }

    /* ====================================================================================
     * 每日上传配额扣减（核心限流）
     * - 单用户每天最多 DAILY_UPLOAD_MAX_COUNT（100）条 resource 行（正式+草稿合并计数）
     * - 用 UNIQUE(user_id, upload_date) + SELECT ... FOR UPDATE 行锁保证并发严格串行化，
     *   杜绝并发 2 请求都读到 count=99 然后同时 +1 穿数到 101 的问题
     * - 管理员暂不豁免（需求未指定）；如需豁免后续只要在此方法开头判断 role 即可
     * ==================================================================================== */
    private void consumeDailyUploadQuota(Long userId) {
        if (userId == null) return;
        final LocalDate today = LocalDate.now();

        // 1. 先加行级锁查询当天计数（若记录已存在）
        DailyUploadCount row = dailyUploadCountMapper.selectByUserAndDateForUpdate(userId, today);
        if (row != null) {
            // 已存在：先判断当前计数是否已达上限；是则直接拒绝，否则原子 +1
            int cur = row.getUploadCount() == null ? 0 : row.getUploadCount();
            if (cur >= Constants.DAILY_UPLOAD_MAX_COUNT) {
                log.warn("[每日上传超限] userId={} date={} cur={} limit={}",
                        userId, today, cur, Constants.DAILY_UPLOAD_MAX_COUNT);
                throw new BusinessException(ResponseCodeEnum.DAILY_UPLOAD_LIMIT_EXCEEDED);
            }
            int rows = dailyUploadCountMapper.incrementCountById(row.getId());
            if (rows <= 0) {
                throw new BusinessException(ResponseCodeEnum.FAILURE, "每日计数更新失败，请重试");
            }
            log.debug("[每日上传扣减] userId={} date={} after={}", userId, today, cur + 1);
            return;
        }

        // 2. 当天无记录：新建一条 upload_count = 1（首条配额）
        //    并发场景下若两个事务同时走到这里插入，UNIQUE 索引会让后提交者抛 DuplicateKeyException；
        //    捕获后重试一次：重新 SELECT FOR UPDATE 拿到对端插入的行，按 "已存在" 分支再判断
        DailyUploadCount insert = new DailyUploadCount();
        insert.setUserId(userId);
        insert.setUploadDate(today);
        try {
            int rows = dailyUploadCountMapper.insertInitial(insert);
            if (rows > 0 && insert.getId() != null) {
                log.debug("[每日上传扣减] 新建记录 userId={} date={} count=1", userId, today);
                return;
            }
            throw new BusinessException(ResponseCodeEnum.FAILURE, "每日计数初始化失败，请重试");
        } catch (DuplicateKeyException dup) {
            // 并发冲突：另一个事务先插入成功 → 重查一次 + 正常扣减
            DailyUploadCount retry = dailyUploadCountMapper.selectByUserAndDateForUpdate(userId, today);
            if (retry == null) {
                throw new BusinessException(ResponseCodeEnum.FAILURE, "每日计数冲突重试失败，请重试");
            }
            int cur = retry.getUploadCount() == null ? 0 : retry.getUploadCount();
            if (cur >= Constants.DAILY_UPLOAD_MAX_COUNT) {
                log.warn("[每日上传超限-冲突重试] userId={} date={} cur={}", userId, today, cur);
                throw new BusinessException(ResponseCodeEnum.DAILY_UPLOAD_LIMIT_EXCEEDED);
            }
            int rows = dailyUploadCountMapper.incrementCountById(retry.getId());
            if (rows <= 0) {
                throw new BusinessException(ResponseCodeEnum.FAILURE, "每日计数重试更新失败，请重试");
            }
            log.debug("[每日上传扣减-冲突重试] userId={} date={} after={}", userId, today, cur + 1);
        }
    }

    /* ====================================================================================
     * 一、发布资源
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishResource(Resource resourceDto, Long uploaderId) {
        // 1. 基础校验：当前用户、必填字段
        BusinessException.throwIfNull(uploaderId, ResponseCodeEnum.UNAUTHORIZED);
        // 2. ✅ 每日上传限流（草稿+正式统一计数）：在事务最开头扣配额，防止后续文件引用已 +1 后才发现超限浪费引用
        consumeDailyUploadQuota(uploaderId);

        BusinessException.throwIfBlank(resourceDto.getTitle(),
                ResponseCodeEnum.PARAM_ERROR, "资源标题不能为空");
        BusinessException.throwIf(resourceDto.getTitle().length() > Constants.RESOURCE_TITLE_MAX_LENGTH,
                ResponseCodeEnum.PARAM_ERROR,
                "资源标题长度不能超过 " + Constants.RESOURCE_TITLE_MAX_LENGTH);
        BusinessException.throwIfBlank(resourceDto.getCourse(),
                ResponseCodeEnum.PARAM_ERROR, "所属课程不能为空");
        BusinessException.throwIf(resourceDto.getCourse().length() > Constants.COURSE_MAX_LENGTH,
                ResponseCodeEnum.PARAM_ERROR,
                "课程名称长度不能超过 " + Constants.COURSE_MAX_LENGTH);

        // 2. 文件信息补全（支持两种方式：A.传 fileStorageId，B.前端已传 fileName/filePath/fileSize/fileExt/fileHash）
        // 只要 filePath 或 fileName 任一个为空，且 fileStorageId 有值且 > 0，就用 fileStorageId 查 FileStorage 表回填
        Long storageId = resourceDto.getFileStorageId();
        if ((!StringUtils.hasText(resourceDto.getFilePath())
                || !StringUtils.hasText(resourceDto.getFileName()))
                && storageId != null && storageId > 0) {
            FileStorage fs = fileService.getFileStorageById(storageId);
            BusinessException.throwIfNull(fs, ResponseCodeEnum.FILE_NOT_FOUND,
                    "关联的文件不存在(fileStorageId=" + storageId + ")");
            // 回填 6 个文件字段
            if (!StringUtils.hasText(resourceDto.getFileName())) {
                resourceDto.setFileName(fs.getOriginalFileName());
            }
            if (!StringUtils.hasText(resourceDto.getFilePath())) {
                resourceDto.setFilePath(fs.getFilePath());
            }
            if (resourceDto.getFileSize() == null) {
                resourceDto.setFileSize(fs.getFileSize());
            }
            if (!StringUtils.hasText(resourceDto.getFileExt())) {
                resourceDto.setFileExt(fs.getFileExt());
            }
            if (!StringUtils.hasText(resourceDto.getFileHash())) {
                resourceDto.setFileHash(fs.getFileHash());
            }
        }

        // 2b. ✅ 关键：只要 form 里有有效 fileStorageId，就给引用计数 +1
        // （草稿 saveDraft 已持有 reference=1；新建正式资源复用同 fileStorage → 引用计数必须 >=2；
        //  后续即使前端 deleteDraft 调了 releaseReference，减 1 后至少是 1，不会误删磁盘文件）
        Long finalStorageId = (resourceDto.getFileStorageId() != null && resourceDto.getFileStorageId() > 0)
                ? resourceDto.getFileStorageId()
                : null;
        if (finalStorageId != null) {
            // 再次校验存在性，避免非法 id
            FileStorage fs = fileService.getFileStorageById(finalStorageId);
            if (fs == null) {
                BusinessException.throwOf(ResponseCodeEnum.FILE_NOT_FOUND,
                        "关联的文件不存在(fileStorageId=" + finalStorageId + ")");
            }
            fileService.increaseReference(finalStorageId);
        }

        // 3. 最终文件校验（filePath + fileName 必传）
        BusinessException.throwIfBlank(resourceDto.getFilePath(),
                ResponseCodeEnum.PARAM_ERROR, "请先上传文件后再发布资源（filePath 必填，或传 fileStorageId 自动关联）");
        BusinessException.throwIfBlank(resourceDto.getFileName(),
                ResponseCodeEnum.PARAM_ERROR, "文件名不能为空");
        BusinessException.throwIf(!StringUtils.hasText(resourceDto.getFilePath())
                || resourceDto.getFilePath().length() > Constants.FILE_PATH_MAX_LENGTH,
                ResponseCodeEnum.PARAM_ERROR, "文件路径长度超限");

        // 3. 组装入库对象：强制 review_status = 0（待审核）
        Resource insert = new Resource();
        insert.setTitle(resourceDto.getTitle().trim());
        insert.setDescription(resourceDto.getDescription());
        insert.setCourse(resourceDto.getCourse().trim());
        insert.setUploaderId(uploaderId);
        insert.setFileName(resourceDto.getFileName());
        insert.setFilePath(resourceDto.getFilePath());
        insert.setFileSize(resourceDto.getFileSize());
        insert.setFileExt(resourceDto.getFileExt());
        insert.setFileHash(resourceDto.getFileHash());
        insert.setDownloadCount(0);
        insert.setReviewStatus(ReviewStatusEnum.PENDING.getCode()); // 默认待审核
        insert.setRejectReason(null);
        insert.setReviewAdminId(null);
        insert.setReviewTime(null);
        ensureNotNullColumns(insert);   // ✅ 数据库 NOT NULL 列强制兜底（永不为 null → <if test="x!=null"> 一定命中）

        int rows = resourceMapper.insert(insert);
        BusinessException.throwIf(rows <= 0 || insert.getId() == null,
                ResponseCodeEnum.RESOURCE_NOT_FOUND, "资源发布失败，请重试");

        log.info("资源发布成功：resourceId={}, uploaderId={}, title={}, course={}",
                insert.getId(), uploaderId, insert.getTitle(), insert.getCourse());

        return insert.getId();
    }

    /* ====================================================================================
     * 二、修改资源
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateResource(Resource resourceDto, Long operatorId) {
        BusinessException.throwIfNull(operatorId, ResponseCodeEnum.UNAUTHORIZED);
        Long resourceId = resourceDto.getId();
        BusinessException.throwIfNull(resourceId,
                ResponseCodeEnum.RESOURCE_NOT_FOUND, "资源ID不能为空");

        // 1. 加载现有资源，校验本人是否有权限修改
        Resource existing = resourceMapper.selectById(resourceId);
        BusinessException.throwIfNull(existing,
                ResponseCodeEnum.RESOURCE_NOT_FOUND);
        BusinessException.throwIf(!existing.getUploaderId().equals(operatorId),
                ResponseCodeEnum.PERMISSION_DENIED, "只能修改自己发布的资源");

        // 2. 入参字段长度校验（只校验非空的，因为 updateById 是动态更新）
        if (resourceDto.getTitle() != null) {
            String t = resourceDto.getTitle().trim();
            BusinessException.throwIf(t.isEmpty(),
                    ResponseCodeEnum.PARAM_ERROR, "资源标题不能为空字符串");
            BusinessException.throwIf(t.length() > Constants.RESOURCE_TITLE_MAX_LENGTH,
                    ResponseCodeEnum.PARAM_ERROR,
                    "资源标题长度不能超过 " + Constants.RESOURCE_TITLE_MAX_LENGTH);
            resourceDto.setTitle(t);
        }
        if (resourceDto.getCourse() != null) {
            String c = resourceDto.getCourse().trim();
            BusinessException.throwIf(c.isEmpty(),
                    ResponseCodeEnum.PARAM_ERROR, "所属课程不能为空字符串");
            BusinessException.throwIf(c.length() > Constants.COURSE_MAX_LENGTH,
                    ResponseCodeEnum.PARAM_ERROR,
                    "课程名称长度不能超过 " + Constants.COURSE_MAX_LENGTH);
            resourceDto.setCourse(c);
        }

        // 3. 状态回退逻辑：PRD 2.3.2 —— 已通过(1)的资源被修改后，自动回退为待审核(0)，重新审核
        Resource update = new Resource();
        update.setId(resourceId);
        update.setTitle(resourceDto.getTitle());
        update.setDescription(resourceDto.getDescription());
        update.setCourse(resourceDto.getCourse());
        update.setFileName(resourceDto.getFileName());
        update.setFilePath(resourceDto.getFilePath());
        update.setFileSize(resourceDto.getFileSize());
        update.setFileExt(resourceDto.getFileExt());
        update.setFileHash(resourceDto.getFileHash());

        if (ReviewStatusEnum.APPROVED.getCode() == (existing.getReviewStatus() == null ? -1 : existing.getReviewStatus())) {
            update.setReviewStatus(ReviewStatusEnum.PENDING.getCode());
            update.setRejectReason(null);       // 清空之前的拒绝理由（如果有的话，因为已通过不会有，但拒绝状态改到通过后的修改同样清空）
            update.setReviewAdminId(null);
            update.setReviewTime(null);
            log.info("修改已通过资源：resourceId={}，状态自动回退为【待审核】", resourceId);
        } else if (ReviewStatusEnum.REJECTED.getCode() == (existing.getReviewStatus() == null ? -1 : existing.getReviewStatus())) {
            // 拒绝状态用户修改后，也重置为待审核（允许用户修正后重新提交），清空拒绝理由
            update.setReviewStatus(ReviewStatusEnum.PENDING.getCode());
            update.setRejectReason(null);
            update.setReviewAdminId(null);
            update.setReviewTime(null);
            log.info("修改已拒绝资源：resourceId={}，重置为【待审核】重新提交", resourceId);
        }
        // 待审核(0)状态下用户修改：不改变状态，管理员仍按顺序审核

        int rows = resourceMapper.updateById(update);
        if (rows > 0) {
            log.info("资源修改成功：resourceId={}, operatorId={}", resourceId, operatorId);
        }
        return rows > 0;
    }

    /* ====================================================================================
     * 三、删除我的资源（同步释放文件引用）
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyResource(Long resourceId, Long operatorId) {
        BusinessException.throwIfNull(operatorId, ResponseCodeEnum.UNAUTHORIZED);
        BusinessException.throwIfNull(resourceId, ResponseCodeEnum.RESOURCE_NOT_FOUND);

        Resource existing = resourceMapper.selectById(resourceId);
        BusinessException.throwIfNull(existing, ResponseCodeEnum.RESOURCE_NOT_FOUND);
        BusinessException.throwIf(!existing.getUploaderId().equals(operatorId),
                ResponseCodeEnum.PERMISSION_DENIED, "只能删除自己发布的资源");

        // 1. 先删资源行
        int rows = resourceMapper.deleteById(resourceId);
        BusinessException.throwIf(rows <= 0,
                ResponseCodeEnum.RESOURCE_NOT_FOUND, "资源删除失败");

        // 2. 释放文件引用（引用计数 -1；归零则删盘）
        if (existing.getFileHash() != null && !existing.getFileHash().isEmpty()) {
            FileStorage storage = fileService.getFileStorageByHash(existing.getFileHash());
            if (storage != null) {
                fileService.releaseReference(storage.getId());
            }
        }

        log.info("用户删除自己的资源：resourceId={}, operatorId={}", resourceId, operatorId);
    }

    /* ====================================================================================
     * 四、已通过资源列表（公开列表）
     * ==================================================================================== */

    @Override
    public PageResult<Resource> listApprovedResources(String keyword, String course,
                                                      LocalDate startDate, LocalDate endDate,
                                                      Integer pageNum, Integer pageSize) {
        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);

        // 公开列表：固定 reviewStatus = 已通过(1)；不按 uploaderId 过滤
        long total = resourceMapper.countResources(
                normalizeLikeKeyword(keyword),
                blankToNull(course),
                ReviewStatusEnum.APPROVED.getCode(),
                null,
                startDate,
                endDate);

        if (total <= 0) {
            return PageResult.empty(pageNum, pageSize);
        }
        int offset = (pageNum - 1) * pageSize;
        List<Resource> records = resourceMapper.selectResourcesPage(
                normalizeLikeKeyword(keyword),
                blankToNull(course),
                ReviewStatusEnum.APPROVED.getCode(),
                null,
                startDate,
                endDate,
                offset,
                pageSize);
        return PageResult.of(records == null ? Collections.emptyList() : records, pageNum, pageSize, total);
    }

    /* ====================================================================================
     * 五、资源详情（含可见性控制）
     * ==================================================================================== */

    @Override
    public Resource getResourceDetail(Long resourceId, Long viewerId, Integer viewerRole) {
        BusinessException.throwIfNull(resourceId,
                ResponseCodeEnum.RESOURCE_NOT_FOUND, "资源ID不能为空");

        Resource res = resourceMapper.selectById(resourceId);
        BusinessException.throwIfNull(res, ResponseCodeEnum.RESOURCE_NOT_FOUND);

        // 可见性控制
        ReviewStatusEnum status = ReviewStatusEnum.of(res.getReviewStatus());
        if (ReviewStatusEnum.APPROVED == status) {
            // 已通过：任何登录用户可见（未登录也可见可在 Controller 层放开；这里不强制，但 JWT 拦截已要求登录）
            return res;
        }

        // 待审核 / 已拒绝：仅上传者本人 或 管理员 可见
        if (viewerId != null && viewerId.equals(res.getUploaderId())) {
            return res;
        }
        if (viewerRole != null && RoleEnum.ADMIN.getCode() == viewerRole) {
            return res;
        }

        throw new BusinessException(ResponseCodeEnum.PERMISSION_DENIED.getCode(),
                "您没有权限查看该资源（尚未通过审核）");
    }

    /* ====================================================================================
     * 六、我的资源列表
     * ==================================================================================== */

    @Override
    public PageResult<Resource> listMyResources(Long uploaderId,
                                                String keyword, String course, Integer reviewStatus,
                                                LocalDate startDate, LocalDate endDate,
                                                Integer pageNum, Integer pageSize) {
        BusinessException.throwIfNull(uploaderId, ResponseCodeEnum.UNAUTHORIZED);
        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);

        // reviewStatus 合法性校验（可选传）
        if (reviewStatus != null && !ReviewStatusEnum.isValid(reviewStatus)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR.getCode(), "非法的审核状态值");
        }

        // ⚠️ 关键：我的资源列表默认排除草稿（DRAFT=3），草稿专属入口是 /profile/drafts
        //    只有当用户显式传 reviewStatus=3 时才查询草稿（一般前端不会传）
        int draftCode = ReviewStatusEnum.DRAFT.getCode();
        Integer excludeDraft = (reviewStatus == null
                || draftCode != (reviewStatus == null ? -1 : reviewStatus))
                ? draftCode : null;

        long total = resourceMapper.countResourcesExcludeStatus(
                normalizeLikeKeyword(keyword),
                blankToNull(course),
                reviewStatus,
                excludeDraft,
                uploaderId,
                startDate,
                endDate);

        if (total <= 0) {
            return PageResult.empty(pageNum, pageSize);
        }

        int offset = (pageNum - 1) * pageSize;
        List<Resource> records = resourceMapper.selectResourcesPageExcludeStatus(
                normalizeLikeKeyword(keyword),
                blankToNull(course),
                reviewStatus,
                excludeDraft,
                uploaderId,
                startDate,
                endDate,
                offset,
                pageSize);
        return PageResult.of(records == null ? Collections.emptyList() : records, pageNum, pageSize, total);
    }

    /* ====================================================================================
     * 七、下载资源（可见性校验 + 原子自增下载计数）
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Resource downloadResource(Long resourceId, Long viewerId, Integer viewerRole) {
        // 1. 校验权限 & 加载资源（与 getResourceDetail 同可见性规则 + 同异常）
        Resource res = getResourceDetail(resourceId, viewerId, viewerRole);

        // 2. 校验文件路径非空（防止 DB 脏数据或未发布成功的资源被直接下载）
        BusinessException.throwIfBlank(res.getFilePath(),
                ResponseCodeEnum.FILE_NOT_FOUND, "该资源尚未上传文件，无法下载");
        BusinessException.throwIfBlank(res.getFileName(),
                ResponseCodeEnum.FILE_NOT_FOUND, "资源文件名缺失，请联系管理员");

        // 3. 原子自增 download_count（行锁避免并发丢失更新）
        int rows = resourceMapper.incrementDownloadCount(resourceId);
        BusinessException.throwIf(rows <= 0,
                ResponseCodeEnum.FAILURE, "下载失败，请稍后重试");

        // 4. 将 DB 中的值同步 +1，返回给 Controller（即便 Controller 用不到，也保持 entity 一致性）
        Integer old = res.getDownloadCount() == null ? 0 : res.getDownloadCount();
        res.setDownloadCount(old + 1);

        log.info("资源下载成功：resourceId={}, viewerId={}, newDownloadCount={}",
                resourceId, viewerId, res.getDownloadCount());
        return res;
    }

    /* ====================================================================================
     * 私有辅助
     * ==================================================================================== */

    private static Integer normalizePageNum(Integer pageNum) {
        return (pageNum == null || pageNum < 1) ? Constants.DEFAULT_PAGE_NUM : pageNum;
    }

    private static Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return Constants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, Constants.MAX_PAGE_SIZE);
    }

    private static String normalizeLikeKeyword(String keyword) {
        if (keyword == null) return null;
        String k = keyword.trim();
        return k.isEmpty() ? null : k;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /* ====================================================================================
     * 八、首页 Dashboard 聚合统计
     * ==================================================================================== */

    @Override
    public Map<String, Object> getDashboardStats() {
        // 1. 资源侧聚合（资源总数 / 已通过 / 待审核 / 已拒绝 / 下载总量）
        Map<String, Object> rs = resourceMapper.selectDashboardStats();
        Map<String, Object> out = new HashMap<>();
        out.put("resourceTotal",    toLong(rs == null ? null : rs.get("resourceTotal")));
        out.put("approvedCount",    toLong(rs == null ? null : rs.get("approvedCount")));
        out.put("pendingCount",     toLong(rs == null ? null : rs.get("pendingCount")));
        out.put("rejectedCount",    toLong(rs == null ? null : rs.get("rejectedCount")));
        out.put("downloadTotal",    toLong(rs == null ? null : rs.get("downloadTotal")));
        out.put("todayDownloadCount", toLong(rs == null ? null : rs.get("todayDownloadCount")));

        // 2. 用户侧
        out.put("userCount", userMapper.countTotalUsers());
        return out;
    }

    /** Number → long（处理 MySQL COUNT/SUM 返回的 Long/BigInteger/BigDecimal/Number 各种情形） */
    private static long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) {
            if (v instanceof BigInteger) return ((BigInteger) v).longValue();
            if (v instanceof BigDecimal) return ((BigDecimal) v).longValue();
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ignore) {
            return 0L;
        }
    }

    /* ====================================================================================
     * 四、草稿箱功能（reviewStatus = DRAFT=3）
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(Resource body, Long operatorId) {
        BusinessException.throwIfNull(operatorId, ResponseCodeEnum.UNAUTHORIZED);

        // 1. 如果传了 fileStorageId（有效且 > 0），同样回填文件 6 字段（与 publishResource 相同逻辑，但不强制）
        Long sId = body == null ? null : body.getFileStorageId();
        if (body != null
                && sId != null && sId > 0
                && (!StringUtils.hasText(body.getFilePath()) || !StringUtils.hasText(body.getFileName()))) {
            FileStorage fs = fileService.getFileStorageById(sId);
            if (fs != null) {
                if (!StringUtils.hasText(body.getFileName())) body.setFileName(fs.getOriginalFileName());
                if (!StringUtils.hasText(body.getFilePath())) body.setFilePath(fs.getFilePath());
                if (body.getFileSize() == null) body.setFileSize(fs.getFileSize());
                if (!StringUtils.hasText(body.getFileExt())) body.setFileExt(fs.getFileExt());
                if (!StringUtils.hasText(body.getFileHash())) body.setFileHash(fs.getFileHash());
            }
        }

        // 2. 字段长度软校验：超长才报错，空内容允许保存
        if (body != null && body.getTitle() != null) {
            String t = body.getTitle().trim();
            BusinessException.throwIf(t.length() > Constants.RESOURCE_TITLE_MAX_LENGTH,
                    ResponseCodeEnum.PARAM_ERROR, "草稿标题超长");
            body.setTitle(t);
        }
        if (body != null && body.getCourse() != null) {
            String c = body.getCourse().trim();
            BusinessException.throwIf(c.length() > Constants.COURSE_MAX_LENGTH,
                    ResponseCodeEnum.PARAM_ERROR, "草稿分类超长");
            body.setCourse(c);
        }

        Long draftId = body == null ? null : body.getId();
        if (draftId != null) {
            // 3. 更新草稿：必须是本人草稿 + 状态必须是 DRAFT
            Resource existing = resourceMapper.selectById(draftId);
            BusinessException.throwIfNull(existing, ResponseCodeEnum.RESOURCE_NOT_FOUND, "草稿不存在");
            BusinessException.throwIf(!existing.getUploaderId().equals(operatorId),
                    ResponseCodeEnum.PERMISSION_DENIED, "只能修改自己的草稿");
            BusinessException.throwIf(ReviewStatusEnum.DRAFT.getCode() != (existing.getReviewStatus() == null ? -1 : existing.getReviewStatus()),
                    ResponseCodeEnum.PERMISSION_DENIED, "该资源不是草稿状态，无法通过草稿接口修改");

            Resource update = new Resource();
            update.setId(draftId);
            if (body != null) {
                update.setTitle(body.getTitle());
                update.setDescription(body.getDescription());
                update.setCourse(body.getCourse());
                update.setTags(body.getTags());
                update.setFileName(body.getFileName());
                update.setFilePath(body.getFilePath());
                update.setFileSize(body.getFileSize());
                update.setFileExt(body.getFileExt());
                update.setFileHash(body.getFileHash());
                update.setFileStorageId(body.getFileStorageId());
            }
            ensureNotNullColumns(update);    // ✅ NOT NULL 列强制兜底（动态 SET 里 if!=null 一定命中）
            int rows = resourceMapper.updateById(update);
            BusinessException.throwIf(rows <= 0, ResponseCodeEnum.FAILURE, "草稿保存失败");
            log.info("草稿更新成功：draftId={}, operatorId={}", draftId, operatorId);
            return draftId;
        } else {
            // 4. 新建草稿：insert 新记录，reviewStatus=DRAFT
            // 4b. ✅ 每日上传配额扣减（更新旧草稿不扣，新建扣 1）
            consumeDailyUploadQuota(operatorId);

            Resource insert = new Resource();
            if (body != null) {
                insert.setTitle(body.getTitle());
                insert.setDescription(body.getDescription());
                insert.setCourse(body.getCourse());
                insert.setTags(body.getTags());
                insert.setFileName(body.getFileName());
                insert.setFilePath(body.getFilePath());
                insert.setFileSize(body.getFileSize());
                insert.setFileExt(body.getFileExt());
                insert.setFileHash(body.getFileHash());
                insert.setFileStorageId(body.getFileStorageId());
            }
            insert.setUploaderId(operatorId);
            insert.setDownloadCount(0);
            insert.setReviewStatus(ReviewStatusEnum.DRAFT.getCode());
            insert.setRejectReason(null);
            insert.setReviewAdminId(null);
            insert.setReviewTime(null);
            ensureNotNullColumns(insert);    // ✅ NOT NULL 列强制兜底（DB NOT NULL 列 <if> 100% 命中）

            int rows = resourceMapper.insert(insert);
            BusinessException.throwIf(rows <= 0 || insert.getId() == null,
                    ResponseCodeEnum.FAILURE, "草稿保存失败，请重试");
            log.info("草稿创建成功：draftId={}, uploaderId={}, title={}",
                    insert.getId(), operatorId, insert.getTitle());
            return insert.getId();
        }
    }

    @Override
    public PageResult<Resource> listMyDrafts(Long ownerId, String keyword,
                                             Integer pageNum, Integer pageSize) {
        BusinessException.throwIfNull(ownerId, ResponseCodeEnum.UNAUTHORIZED);
        pageNum = normalizePageNum(pageNum);
        pageSize = normalizePageSize(pageSize);

        // 固定过滤 reviewStatus = DRAFT(3) + uploaderId = ownerId
        Integer draftStatus = ReviewStatusEnum.DRAFT.getCode();
        long total = resourceMapper.countResources(
                normalizeLikeKeyword(keyword),
                null,
                draftStatus,
                ownerId,
                null,
                null);

        if (total <= 0) {
            return PageResult.empty(pageNum, pageSize);
        }

        int offset = (pageNum - 1) * pageSize;
        List<Resource> records = resourceMapper.selectResourcesPage(
                normalizeLikeKeyword(keyword),
                null,
                draftStatus,
                ownerId,
                null,
                null,
                offset,
                pageSize);

        return PageResult.of(records == null ? Collections.emptyList() : records, pageNum, pageSize, total);
    }

    @Override
    public Resource getDraft(Long id, Long operatorId) {
        BusinessException.throwIfNull(operatorId, ResponseCodeEnum.UNAUTHORIZED);
        BusinessException.throwIfNull(id, ResponseCodeEnum.RESOURCE_NOT_FOUND, "草稿ID不能为空");

        Resource res = resourceMapper.selectById(id);
        BusinessException.throwIfNull(res, ResponseCodeEnum.RESOURCE_NOT_FOUND, "草稿不存在");
        BusinessException.throwIf(ReviewStatusEnum.DRAFT.getCode() != (res.getReviewStatus() == null ? -1 : res.getReviewStatus()),
                ResponseCodeEnum.PERMISSION_DENIED, "该资源不是草稿");
        BusinessException.throwIf(!res.getUploaderId().equals(operatorId),
                ResponseCodeEnum.PERMISSION_DENIED, "只能查看自己的草稿");
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long id, Long operatorId) {
        BusinessException.throwIfNull(operatorId, ResponseCodeEnum.UNAUTHORIZED);
        BusinessException.throwIfNull(id, ResponseCodeEnum.RESOURCE_NOT_FOUND, "草稿ID不能为空");

        Resource existing = resourceMapper.selectById(id);
        BusinessException.throwIfNull(existing, ResponseCodeEnum.RESOURCE_NOT_FOUND, "草稿不存在");
        BusinessException.throwIf(ReviewStatusEnum.DRAFT.getCode() != (existing.getReviewStatus() == null ? -1 : existing.getReviewStatus()),
                ResponseCodeEnum.PERMISSION_DENIED, "该资源不是草稿，无法通过草稿接口删除");
        BusinessException.throwIf(!existing.getUploaderId().equals(operatorId),
                ResponseCodeEnum.PERMISSION_DENIED, "只能删除自己的草稿");

        // 物理删除 + 释放文件引用（与 deleteMyResource 一致）
        int rows = resourceMapper.deleteById(id);
        BusinessException.throwIf(rows <= 0, ResponseCodeEnum.FAILURE, "草稿删除失败");

        if (existing.getFileHash() != null && !existing.getFileHash().isEmpty()) {
            FileStorage storage = fileService.getFileStorageByHash(existing.getFileHash());
            if (storage != null) {
                fileService.releaseReference(storage.getId());
            }
        }
        log.info("草稿删除成功：draftId={}, operatorId={}", id, operatorId);
    }
}
