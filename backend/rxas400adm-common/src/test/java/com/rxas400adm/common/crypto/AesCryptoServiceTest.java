package com.rxas400adm.common.crypto;

import com.rxas400adm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3：AesCryptoService 密钥派生由 SHA-256 升级为 PBKDF2 后的行为测试。
 * 重点覆盖：新格式往返、随机盐、存量 legacy 密文兼容、密钥不匹配/损坏一律抛受控异常
 * （绝不把密文当密码返回，P2-7 语义在两种格式上保持一致）。
 */
class AesCryptoServiceTest {

    private static final String KEY = "RXAS400-TEST-KEY-0123456789";

    @Test
    void encryptDecrypt_roundtrip() {
        AesCryptoService service = new AesCryptoService(KEY);
        String ciphertext = service.encrypt("P@ssw0rd-中文-秘密");
        assertTrue(ciphertext.startsWith("v2:"), "新密文应带 v2 前缀");
        assertEquals("P@ssw0rd-中文-秘密", service.decrypt(ciphertext));
    }

    @Test
    void encrypt_samePlaintext_differentCiphertext() {
        AesCryptoService service = new AesCryptoService(KEY);
        assertNotEquals(service.encrypt("same"), service.encrypt("same"), "随机盐+IV 应使同明文不同密文");
    }

    @Test
    void decrypt_v2Ciphertext_wrongKey_throwsBusinessException() {
        String ciphertext = new AesCryptoService(KEY).encrypt("secret");
        AesCryptoService wrong = new AesCryptoService(KEY + "-other");
        BusinessException ex = assertThrows(BusinessException.class, () -> wrong.decrypt(ciphertext));
        assertTrue(ex.getMessage().contains("RXAS400_CRYPTO_KEY"));
    }

    @Test
    void decrypt_v2Ciphertext_tampered_throwsBusinessException() {
        String ciphertext = new AesCryptoService(KEY).encrypt("secret");
        // 篡改密文主体（不影响前缀与盐/IV 边界）：翻转密文区最后一个字节
        byte[] combined = Base64.getDecoder().decode(ciphertext.substring(3));
        combined[combined.length - 1] ^= 0x01;
        String tampered = "v2:" + Base64.getEncoder().encodeToString(combined);
        assertThrows(BusinessException.class, () -> new AesCryptoService(KEY).decrypt(tampered));
    }

    @Test
    void decrypt_v2MalformedPrefix_throwsBusinessException() {
        assertThrows(BusinessException.class, () -> new AesCryptoService(KEY).decrypt("v2:not-base64!"));
        assertThrows(BusinessException.class, () -> new AesCryptoService(KEY).decrypt("v2:AAAA")); // 长度不足盐+IV
    }

    @Test
    void decrypt_legacyCiphertext_stillCompatible() throws Exception {
        // 存量密文：SHA-256(rawKey) 派生 + Base64(12字节IV + 密文)，用旧算法构造
        String legacyCipher = legacyEncrypt("legacy-key", "老密码-secret");
        AesCryptoService service = new AesCryptoService("legacy-key");
        assertEquals("老密码-secret", service.decrypt(legacyCipher), "存量 SHA-256 密文应可正常解密");
    }

    @Test
    void decrypt_legacyCiphertext_wrongKey_throwsBusinessException() throws Exception {
        String legacyCipher = legacyEncrypt("key-A", "secret");
        AesCryptoService service = new AesCryptoService("key-B");
        assertThrows(BusinessException.class, () -> service.decrypt(legacyCipher));
    }

    @Test
    void decrypt_plaintextLegacy_passthrough() {
        AesCryptoService service = new AesCryptoService(KEY);
        // 非 Base64 / 过短 → 存量明文兼容，原样返回
        assertEquals("notbase64!!", service.decrypt("notbase64!!"));
        assertEquals("short", service.decrypt("short"));
        assertEquals("", service.decrypt(""));
        assertEquals(null, service.decrypt(null));
    }

    @Test
    void customPbkdf2Iterations_roundtrip() {
        // P3：迭代次数可配置化——自定义（小）迭代次数实例同样可加解密，且密文带 v2 前缀
        AesCryptoService service = new AesCryptoService(KEY, 1_000);
        String ciphertext = service.encrypt("configurable-iterations");
        assertTrue(ciphertext.startsWith("v2:"));
        assertEquals("configurable-iterations", service.decrypt(ciphertext));
    }

    @Test
    void invalidPbkdf2Iterations_throws() {
        assertThrows(IllegalArgumentException.class, () -> new AesCryptoService(KEY, 0));
        assertThrows(IllegalArgumentException.class, () -> new AesCryptoService(KEY, -1));
    }

    @Test
    void encrypt_emptyOrNull_passthrough() {
        AesCryptoService service = new AesCryptoService(KEY);
        assertEquals("", service.encrypt(""));
        assertEquals(null, service.encrypt(null));
    }

    /** 复刻旧版加密格式（SHA-256 派生密钥 + Base64(IV||密文)），用于存量兼容测试 */
    private String legacyEncrypt(String rawKey, String plaintext) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha256.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec key = new SecretKeySpec(hash, "AES");

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[12 + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, 12);
        System.arraycopy(ciphertext, 0, combined, 12, ciphertext.length);
        return Base64.getEncoder().encodeToString(combined);
    }
}
