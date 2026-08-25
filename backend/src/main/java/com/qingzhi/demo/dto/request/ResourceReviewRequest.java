package com.qingzhi.demo.dto.request;

import com.qingzhi.demo.common.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 管理员审核资源请求 DTO
 * <p>PRD 2.3.2 状态流转 + PRD 2.2.2 资源管理（审核通过/拒绝）</p>
 */
public class ResourceReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 被审核的资源ID
     */
    @NotNull(message = "资源ID不能为空")
    @Positive(message = "资源ID必须大于0")
    private Long resourceId;

    /**
     * 审核动作：true=通过，false=拒绝
     */
    @NotNull(message = "审核动作不能为空")
    private Boolean approve;

    /**
     * 拒绝理由（拒绝时必填；通过时可空）
     */
    @Size(max = Constants.REJECT_REASON_MAX_LENGTH,
            message = "拒绝理由长度不能超过 " + Constants.REJECT_REASON_MAX_LENGTH + " 字符")
    private String rejectReason;

    public ResourceReviewRequest() {
    }

    /**
     * 拒绝时校验拒绝理由是否非空
     */
    public boolean isRejectReasonMissing() {
        if (approve == null || approve) return false; // 非拒绝无需理由
        return rejectReason == null || rejectReason.trim().isEmpty();
    }

    /* ====================================================================================
     * Getter / Setter
     * ==================================================================================== */

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Boolean getApprove() { return approve; }
    public void setApprove(Boolean approve) { this.approve = approve; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
