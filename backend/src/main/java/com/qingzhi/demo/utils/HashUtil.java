package com.qingzhi.demo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 哈希工具类
 * <p>对应 PRD 加分项「秒传」：上传文件前计算 MD5/SHA-256 哈希值，
 * 与数据库已有文件哈希比对，相同文件直接关联已有记录而非重复存储。
 */
public final class HashUtil {

    private HashUtil() {
        throw new UnsupportedOperationException("HashUtil 不可实例化");
    }

    private static final int BUFFER_SIZE = 8192;

    /* ====================================================================================
     * 一、MD5 哈希（128位，32位十六进制字符串）
     * ==================================================================================== */

    public static String md5(String input) {
        if (input == null) return null;
        byte[] bytes = digest(input.getBytes(StandardCharsets.UTF_8), "MD5");
        return bytesToHex(bytes);
    }

    public static String md5(byte[] input) {
        if (input == null) return null;
        return bytesToHex(digest(input, "MD5"));
    }

    /**
     * 计算文件的 MD5 哈希（秒传推荐算法，速度快，对重复文件检测足够）
     *
     * @param filePath 文件路径
     * @return 32 位小写 MD5 字符串；文件不存在或读取出错返回 null
     */
    public static String md5File(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) return null;
        try (InputStream is = Files.newInputStream(filePath)) {
            return bytesToHex(digestStream(is, "MD5"));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 计算上传文件流的 MD5（不落地到磁盘，用于秒传预检：先读流算哈希，再决定是否存文件）
     * <p>注意：此方法消费 InputStream 后流会关闭，调用方需确保后续不再使用。
     * 若需要二次消费（实际写盘），请调用方提前将流读入 byte[] 或临时文件。
     *
     * @param inputStream MultipartFile.getInputStream() 或其它文件流
     * @return 32 位小写 MD5 字符串
     * @throws IOException 读流失败
     */
    public static String md5Stream(InputStream inputStream) throws IOException {
        if (inputStream == null) return null;
        return bytesToHex(digestStream(inputStream, "MD5"));
    }

    /* ====================================================================================
     * 二、SHA-256 哈希（256位，64位十六进制字符串，碰撞概率更低，按需使用）
     * ==================================================================================== */

    public static String sha256(String input) {
        if (input == null) return null;
        return bytesToHex(digest(input.getBytes(StandardCharsets.UTF_8), "SHA-256"));
    }

    public static String sha256(byte[] input) {
        if (input == null) return null;
        return bytesToHex(digest(input, "SHA-256"));
    }

    public static String sha256File(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) return null;
        try (InputStream is = Files.newInputStream(filePath)) {
            return bytesToHex(digestStream(is, "SHA-256"));
        } catch (IOException e) {
            return null;
        }
    }

    public static String sha256Stream(InputStream inputStream) throws IOException {
        if (inputStream == null) return null;
        return bytesToHex(digestStream(inputStream, "SHA-256"));
    }

    /* ====================================================================================
     * 三、内部通用方法
     * ==================================================================================== */

    private static byte[] digest(byte[] input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的哈希算法：" + algorithm, e);
        }
    }

    private static byte[] digestStream(InputStream input, String algorithm) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的哈希算法：" + algorithm, e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
