package com.qingzhi.demo.dto.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 单行导入失败信息
 * <p>用于 ImportResultResponse.failList</p>
 */
public class ImportRowError implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Excel 行号（从 1 开始，用户可以快速定位哪一行出错）
     */
    private int rowNum;

    /**
     * 失败原因（中文描述）
     */
    private String reason;

    /**
     * 该行的主键标识（学号/工号，便于查找）
     */
    private String username;

    public ImportRowError() {
    }

    public ImportRowError(int rowNum, String username, String reason) {
        this.rowNum = rowNum;
        this.username = username;
        this.reason = reason;
    }

    public int getRowNum() { return rowNum; }
    public void setRowNum(int rowNum) { this.rowNum = rowNum; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
