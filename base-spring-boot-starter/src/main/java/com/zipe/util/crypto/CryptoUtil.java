package com.zipe.util.crypto;

/**
 * 加解密工具類別，封裝 {@link Crypto} 介面，提供統一的加密與解密操作入口。
 * <p>
 * 透過建構子注入不同的 {@link Crypto} 實作（如 AES、3DES、Base64 等），
 * 呼叫端無需關心底層演算法細節，即可透過本類別完成加解密作業。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 上午 09:30
 **/
public class CryptoUtil {
    /** 底層加解密實作，由建構子注入 */
    private Crypto crypto;

    /**
     * 建立 CryptoUtil 實例，並指定所使用的加解密策略。
     *
     * @param crypto 實作 {@link Crypto} 介面的加解密物件（例如 AesUtil、DESedeUtil）
     */
    public CryptoUtil(Crypto crypto) {
        this.crypto = crypto;
    }

    /**
     * 使用預設字元集對字串進行加密。
     *
     * @param str 待加密的明文字串
     * @return 加密後的字串
     */
    public String encrypt(String str) {
        // 以 null 字元集呼叫重載方法，由底層實作決定預設編碼
        return this.encrypt(str, (String)null);
    }

    /**
     * 使用指定字元集對字串進行加密。
     *
     * @param str      待加密的明文字串
     * @param charCode 字元集名稱（例如 {@code "UTF-8"}），傳入 {@code null} 表示使用預設值
     * @return 加密後的字串
     */
    public String encrypt(String str, String charCode) {
        return this.crypto.getEncrypt(str, charCode);
    }

    /**
     * 使用預設字元集對字串進行解密。
     *
     * @param str 待解密的密文字串
     * @return 解密後的明文字串
     */
    public String decode(String str) {
        // 以 null 字元集呼叫重載方法，由底層實作決定預設編碼
        return this.decode(str, (String)null);
    }

    /**
     * 使用指定字元集對字串進行解密。
     *
     * @param str      待解密的密文字串
     * @param charCode 字元集名稱（例如 {@code "UTF-8"}），傳入 {@code null} 表示使用預設值
     * @return 解密後的明文字串
     */
    public String decode(String str, String charCode) {
        return this.crypto.getDecode(str, charCode);
    }
}
