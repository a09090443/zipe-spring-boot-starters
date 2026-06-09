package com.zipe.util.crypto;

/**
 * 十六進位（Hex）工具類別。
 *
 * <p>提供位元組陣列與十六進位字串之間的相互轉換，
 * 以及輔助用的空白字串判斷方法。常用於加解密模組的金鑰與密文格式轉換。</p>
 *
 * @Description 16進制工具
 */
public class HexUtil {

    /**
     * 將位元組陣列轉換為大寫十六進位字串。
     *
     * <p>每個位元組以兩個十六進位字元表示（例如 {@code 0x0A} → {@code "0A"}）。</p>
     *
     * @param bytes 來源位元組陣列（二進位資料）
     * @return 對應的大寫十六進位字串
     * @Description byte二進制 轉 hex 十六進制
     */
    public static String byte2hex(byte[] bytes) {
        StringBuilder hexStr = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            // 利用 0x0100 補位確保個位數字元前方補零，再取後兩位並轉大寫
            hexStr.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF)).substring(1).toUpperCase());
        }
        return hexStr.toString();
    }

    /**
     * 將十六進位字串轉換為位元組陣列。
     *
     * <p>每兩個十六進位字元解析為一個位元組（例如 {@code "0A"} → {@code 0x0A}）。
     * 若輸入字串為空白或 {@code null}，則回傳 {@code null}。</p>
     *
     * @param hex 來源十六進位字串（長度應為偶數）
     * @return 對應的位元組陣列；若輸入為空白則回傳 {@code null}
     * @Description hex十六進制 轉 byte二進制
     */
    public static byte[] hex2byte(String hex) {
        if (isBlank(hex)) {
            return null;
        }
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            // 每次取兩個字元，以基數 16 解析為整數後強制轉型為 byte
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    /**
     * 判斷字串是否為空白（{@code null}、長度為零，或全為空白字元）。
     *
     * @param str 待判斷的字串
     * @return 若字串為 {@code null}、空字串或全為空白字元則回傳 {@code true}，否則回傳 {@code false}
     */
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
