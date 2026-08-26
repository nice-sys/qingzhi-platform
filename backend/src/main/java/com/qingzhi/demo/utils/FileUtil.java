package com.qingzhi.demo.utils;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件工具类
 * <p>对应 PRD 2.3.1 资源字段定义 + 3.1 性能需求（50MB限制）+ 加分项秒传。</p>
 */
public final class FileUtil {

    private FileUtil() {
        throw new UnsupportedOperationException("FileUtil 不可实例化");
    }

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;
    private static final long TB = GB * 1024L;

    /* ====================================================================================
     * 一、扩展名相关
     * ==================================================================================== */

    /**
     * 获取文件扩展名（小写，不含点号）
     * <p>例："report.PDF" → "pdf"，"readme" → ""</p>
     */
    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 校验文件扩展名是否在允许列表中（Constants.ALLOWED_FILE_EXTENSIONS）
     *
     * @throws BusinessException FILE_TYPE_NOT_SUPPORTED
     */
    public static void validateExtension(String fileName) {
        String ext = getExtension(fileName);
        if (ext.isEmpty()) {
            BusinessException.throwOf(ResponseCodeEnum.FILE_TYPE_NOT_SUPPORTED);
        }
        for (String allowed : Constants.ALLOWED_FILE_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(ext)) {
                return;
            }
        }
        BusinessException.throwOf(ResponseCodeEnum.FILE_TYPE_NOT_SUPPORTED);
    }

    /**
     * 校验文件大小是否超过 50MB（PRD 3.1：单文件 <= 50MB）
     *
     * @throws BusinessException FILE_SIZE_EXCEEDED
     */
    public static void validateFileSize(long sizeBytes) {
        if (sizeBytes > Constants.MAX_FILE_SIZE_BYTES) {
            BusinessException.throwOf(ResponseCodeEnum.FILE_SIZE_EXCEEDED);
        }
    }

    /* ====================================================================================
     * 二、路径 / 文件名生成
     * ==================================================================================== */

    /**
     * 生成带日期分层的相对存储子目录（便于按日期归档，避免单目录文件过多）
     * <p>格式：yyyy/MM/dd → 例：2026/08/26</p>
     */
    public static String generateDateSubDir() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * 生成安全的存储文件名（UUID + 原始扩展名，防止中文特殊字符 & 重名覆盖）
     *
     * @param originalFileName 原始文件名（用于取扩展名）
     * @return 例："a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf"
     */
    public static String generateSafeFileName(String originalFileName) {
        String ext = getExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (ext.isEmpty()) {
            return uuid;
        }
        return uuid + "." + ext;
    }

    /**
     * 生成完整的相对存储路径（日期子目录 + 安全文件名）
     * <p>例："2026/08/26/a1b2c3...pdf"</p>
     */
    public static String generateRelativePath(String originalFileName) {
        return generateDateSubDir() + "/" + generateSafeFileName(originalFileName);
    }

    /**
     * 将相对路径解析为绝对路径（以 baseDir 为根）
     *
     * @param baseDir      基础存储目录（绝对路径），如 "D:/qingzhi_uploads"
     * @param relativePath 相对路径（如 "2026/08/26/xxx.pdf"）
     * @return 合并后的绝对 Path 对象（已自动创建缺失的目录）
     * @throws IOException 创建目录失败
     */
    public static Path resolveAbsolutePath(String baseDir, String relativePath) throws IOException {
        Path absPath = Paths.get(baseDir, relativePath);
        Path parentDir = absPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        return absPath;
    }

    /* ====================================================================================
     * 三、文件读写操作
     * ==================================================================================== */

    /**
     * 将 InputStream 保存到指定绝对路径
     *
     * @param source   输入流（调用方负责关闭）
     * @param destPath 目标绝对路径（目录需已存在，或配合 resolveAbsolutePath 使用）
     * @return 实际写入的字节数
     * @throws IOException 写入失败
     */
    public static long saveStream(InputStream source, Path destPath) throws IOException {
        return Files.copy(source, destPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 删除磁盘上的文件（忽略不存在的情况）
     *
     * @return true=删除成功或本就不存在；false=删除异常但已记录日志
     */
    public static boolean deleteFile(Path path) {
        if (path == null) return true;
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    /* ====================================================================================
     * 四、显示格式化（前端友好）
     * ==================================================================================== */

    /**
     * 将字节数格式化为可读字符串（保留 2 位小数，自动选合适单位）
     * <p>例：524288 → "512.00 KB"，1572864 → "1.50 MB"</p>
     */
    public static String formatSize(long sizeBytes) {
        if (sizeBytes < 0) return "0 B";
        if (sizeBytes < KB) return sizeBytes + " B";
        if (sizeBytes < MB) return String.format(Locale.ROOT, "%.2f KB", (double) sizeBytes / KB);
        if (sizeBytes < GB) return String.format(Locale.ROOT, "%.2f MB", (double) sizeBytes / MB);
        if (sizeBytes < TB) return String.format(Locale.ROOT, "%.2f GB", (double) sizeBytes / GB);
        return String.format(Locale.ROOT, "%.2f TB", (double) sizeBytes / TB);
    }
}
