package com.zipe.util.crypto;

/**
 * 加解密統一介面，定義各類加密演算法（AES、3DES、Base64 等）的共通操作契約。
 * <p>
 * 實作此介面的類別須提供「指定明文 → 密文」的加密方法，
 * 以及「指定密文 → 明文」的解密方法，並分別支援預設字元集與自訂字元集兩種呼叫方式。
 * </p>
 *
 * @author : Gary Tsai
 **/
public interface Crypto {

    /**
     * 使用預設字元集對指定內容進行加密。
     *
     * @param content 待加密的明文字串
     * @return 加密後的密文字串
     */
    String getEncrypt(String content);

    /**
     * 使用指定字元集對指定內容進行加密。
     *
     * @param content 待加密的明文字串
     * @param charset 字元集名稱（例如 "UTF-8"、"Big5"）
     * @return 加密後的密文字串
     */
    String getEncrypt(String content, String charset);

    /**
     * 使用預設字元集對指定密文進行解密。
     *
     * @param content 待解密的密文字串
     * @return 解密後的明文字串
     */
    String getDecode(String content);

    /**
     * 使用指定字元集對指定密文進行解密。
     *
     * @param content 待解密的密文字串
     * @param charset 字元集名稱（例如 "UTF-8"、"Big5"）
     * @return 解密後的明文字串
     */
    String getDecode(String content, String charset);
}
