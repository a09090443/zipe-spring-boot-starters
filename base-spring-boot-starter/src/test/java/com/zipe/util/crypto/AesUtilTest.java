package com.zipe.util.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

/**
 * 驗證 AesUtil 採用隨機 IV 的 AES/CBC/PKCS5Padding，確保加密為非確定性。
 */
class AesUtilTest {

    // 16 bytes 合法金鑰
    private static final String SECRET_KEY = "testtesttesttest";

    /**
     * 相同明文連續兩次加密，因 IV 為隨機產生，密文必須不同。
     */
    @Test
    void encryptIsNonDeterministicDueToRandomIv() {
        AesUtil aesUtil = new AesUtil(SECRET_KEY);
        String content = "AESTest";

        String cipher1 = aesUtil.getEncrypt(content);
        String cipher2 = aesUtil.getEncrypt(content);

        assertNotEquals(cipher1, cipher2, "相同明文兩次加密應因隨機 IV 而產生不同密文");
    }

    /**
     * 加密後再解密，應能還原原始明文。
     */
    @Test
    void encryptThenDecodeRoundTrip() {
        AesUtil aesUtil = new AesUtil(SECRET_KEY);
        String content = "AESTest中文內容";

        String cipher = aesUtil.getEncrypt(content, StandardCharsets.UTF_8.name());
        String plain = aesUtil.getDecode(cipher, StandardCharsets.UTF_8.name());

        assertEquals(content, plain, "解密後應還原原始明文");
    }

    /**
     * 密文格式應為 Base64(IV || ciphertext)，長度需大於單純 16-byte IV。
     */
    @Test
    void cipherTextContainsIvPrefix() {
        AesUtil aesUtil = new AesUtil(SECRET_KEY);

        String cipher = aesUtil.getEncrypt("A");
        byte[] decoded = Base64.decodeBase64(cipher);

        assertTrue(decoded.length > 16, "密文應包含 16-byte IV 前綴與密文本體");
    }

    /**
     * 金鑰長度非 16 bytes 時，加密應丟出例外。
     */
    @Test
    void invalidKeyLengthThrowsOnEncrypt() {
        AesUtil aesUtil = new AesUtil("shortkey");

        assertThrows(RuntimeException.class, () -> aesUtil.getEncrypt("A"),
                "金鑰長度非 16 bytes 時加密應丟例外");
    }

    /**
     * 金鑰長度非 16 bytes 時，解密應丟出例外。
     */
    @Test
    void invalidKeyLengthThrowsOnDecode() {
        AesUtil aesUtil = new AesUtil("shortkey");

        assertThrows(RuntimeException.class, () -> aesUtil.getDecode("anything"),
                "金鑰長度非 16 bytes 時解密應丟例外");
    }
}
