package com.zipe.util.crypto;

import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * @Description 3DES算法工具类
 */
public class DESedeUtil implements Crypto {

    public DESedeUtil(String secretKey) {
        this.secretKey = secretKey;
    }

    public String secretKey;

    private static final Integer PRIVATE_KEY_SIZE_BYTE = 48;
    // 算法方式
    private static final String KEY_ALGORITHM = "DESede";
    // 算法/模式/填充
    private static final String CIPHER_ALGORITEM = "DESede/ECB/PKCS5Padding";

    private static Key getKey(String key) throws Exception {
        byte[] keyByte = HexUtil.hex2byte(key);
        DESedeKeySpec dks = new DESedeKeySpec(keyByte);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        return skf.generateSecret(dks);
    }

    /**
     * 加密
     *
     * @param content 需加密內容
     * @return 已加密內容
     */
    @Override
    public String getEncrypt(String content) {
        return getEncrypt(content, null);
    }

    /**
     * 加密
     *
     * @param content 需加密內容
     * @param charset 字符
     * @return 已加密內容
     */
    @Override
    public String getEncrypt(String content, String charset) {
        if (secretKey.length() != PRIVATE_KEY_SIZE_BYTE) {
            throw new RuntimeException("DESedeUtil:Invalid 3DES secretKey length (must be 48 bytes)");
        }

        try {
            byte[] encryptByte;
            if (StringUtils.isBlank(charset)) {
                encryptByte = content.getBytes();
            } else {
                encryptByte = content.getBytes(charset);
            }
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITEM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(secretKey));
            return HexUtil.byte2hex(cipher.doFinal(encryptByte));
        } catch (Exception e) {
            System.err.println("# 3DES加密失敗");
            return null;
        }
    }

    @Override
    public String getDecode(String content) {
        return getDecode(content, null);
    }

    /**
     * 解密
     *
     * @param content 需解密內容
     * @return 已解密內容
     */
    @Override
    public String getDecode(String content, String charset) {
        if (secretKey.length() != PRIVATE_KEY_SIZE_BYTE) {
            throw new RuntimeException("DESedeUtil:Invalid 3DES secretKey length (must be 48 bytes)");
        }

        try {
            byte[] hexByte = HexUtil.hex2byte(content);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITEM);
            cipher.init(Cipher.DECRYPT_MODE, getKey(secretKey));
            String decodeContent;
            if (StringUtils.isBlank(charset)) {
                decodeContent = new String(cipher.doFinal(hexByte));
            } else {
                decodeContent = new String(cipher.doFinal(hexByte), charset);
            }
            return decodeContent;
        } catch (Exception e) {
            System.err.println("# 3DES解密失敗");
            return null;
        }
    }

    public static void main(String[] args) {
        DESedeUtil deSedeUtil = new DESedeUtil("098f6bcd4621d373cade4e832627b4f62017121611345734");
        String param = "Gary";
        String encr = deSedeUtil.getEncrypt(param, StandardCharsets.UTF_8.name());
        System.out.println(param + " ==>> " + encr);
        String decr = deSedeUtil.getDecode(encr, StandardCharsets.UTF_8.name());
        System.out.println(encr + " ==>> " + decr);
    }

}
