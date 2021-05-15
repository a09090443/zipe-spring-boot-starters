package com.zipe.util.crypto;

/**
 * @author : Gary Tsai
 **/
public interface Crypto {

    /**
     * 加密
     * @param content
     * @return
     */
    String getEncrypt(String content);

    /**
     * 加密
     * @param content
     * @param charset
     * @return
     */
    String getEncrypt(String content, String charset);

    /**
     * 解密
     * @param content
     * @return
     */
    String getDecode(String content);

    /**
     * 解密
     * @param content
     * @param charset
     * @return
     */
    String getDecode(String content, String charset);
}
