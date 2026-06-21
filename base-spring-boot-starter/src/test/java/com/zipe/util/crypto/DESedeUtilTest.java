package com.zipe.util.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * 驗證 DESedeUtil 採用隨機 IV 的 DESede/CBC/PKCS5Padding，並於失敗時丟例外（不再靜默回傳 null）。
 */
class DESedeUtilTest {

    // 48 個 16 進位字元（24 bytes）合法 3DES 金鑰
    private static final String SECRET_KEY = "098f6bcd4621d373cade4e832627b4f62017121611345734";

    /**
     * 加密後再解密，應能還原原始明文。
     */
    @Test
    void encryptThenDecodeRoundTrip() {
        DESedeUtil util = new DESedeUtil(SECRET_KEY);
        String content = "Gary中文";

        String cipher = util.getEncrypt(content, StandardCharsets.UTF_8.name());
        String plain = util.getDecode(cipher, StandardCharsets.UTF_8.name());

        assertEquals(content, plain, "解密後應還原原始明文");
    }

    /**
     * 相同明文連續兩次加密，因 IV 為隨機產生，密文必須不同。
     */
    @Test
    void encryptIsNonDeterministicDueToRandomIv() {
        DESedeUtil util = new DESedeUtil(SECRET_KEY);
        String content = "Gary";

        String cipher1 = util.getEncrypt(content);
        String cipher2 = util.getEncrypt(content);

        assertNotEquals(cipher1, cipher2, "相同明文兩次加密應因隨機 IV 而產生不同密文");
    }

    /**
     * 解密非法輸入時，應丟出例外而非回傳 null。
     */
    @Test
    void decodeInvalidInputThrows() {
        DESedeUtil util = new DESedeUtil(SECRET_KEY);

        assertThrows(RuntimeException.class, () -> util.getDecode("00"),
                "解密非法輸入應丟例外，不可靜默回傳 null");
    }
}
