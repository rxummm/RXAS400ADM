package com.rxas400adm.common.crypto;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 加解密服务。
 * 密钥由环境变量 RXAS400_CRYPTO_KEY 提供，不落地、不进数据库。
 *
 * <p>P3：密钥派生由 SHA-256(rawKey) 升级为 PBKDF2WithHmacSHA256（随机盐 + 12 万次迭代），
 * 抗暴力破解/字典攻击能力显著增强。新密文格式带版本前缀：
 * <pre>v2:Base64(16字节盐 + 12字节IV + 密文)</pre>
 * 每次加密随机盐 + 随机 IV，同明文不同密文。
 *
 * <p>存量兼容：解密先识别 v2 前缀走 PBKDF2；无前缀的旧格式（SHA-256 派生的
 * Base64(12字节IV+密文)）走 legacy 路径，因此历史密文无需迁移、滚动切换即可。
 */
@Slf4j
public final class AesCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_BITS = 256;
    /** 默认 PBKDF2 迭代次数（可由 rxas400.crypto.pbkdf2-iterations 覆盖） */
    public static final int DEFAULT_PBKDF2_ITERATIONS = 120_000;
    /** v2 密文前缀（PBKDF2 派生密钥），与 legacy 格式（纯 Base64）天然可区分 */
    private static final String V2_PREFIX = "v2:";

    private final String rawKey;
    private final int pbkdf2Iterations;
    private final SecretKeySpec legacyKeySpec;
    private final SecureRandom secureRandom;

    /** 使用默认迭代次数（DEFAULT_PBKDF2_ITERATIONS） */
    public AesCryptoService(String rawKey) {
        this(rawKey, DEFAULT_PBKDF2_ITERATIONS);
    }

    /**
     * @param pbkdf2Iterations PBKDF2 迭代次数（P3 可配置化）。密钥源是环境变量（高熵随机串），
     *                         KDF 仅在服务启动/每次构造时执行一次（非每次登录），
     *                         120k 为默认值，在启动耗时与安全性之间取平衡。
     */
    public AesCryptoService(String rawKey, int pbkdf2Iterations) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalArgumentException("AES 密钥不能为空，请设置环境变量 RXAS400_CRYPTO_KEY");
        }
        if (pbkdf2Iterations < 1) {
            throw new IllegalArgumentException("PBKDF2 迭代次数必须 >= 1: " + pbkdf2Iterations);
        }
        this.rawKey = rawKey;
        this.pbkdf2Iterations = pbkdf2Iterations;
        // legacy 密钥派生保留（存量密文解密用）
        this.legacyKeySpec = deriveKeySha256(rawKey);
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            SecretKeySpec key = deriveKeyPbkdf2(salt);

            byte[] ciphertext = gcm(key, iv, Cipher.ENCRYPT_MODE, plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[SALT_LENGTH + GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(salt, 0, combined, 0, SALT_LENGTH);
            System.arraycopy(iv, 0, combined, SALT_LENGTH, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, SALT_LENGTH + GCM_IV_LENGTH, ciphertext.length);

            return V2_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // 【E11】log-and-rethrow 去重：异常带 cause 抛出后由全局处理器统一记录，这里降为 debug，避免双份 error 堆栈
            log.debug("AES 加密失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AES 加密失败");
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        // v2 前缀（PBKDF2 派生）：格式错误/解密失败一律抛受控异常，绝不把密文当密码返回
        if (ciphertext.startsWith(V2_PREFIX)) {
            return decryptV2(ciphertext);
        }
        // 无前缀 → legacy（SHA-256 派生）存量格式，兼容历史密文
        return decryptLegacy(ciphertext);
    }

    private String decryptV2(String ciphertext) {
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(ciphertext.substring(V2_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw decryptionFailed();
        }
        if (combined.length < SALT_LENGTH + GCM_IV_LENGTH) {
            throw decryptionFailed();
        }
        try {
            byte[] salt = Arrays.copyOfRange(combined, 0, SALT_LENGTH);
            byte[] iv = Arrays.copyOfRange(combined, SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, SALT_LENGTH + GCM_IV_LENGTH, combined.length);
            SecretKeySpec key = deriveKeyPbkdf2(salt);
            byte[] plaintext = gcm(key, iv, Cipher.DECRYPT_MODE, encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 【E11】log-and-rethrow 去重：decryptionFailed() 会被全局处理器记录，这里降为 debug 避免双份堆栈
            log.debug("AES 解密失败（PBKDF2 密文，密钥不匹配或数据损坏）: {}", e.getMessage());
            throw decryptionFailed();
        }
    }

    private String decryptLegacy(String ciphertext) {
        byte[] combined;
        try {
            combined = Base64.getDecoder().decode(ciphertext);
        } catch (IllegalArgumentException e) {
            // 非 Base64 → 存量明文密码（V31 兼容），原样返回
            return ciphertext;
        }
        if (combined.length < GCM_IV_LENGTH) {
            // 长度不足 IV → 非密文格式，按存量明文兼容
            return ciphertext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            byte[] plaintext = gcm(legacyKeySpec, iv, Cipher.DECRYPT_MODE, encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // P2-7：格式上确为密文但解密失败（密钥不匹配/数据损坏）→ 抛受控异常，
            // 绝不把 Base64 密文当真实密码返回（否则登录链路会把密文当密码去认证）
            // 【E11】log-and-rethrow 去重：decryptionFailed() 会被全局处理器记录，这里降为 debug 避免双份堆栈
            log.debug("AES 解密失败（legacy 密文，密钥不匹配或密文损坏）: {}", e.getMessage());
            throw decryptionFailed();
        }
    }

    private byte[] gcm(SecretKeySpec key, byte[] iv, int mode, byte[] data) throws Exception {
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, key, gcmSpec);
        return cipher.doFinal(data);
    }

    /** v2 密钥派生：PBKDF2WithHmacSHA256(rawKey, salt, pbkdf2Iterations, 256bit) */
    private SecretKeySpec deriveKeyPbkdf2(byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawKey.toCharArray(), salt, pbkdf2Iterations, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] key = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AES 密钥派生失败（PBKDF2）");
        }
    }

    /** legacy 密钥派生：SHA-256(rawKey)（仅用于存量密文解密，新密文不再使用） */
    private static SecretKeySpec deriveKeySha256(String rawKey) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AES 密钥派生失败");
        }
    }

    private static BusinessException decryptionFailed() {
        return new BusinessException(ErrorCode.INTERNAL_ERROR,
                "AS400 连接密码解密失败：请检查 RXAS400_CRYPTO_KEY 是否与加密时一致");
    }
}
