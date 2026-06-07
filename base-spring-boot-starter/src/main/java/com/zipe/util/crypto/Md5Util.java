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
     * @param str 來源字串
     * @Description: 32位小寫MD5
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
                int bt = b & 0xff;
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
     * @param str 來源字串
     * @Description: 32位大寫MD5
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
     * @param str 來源字串
     * @Description: 16位小寫MD5
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
     * @param str 來源字串
     * @Description: 16位大寫MD5
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
