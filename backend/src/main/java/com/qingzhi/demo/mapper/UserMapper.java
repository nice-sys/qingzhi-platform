package com.qingzhi.demo.mapper;

import com.qingzhi.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper 接口
 * <p>对应 PRD 4.1 用户表，提供注册模块所需的查询与插入操作</p>
 */
@Mapper
public interface UserMapper {

    /**
     * 根据账号（用户名）查询用户
     * <p>用于注册时查重、登录时查询用户</p>
     *
     * @param username 账号（学号/工号/Admin）
     * @return 用户实体；不存在返回 null
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 插入新用户（注册、批量导入共用）
     *
     * @param user 用户实体（需包含 username/password/role 等必填字段）
     * @return 影响行数；成功插入返回 1
     */
    int insert(User user);

    /**
     * 根据主键ID查询用户
     *
     * @param id 用户ID
     * @return 用户实体；不存在返回 null
     */
    User selectById(@Param("id") Long id);

    /**
     * 更新用户登录失败信息（登录失败次数 + 锁定状态 + 锁定时间）
     * <p>对应 PRD 加分项：登录防暴力破解</p>
     *
     * @param user 需包含 id、loginFailCount、status、lockTime 字段
     * @return 影响行数
     */
    int updateLoginFailInfo(User user);

    /**
     * 重置用户登录失败信息（登录成功时调用：失败次数归零 + 解锁）
     *
     * @param id 用户ID
     * @return 影响行数
     */
    int resetLoginFailInfo(@Param("id") Long id);

    /**
     * 更新用户密码
     *
     * @param id       用户ID
     * @param password 新密码（MD5 加密后）
     * @return 影响行数
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /* ====================================================================================
     * 管理员用户管理接口（PRD 2.2.2）
     * ==================================================================================== */

    /**
     * 按条件统计用户总数（用于分页 total）
     *
     * @param keyword    关键字（username / name 模糊），可为 null
     * @param role       角色筛选，可为 null（全部）
     * @param status     状态筛选，可为 null（全部）
     * @param department 院系模糊，可为 null
     */
    long countUsers(@Param("keyword") String keyword,
                    @Param("role") Integer role,
                    @Param("status") Integer status,
                    @Param("department") String department);

    /**
     * 按条件分页查询用户列表
     *
     * @param keyword    关键字（username / name 模糊），可为 null
     * @param role       角色筛选，可为 null（全部）
     * @param status     状态筛选，可为 null（全部）
     * @param department 院系模糊，可为 null
     * @param offset     SQL LIMIT offset
     * @param pageSize   SQL LIMIT pageSize
     */
    java.util.List<User> selectUsersPage(@Param("keyword") String keyword,
                                         @Param("role") Integer role,
                                         @Param("status") Integer status,
                                         @Param("department") String department,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    /**
     * 根据主键删除用户（管理员删除用户）
     *
     * @param id 用户ID
     * @return 影响行数（不存在返回 0，成功删除返回 1）
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据主键动态更新用户信息（管理员编辑用户）
     * <p>只更新传入非空的字段，避免覆盖原值。</p>
     *
     * @param user 需包含 id + 要修改的字段（其余字段留 null 即可不更新）
     * @return 影响行数
     */
    int updateById(User user);

    /**
     * 统计平台用户总数（所有角色 + 所有状态）
     *
     * @return 用户数（空表返回 0）
     */
    long countTotalUsers();
}
