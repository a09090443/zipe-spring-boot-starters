package com.zipe.util.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 雜湊工具類。
 *
 * <p>以 JDK 內建的 {@link MessageDigest} 實作，字串一律以 {@link StandardCharsets#UTF_8}
 * 編碼後再計算雜湊值，避免如 {@link Md5Util} 使用平台預設編碼、導致相同輸入在不同環境
 * 產生不同結果的問題。</p>
 *
 * <p><b>用途提醒：</b>SHA-256 適合檔案完整性校驗、產生快取鍵等一般雜湊用途；
 * <b>不適合直接用於密碼雜湊</b>（缺乏加鹽與工作量調校機制），密碼請改用 BCrypt/Argon2。</p>
 */
public class Sha256Util {

    private Sha256Util() {
    }

    /**
     * 計算字串的 SHA-256 雜湊值，回傳 64 位小寫十六進位字串。
     *
     * @param str 來源字串，以 UTF-8 編碼取得位元組；不可為 {@code null}
     * @return 64 位小寫十六進位 SHA-256 字串
     * @throws IllegalArgumentException 若 {@code str} 為 {@code null}
     */
    public static String sha256Hex(String str) {
        if (str == null) {
            throw new IllegalArgumentException("str 不可為 null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexStr = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                // 利用 0x100 補位確保個位數字元前方補零，再取後兩位小寫十六進位字元
                hexStr.append(Integer.toHexString(0x100 + (b & 0xFF)).substring(1));
            }
            return hexStr.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 為 JDK 保證支援的標準演算法，正常執行環境不會發生此例外
            throw new IllegalStateException("JVM 不支援 SHA-256 演算法", e);
        }
    }
}
