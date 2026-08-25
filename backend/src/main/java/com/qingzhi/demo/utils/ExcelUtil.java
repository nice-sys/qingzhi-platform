package com.qingzhi.demo.utils;

import com.qingzhi.demo.dto.response.ExcelUserRow;
import com.qingzhi.demo.enums.RoleEnum;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入工具类（自实现，PRD 3.3.1 规定）
 * <p>基于 Apache POI，同时支持 .xls（HSSF）和 .xlsx（XSSF）格式。</p>
 *
 * <p>Excel 列约定：
 * <ul>
 *   <li>学生导入（role=STUDENT）：列 0-学号, 1-姓名, 2-手机号, 3-邮箱, 4-院系, 5-专业, 6-初始密码</li>
 *   <li>教师导入（role=TEACHER）：列 0-工号, 1-姓名, 2-手机号, 3-邮箱, 4-院系, 5-初始密码</li>
 * </ul>
 * 第 1 行为表头，会自动跳过。
 */
public final class ExcelUtil {

    private ExcelUtil() {
        throw new UnsupportedOperationException("ExcelUtil 不可实例化");
    }

    /**
     * 解析 Excel 文件为用户行列表
     * <p>第一行（表头）会被自动跳过；不会在此方法中执行任何业务校验（必填/重复/格式等），
     * 仅负责把单元格值读取成字符串并封装为 ExcelUserRow。</p>
     *
     * @param file 前端上传的 Excel 文件
     * @param role  角色（决定列索引）：TEACHER 教师 / STUDENT 学生；ADMIN 不允许导入
     * @return 解析后的行数据列表（ExcelUserRow.rowNum 从 2 开始，对应 Excel 真实行号）
     * @throws IOException 文件读取异常
     * @throws IllegalArgumentException 文件格式不支持或 role 非法
     */
    public static List<ExcelUserRow> parseUserExcel(MultipartFile file, RoleEnum role) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (role == null || !role.isTeacher() && !role.isStudent()) {
            throw new IllegalArgumentException("导入角色必须为教师或学生");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名识别失败");
        }
        String lowerName = filename.toLowerCase();

        try (InputStream is = file.getInputStream();
             Workbook wb = createWorkbook(is, lowerName)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 中没有可用的 Sheet");
            }

            List<ExcelUserRow> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            // row=0 是表头，从 row=1 开始读数据，真实行号 = i+1
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowBlank(row)) {
                    continue; // 跳过空行
                }
                ExcelUserRow data = parseRow(row, i + 1, role);
                if (data != null) {
                    rows.add(data);
                }
            }
            return rows;
        }
    }

    /* ====================================================================================
     * 内部辅助
     * ==================================================================================== */

    private static Workbook createWorkbook(InputStream is, String lowerName) throws IOException {
        if (lowerName.endsWith(".xlsx")) {
            return new XSSFWorkbook(is);
        } else if (lowerName.endsWith(".xls")) {
            return new HSSFWorkbook(is);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .xls 或 .xlsx");
        }
    }

    private static boolean isRowBlank(Row row) {
        for (int c = 0; c < 7; c++) {
            Cell cell = row.getCell(c);
            String v = getCellStr(cell);
            if (v != null) return false;
        }
        return true;
    }

    private static ExcelUserRow parseRow(Row row, int rowNum, RoleEnum role) {
        ExcelUserRow d = new ExcelUserRow();
        d.setRowNum(rowNum);
        d.setUsername(getCellStr(row.getCell(0)));
        d.setName(getCellStr(row.getCell(1)));
        d.setPhone(getCellStr(row.getCell(2)));
        d.setEmail(getCellStr(row.getCell(3)));
        d.setDepartment(getCellStr(row.getCell(4)));
        if (role.isStudent()) {
            d.setMajor(getCellStr(row.getCell(5)));
            d.setPassword(getCellStr(row.getCell(6)));
        } else {
            // 教师无专业列
            d.setMajor(null);
            d.setPassword(getCellStr(row.getCell(5)));
        }
        return d;
    }

    /**
     * 读取单元格为字符串（处理数字/日期/公式单元格类型，
     * 避免数字 2024001001 被读取成 2.024001001E9 的科学计数法）。
     */
    private static String getCellStr(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                String s = cell.getStringCellValue();
                return (s == null || s.trim().isEmpty()) ? null : s.trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 日期转字符串（简单处理：toString；本项目无需日期列）
                    return String.valueOf(cell.getDateCellValue().getTime());
                }
                // 数字列（学号/工号/手机号），转成无小数位的字符串
                double v = cell.getNumericCellValue();
                long lv = (long) v;
                if (Math.abs(v - lv) < 1e-9) {
                    return String.valueOf(lv);
                }
                return String.valueOf(v);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return null;
                }
            case BLANK:
            default:
                return null;
        }
    }
}
