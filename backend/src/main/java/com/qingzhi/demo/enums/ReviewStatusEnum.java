package com.qingzhi.demo.enums;

/**
 * 资源审核状态枚举
 * <p>对应 PRD 2.3.2 资源状态流转：待审核 → 已通过 / 已拒绝 → 修改 → 待审核</p>
 */
public enum ReviewStatusEnum {

    /** 待审核：用户提交资源后的初始状态，仅本人和管理员可见 */
    PENDING(0, "待审核"),

    /** 已通过：管理员审核通过后，所有用户可见 */
    APPROVED(1, "已通过"),

    /** 已拒绝：管理员审核拒绝，须填写拒绝理由，用户修改后可重新提交 */
    REJECTED(2, "已拒绝");

    private final int code;
    private final String message;

    ReviewStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据 code 查询枚举；找不到返回 null
     */
    public static ReviewStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReviewStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    /**
     * 是否为合法的审核状态值
     */
    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}
