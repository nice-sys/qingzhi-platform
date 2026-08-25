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
@TableName("teacher")
public class Teacher {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String teacherId; // 工号（格式：T + 8位数字，如 T20240001，唯一）
    private String name; // 姓名
    private String phone; // 手机号
    private String email; // 邮箱
    private String department; // 院系
    private Long userId; // 关联 user 表的 id（用于登录认证）
}