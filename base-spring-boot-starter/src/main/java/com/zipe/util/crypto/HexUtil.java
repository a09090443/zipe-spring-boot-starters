package com.zipe.util.crypto;

/**
 * @Description 16进制工具类
 */
public class HexUtil {

    /**
     * @param bytes byte二进制
     * @return hex 十六进制
     * @Description byte二进制 转 hex 十六进制
     * @author 王鑫
     */
    public static String byte2hex(byte[] bytes) {
        StringBuilder hexStr = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            hexStr.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF)).substring(1).toUpperCase());
        }
        return hexStr.toString();
    }

    /**
     * @param hex hex十六进制
     * @return byte二进制
     * @Description hex十六进制 转 byte二进制
     * @author 王鑫
     */
    public static byte[] hex2byte(String hex) {
        if (isBlank(hex)) {
            return null;
        }
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }
}
