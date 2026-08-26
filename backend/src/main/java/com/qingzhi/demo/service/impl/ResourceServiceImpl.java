package com.qingzhi.demo.service.impl;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.common.PageResult;
import com.qingzhi.demo.entity.FileStorage;
import com.qingzhi.demo.entity.Resource;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.enums.ReviewStatusEnum;
import com.qingzhi.demo.enums.RoleEnum;
import com.qingzhi.demo.exception.BusinessException;
import com.qingzhi.demo.mapper.ResourceMapper;
import com.qingzhi.demo.service.FileService;
import com.qingzhi.demo.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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

    public ResourceServiceImpl(ResourceMapper resourceMapper, FileService fileService) {
        this.resourceMapper = resourceMapper;
        this.fileService = fileService;
    }

    /* ====================================================================================
     * 一、发布资源
     * ==================================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishResource(Resource resourceDto, Long uploaderId) {
        // 1. 基础校验：当前用户、必填字段
        BusinessException.throwIfNull(uploaderId, ResponseCodeEnum.UNAUTHORIZED);
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
        // 只要 filePath 或 fileName 任一个为空，且 fileStorageId 有值，就用 fileStorageId 查 FileStorage 表回填
        if ((!StringUtils.hasText(resourceDto.getFilePath())
                || !StringUtils.hasText(resourceDto.getFileName()))
                && resourceDto.getFileStorageId() != null) {
            FileStorage fs = fileService.getFileStorageById(resourceDto.getFileStorageId());
            BusinessException.throwIfNull(fs, ResponseCodeEnum.FILE_NOT_FOUND,
                    "关联的文件不存在(fileStorageId=" + resourceDto.getFileStorageId() + ")");
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

        long total = resourceMapper.countResources(
                normalizeLikeKeyword(keyword),
                blankToNull(course),
                reviewStatus,
                uploaderId,
                startDate,
                endDate);

        if (total <= 0) {
            return PageResult.empty(pageNum, pageSize);
        }

        int offset = (pageNum - 1) * pageSize;
        List<Resource> records = resourceMapper.selectResourcesPage(
                normalizeLikeKeyword(keyword),
                blankToNull(course),
                reviewStatus,
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
}
