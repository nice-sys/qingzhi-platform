package com.qingzhi.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id; // 学生id（主键）
    private String stuId; // 学号（12位纯数字，唯一）
    private String name; // 姓名
    private String phone; // 手机号
    private String email; // 邮箱
    private String department; // 院系
    private String major; // 专业
    private Long userId; // 关联 user 表的 id（用于登录认证）
}