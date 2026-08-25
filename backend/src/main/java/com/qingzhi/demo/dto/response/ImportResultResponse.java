package com.qingzhi.demo.dto.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入整体结果响应 DTO
 * <p>对应 PRD 2.4 数据导入模块（批量导入师生信息）
 * & 加分项「导入回滚」：有任何一行失败时 successCount=0 total=全部行；或部分成功部分失败（两种模式均可，此处采用"部分成功+失败明细"模式）。</p>
 */
public class ImportResultResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总行数（不含表头） */
    private int total;

    /** 成功导入数量 */
    private int successCount;

    /** 失败数量 */
    private int failCount;

    /** 失败行明细（便于前端高亮展示 + 用户修正 Excel） */
    private List<ImportRowError> failList;

    public ImportResultResponse() {
        this.failList = new ArrayList<>();
    }

    /**
     * 新增一条失败记录
     */
    public void addError(int rowNum, String username, String reason) {
        this.failList.add(new ImportRowError(rowNum, username, reason));
        this.failCount++;
    }

    /**
     * 完成统计：由外部设置 total/successCount；内部用 failList.size() 保证一致
     */
    public void finish(int total) {
        this.total = total;
        this.successCount = total - this.failCount;
    }

    public boolean hasError() {
        return failCount > 0;
    }

    /* ====================================================================================
     * Getter / Setter
     * ==================================================================================== */

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public List<ImportRowError> getFailList() { return failList; }
    public void setFailList(List<ImportRowError> failList) { this.failList = failList; }
}
