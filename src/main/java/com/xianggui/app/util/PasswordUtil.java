package com.xianggui.app.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    /**
     * 生成密码哈希值（包含盐值）
     */
    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);
            
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码哈希算法不支持", e);
        }
    }

    /**
     * 验证密码是否匹配
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            byte[] decodedHash = Base64.getDecoder().decode(hashedPassword);
            
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(decodedHash, 0, salt, 0, SALT_LENGTH);
            
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] computedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            byte[] storedHash = new byte[decodedHash.length - SALT_LENGTH];
            System.arraycopy(decodedHash, SALT_LENGTH, storedHash, 0, storedHash.length);
            
            return MessageDigest.isEqual(computedHash, storedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
