package com.qingzhi.demo.mapper;

import com.qingzhi.demo.entity.FileStorage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件存储 Mapper 接口（加分项：秒传）
 * <p>对应 PRD 4.4 file_storage 表，与 ResourceMapper 配合完成秒传逻辑。</p>
 */
@Mapper
public interface FileStorageMapper {

    /**
     * 根据文件哈希查找已存储记录（秒传命中判断核心）
     *
     * @param fileHash 文件 MD5/SHA-256 哈希值（唯一约束）
     * @return 已存在的 FileStorage 记录；不存在返回 null
     */
    FileStorage selectByFileHash(@Param("fileHash") String fileHash);

    /**
     * 根据主键查询（用于加载下载信息等）
     */
    FileStorage selectById(@Param("id") Long id);

    /**
     * 插入新的文件存储记录（首次上传未命中秒传时调用）
     *
     * @param storage 需包含 fileHash/filePath/fileSize；referenceCount 默认 1；createTime 默认 NOW()
     * @return 影响行数；useGeneratedKeys 回填 id 到 storage.id
     */
    int insert(FileStorage storage);

    /**
     * 秒传命中时：引用计数 +1
     *
     * @param id file_storage.id
     * @return 影响行数（应返回 1）
     */
    int incrementReferenceCount(@Param("id") Long id);

    /**
     * 解除文件引用时：引用计数 -1
     * <p>当 Resource 记录被删除时，释放其关联的 file 记录引用。</p>
     *
     * @param id file_storage.id
     * @return 影响行数
     */
    int decrementReferenceCount(@Param("id") Long id);

    /**
     * 根据主键删除记录（仅当 referenceCount 被检查为 0 时调用，由 Service 保证前置条件）
     *
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
