package com.qingzhi.demo.utils;

import com.qingzhi.demo.common.Constants;
import com.qingzhi.demo.enums.ResponseCodeEnum;
import com.qingzhi.demo.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码工具类
 * <p>对应 PRD 2.1.3 密码管理：MD5 加密存储 + 格式校验</p>
 */
public final class PasswordUtil {

    private PasswordUtil() {
        throw new UnsupportedOperationException("PasswordUtil 不可实例化");
    }

    /**
     * 使用 MD5 对密码进行加密
     * <p>对应 PRD 2.1.3：使用加密算法（如 MD5）对密码进行加密后存储</p>
     *
     * @param rawPassword 明文密码
     * @return 32 位小写 MD5 哈希值；输入为 null 时返回 null
     */
    public static String encrypt(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ResponseCodeEnum.FAILURE, "密码加密失败");
        }
    }

    /**
     * 校验明文密码格式是否合法
     * <p>对应 PRD 2.1.1 密码规则：>=8位，须含数字+字母</p>
     *
     * @param rawPassword 明文密码
     * @return true=格式合法；false=格式不合法
     */
    public static boolean isValidFormat(String rawPassword) {
        return rawPassword != null && rawPassword.matches(Constants.PASSWORD_REGEX);
    }

    /**
     * 校验密码格式，若不合法则直接抛出业务异常
     * <p>Service 层可直接调用，无需再手动 if + throw</p>
     *
     * @param rawPassword 明文密码
     * @throws BusinessException 密码格式不合法（2004）时抛出
     */
    public static void validateFormat(String rawPassword) {
        if (!isValidFormat(rawPassword)) {
            BusinessException.throwOf(ResponseCodeEnum.INVALID_PASSWORD_FORMAT);
        }
    }

    /**
     * 校验两次输入的密码是否一致
     *
     * @param password        密码
     * @param confirmPassword 确认密码
     * @return true=一致；false=不一致
     */
    public static boolean isMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * 校验两次输入的密码是否一致，不一致时抛出业务异常
     *
     * @param password        密码
     * @param confirmPassword 确认密码
     * @throws BusinessException 两次密码不一致时抛出
     */
    public static void validateMatch(String password, String confirmPassword) {
        if (!isMatch(password, confirmPassword)) {
            BusinessException.throwOf(ResponseCodeEnum.FAILURE, "两次输入的密码不一致");
        }
    }

    /**
     * 校验明文密码与数据库中加密后的密码是否匹配
     *
     * @param rawPassword      明文密码（用户输入）
     * @param encryptedPassword 数据库存储的加密密码
     * @return true=匹配；false=不匹配
     */
    public static boolean checkPassword(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || encryptedPassword == null) {
            return false;
        }
        return encrypt(rawPassword).equals(encryptedPassword);
    }
}
