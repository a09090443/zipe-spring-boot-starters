package com.zipe.util.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 雜湊工具類。
 * <p>
 * <b>資安警告：</b>MD5 已被證實存在碰撞弱點，<b>禁止用於密碼雜湊或數位簽章</b>等
 * 任何安全用途。如需密碼雜湊請改用 BCrypt/Argon2，如需完整性或簽章請改用
 * SHA-256 以上演算法。本類別僅可用於非安全用途（例如產生快取鍵、檔案去重比對等）。
 *
 * @deprecated MD5 不具備密碼學安全性，請勿用於密碼或完整性保護。
 */
@Deprecated
public class Md5Util {
    /**
     * 將字串計算為 32 位小寫 MD5 雜湊值。
     *
     * @param str 來源字串
     * @return 32 位小寫十六進位 MD5 字串；若發生演算法例外則回傳 {@code null}
     * @deprecated 禁止用於密碼雜湊或簽章，僅可作非安全用途。
     */
    @Deprecated
    public static String parseStrToMd5L32(String str) {
        String reStr = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(str.getBytes());
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bytes) {
                // 將有號 byte 轉為無號整數，避免負數造成十六進位字串錯誤
                int bt = b & 0xff;
                // 若值小於 16（即單一十六進位字元），補上前導零以維持固定 2 字元寬度
                if (bt < 16) {
                    stringBuffer.append(0);
                }
                stringBuffer.append(Integer.toHexString(bt));
            }
            reStr = stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return reStr;
    }

    /**
     * 將字串計算為 32 位大寫 MD5 雜湊值。
     *
     * @param str 來源字串
     * @return 32 位大寫十六進位 MD5 字串；若發生演算法例外則回傳 {@code null}
     * @deprecated 禁止用於密碼雜湊或簽章，僅可作非安全用途。
     */
    @Deprecated
    public static String parseStrToMd5U32(String str) {
        String reStr = parseStrToMd5L32(str);
        if (reStr != null) {
            reStr = reStr.toUpperCase();
        }
        return reStr;
    }

    /**
     * 將字串計算為 16 位大寫 MD5 雜湊值（取 32 位結果的中段 8～24 字元）。
     * <p>
     * 注意：方法名稱雖含 "U16"，實際上回傳的是大寫格式；
     * 16 位 MD5 是截取 32 位完整 MD5 的第 9 至 24 個字元所得。
     *
     * @param str 來源字串
     * @return 16 位大寫十六進位 MD5 字串；若發生演算法例外則回傳 {@code null}
     * @deprecated 禁止用於密碼雜湊或簽章，僅可作非安全用途。
     */
    @Deprecated
    public static String parseStrToMd5U16(String str) {
        String reStr = parseStrToMd5L32(str);
        if (reStr != null) {
            reStr = reStr.toUpperCase().substring(8, 24);
        }
        return reStr;
    }

    /**
     * 將字串計算為 16 位小寫 MD5 雜湊值（取 32 位結果的中段 8～24 字元）。
     * <p>
     * 注意：方法名稱雖含 "L16"，實際上回傳的是小寫格式；
     * 16 位 MD5 是截取 32 位完整 MD5 的第 9 至 24 個字元所得。
     *
     * @param str 來源字串
     * @return 16 位小寫十六進位 MD5 字串；若發生演算法例外則回傳 {@code null}
     * @deprecated 禁止用於密碼雜湊或簽章，僅可作非安全用途。
     */
    @Deprecated
    public static String parseStrToMd5L16(String str) {
        String reStr = parseStrToMd5L32(str);
        if (reStr != null) {
            reStr = reStr.substring(8, 24);
        }
        return reStr;
    }
}
