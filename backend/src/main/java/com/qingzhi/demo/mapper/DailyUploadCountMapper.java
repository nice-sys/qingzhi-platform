package com.qingzhi.demo.mapper;

import com.qingzhi.demo.entity.DailyUploadCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 每日上传计数 Mapper
 * <p>配合 UNIQUE(user_id, upload_date) 索引 + 行级锁实现并发安全的配额扣减。
 */
@Mapper
public interface DailyUploadCountMapper {

    /**
     * 按 (userId, uploadDate) 查询记录并加行锁（SELECT ... FOR UPDATE）。
     * <p>调用方必须在事务内使用；记录不存在返回 null，调用方负责 insert 初始化。
     *
     * @param userId     用户ID
     * @param uploadDate 上传日期（当天 LocalDate.now()）
     * @return 匹配的 DailyUploadCount 实例；不存在返回 null
     */
    DailyUploadCount selectByUserAndDateForUpdate(@Param("userId") Long userId,
                                                  @Param("uploadDate") LocalDate uploadDate);

    /**
     * 首次新建记录：upload_count = 1
     *
     * @param record 必须包含 userId / uploadDate；uploadCount=1；create_time/update_time 自动填充
     * @return 影响行数（冲突时 UNIQUE 索引会抛 DuplicateKey，交给 Service 重试）
     */
    int insertInitial(DailyUploadCount record);

    /**
     * 将指定行的 upload_count 原子 +1（SET upload_count = upload_count + 1）
     *
     * @param id 主键ID（由 selectByUserAndDateForUpdate 返回）
     * @return 影响行数（应返回 1）
     */
    int incrementCountById(@Param("id") Long id);
}
