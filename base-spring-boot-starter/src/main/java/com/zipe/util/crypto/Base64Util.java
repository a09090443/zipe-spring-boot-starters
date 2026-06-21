package com.zipe.util.crypto;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Base64 編解碼工具類別。
 *
 * <p>實作 {@link Crypto} 介面，提供以 Base64 演算法對字串進行編碼（加密）
 * 與解碼（解密）的功能，支援指定字元集或使用系統預設字元集。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 上午 09:33
 **/
@Slf4j
public class Base64Util implements Crypto {

    /**
     * 預設建構子，使用系統預設字元集進行 Base64 操作。
     */
    public Base64Util() {
    }

    /**
     * 使用系統預設字元集，對指定字串進行 Base64 編碼。
     *
     * @param content 待編碼的原始字串
     * @return Base64 編碼後的字串；若 {@code content} 為空白則回傳空字串
     */
    @Override
    public String getEncrypt(String content) {
        return this.getEncrypt(content, null);
    }

    /**
     * 使用系統預設字元集，對指定 Base64 字串進行解碼。
     *
     * @param content 待解碼的 Base64 字串
     * @return 解碼後的原始字串；若 {@code content} 為空白則回傳空字串
     */
    @Override
    public String getDecode(String content) {
        return this.getDecode(content, null);
    }

    /**
     * 使用指定字元集，對指定字串進行 Base64 編碼。
     *
     * <p>若 {@code charset} 為空白，則以系統預設字元集取得位元組陣列；
     * 若指定的字元集不被支援，則退回使用系統預設字元集並記錄錯誤日誌。</p>
     *
     * @param content 待編碼的原始字串
     * @param charset 字元集名稱（例如 {@code "UTF-8"}）；傳入 {@code null} 或空白時使用系統預設值
     * @return Base64 編碼後的字串；若 {@code content} 為空白則回傳空字串
     */
    @Override
    public String getEncrypt(String content, String charset) {
        log.info("into getBase64Encrypt orgString:{}", charset);
        if (StringUtils.isBlank(content)) {
            return "";
        } else {
            byte[] b;
            if (StringUtils.isBlank(charset)) {
                // 未指定字元集，直接使用 JVM 預設字元集取得位元組
                b = content.getBytes();
            } else {
                try {
                    b = content.getBytes(charset);
                } catch (UnsupportedEncodingException var5) {
                    // 指定字元集不受支援時，退回使用 JVM 預設字元集，避免整體流程中斷
                    b = content.getBytes();
                    log.error(var5.getMessage());
                }
            }

            return new String(Base64.getEncoder().encode(b));
        }
    }

    /**
     * 使用指定字元集，對指定 Base64 字串進行解碼。
     *
     * <p>若 {@code charset} 為空白，則以系統預設字元集取得位元組陣列；
     * 若指定的字元集不被支援，則退回使用系統預設字元集並記錄錯誤日誌。</p>
     *
     * @param content 待解碼的 Base64 字串
     * @param charset 字元集名稱（例如 {@code "UTF-8"}）；傳入 {@code null} 或空白時使用系統預設值
     * @return 解碼後的原始字串；若 {@code content} 為空白則回傳空字串
     */
    @Override
    public String getDecode(String content, String charset) {
        log.info("into getBase64Decode encString:{}", content);
        if (StringUtils.isBlank(content)) {
            return "";
        } else {
            byte[] b;
            if (StringUtils.isBlank(charset)) {
                // 未指定字元集，直接使用 JVM 預設字元集取得位元組
                b = content.getBytes();
            } else {
                try {
                    b = content.getBytes(charset);
                } catch (UnsupportedEncodingException var5) {
                    // 指定字元集不受支援時，退回使用 JVM 預設字元集，避免整體流程中斷
                    b = content.getBytes();
                    log.error(var5.getMessage());
                }
            }

            return new String(Base64.getDecoder().decode(b));
        }
    }
}
