package com.zipe.util.crypto;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * @author Gary Tsai
 * @Description: AES加密和解密工具類
 */
public class AesUtil implements Crypto {

    public AesUtil(String secretKey) {
        this.secretKey = secretKey;
    }

    public String secretKey;
    // 算法方式
    private static final String KEY_ALGORITHM = "AES";

    // 算法/模式/填充
    private static final String CIPHER_ALGORITHM_CBC = "AES/CBC/PKCS5Padding";

    // 私鑰大小128/192/256(bits)位 即：16/24/32bytes，暫時使用128，如果擴大需要更換java/jre裡面的jar包
    private static final Integer PRIVATE_KEY_SIZE_BIT = 128;

    private static final Integer PRIVATE_KEY_SIZE_BYTE = 16;

    /**
     * 初始化參數
     *
     * @param secretKey 密鑰：加密的規則 16位
     * @param mode      加密模式：加密or解密
     */
    public Cipher initParam(String secretKey, int mode) {
        try {
            // 防止Linux下生成隨機key
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(secretKey.getBytes());
            // 獲取key生成器
            KeyGenerator keygen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keygen.init(PRIVATE_KEY_SIZE_BIT, secureRandom);

            // 獲得原始對稱密鑰的字節數組
            byte[] raw = secretKey.getBytes();

            // 根據字節數組生成AES內部密鑰
            SecretKeySpec key = new SecretKeySpec(raw, KEY_ALGORITHM);
            // 根據指定算法"AES/CBC/PKCS5Padding"實例化密碼器
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_CBC);
            IvParameterSpec iv = new IvParameterSpec(secretKey.getBytes());

            // 初始化AES密碼器
            cipher.init(mode, key, iv);

            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("AESUtil:initParam fail!", e);
        }
    }

    /**
     * @param content 明文：要加密的內容
     * @return 密文：加密後的內容，如有異常返回空串：""
     * @Description: 加密
     */
    @Override
    public String getEncrypt(String content) {
        return getEncrypt(content, null);
    }

    /**
     * @param content 明文：要加密的內容
     * @param charset 字符
     * @return cipherText 密文：加密後的內容，如有異常返回空串：""
     * @Description: 加密
     */
    @Override
    public String getEncrypt(String content, String charset) {
        if (secretKey.length() != PRIVATE_KEY_SIZE_BYTE) {
            throw new RuntimeException("AESUtil:Invalid AES secretKey length (must be 16 bytes)");
        }

        // 密文字符串
        String cipherText = "";
        try {
            // 加密模式初始化参数
            Cipher cipher = initParam(secretKey, Cipher.ENCRYPT_MODE);
            // 加密模式初始化參數
            byte[] bytePlainText;

            if (StringUtils.isBlank(charset)) {
                bytePlainText = content.getBytes();
            } else {
                bytePlainText = content.getBytes(charset);
            }

            // 執行加密
            byte[] byteCipherText = cipher.doFinal(bytePlainText);
            cipherText = Base64.encodeBase64String(byteCipherText);
        } catch (Exception e) {
            throw new RuntimeException("AESUtil:encrypt fail!", e);
        }
        return cipherText;
    }

    /**
     * @param content 密文：加密後的內容，即需要解密的內容
     * @return 明文：解密後的內容即加密前的內容，如有異常返回空串：""
     * @Description: 解密
     */
    @Override
    public String getDecode(String content) {
        return getDecode(content, null);
    }

    /**
     * @param content 明文：要加密的內容
     * @param charset 字符
     * @return cipherText 密文：加密後的內容，如有異常返回空串：""
     * @Description: 加密
     */
    @Override
    public String getDecode(String content, String charset) {

        if (secretKey.length() != PRIVATE_KEY_SIZE_BYTE) {
            throw new RuntimeException("AESUtil:Invalid AES secretKey length (must be 16 bytes)");
        }

        // 明文字符串
        String plainText = "";
        try {
            Cipher cipher = initParam(secretKey, Cipher.DECRYPT_MODE);
            // 將加密並編碼後的內容解碼成字節數組
            byte[] byteCipherText = Base64.decodeBase64(content);
            // 解密
            byte[] bytePlainText = cipher.doFinal(byteCipherText);

            if (StringUtils.isBlank(charset)) {
                plainText = new String(bytePlainText);
            } else {
                plainText = new String(bytePlainText, charset);
            }
        } catch (Exception e) {
            throw new RuntimeException("AESUtil:decrypt fail!", e);
        }
        return plainText;
    }


    public static void main(String[] args) {
        long s = System.currentTimeMillis();
        AesUtil aesUtil = new AesUtil("asdasdadetsaacac");
        String encryptMsg = aesUtil.getEncrypt("aaa", StandardCharsets.UTF_8.name());
        System.out.println("密文为：" + encryptMsg);

        long e = System.currentTimeMillis();

        System.out.println(e - s);

        String decryptMsg = aesUtil.getDecode(encryptMsg, StandardCharsets.UTF_8.name());
        System.out.println("明文为：" + decryptMsg);

        long d = System.currentTimeMillis();

        System.out.println(d - e);
    }

}
