package com.zipe.util.crypto;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.security.SecureRandom;

/**
 * 3DES（Triple DES）加解密工具類別。
 * <p>
 * 採用 DESede/CBC/PKCS5Padding，每次加密皆以 {@link SecureRandom} 產生隨機 8-byte IV，
 * 輸出格式為 Hex(IV || ciphertext)，解密時先取前 8 bytes 還原 IV。
 * <p>
 * 注意：3DES（Triple DES）已屬淘汰演算法（區塊長度僅 64-bit，安全強度不足），
 * 新專案請改用 {@link AesUtil}。本類別僅為相容既有系統而保留。
 *
 * @Description 3DES算法工具類
 */
public class DESedeUtil implements Crypto {

    /**
     * 建立 DESedeUtil 實例。
     *
     * @param secretKey 16 進位格式的 3DES 密鑰字串，長度必須為 48 個 Hex 字元（對應 24 bytes）
     */
    public DESedeUtil(String secretKey) {
        this.secretKey = secretKey;
    }

    // 密鑰：建立後不可變更
    private final String secretKey;

    /** 3DES 密鑰的 Hex 字串長度（24 bytes × 2 = 48 個 Hex 字元） */
    private static final Integer PRIVATE_KEY_SIZE_BYTE = 48;
    // 算法方式
    private static final String KEY_ALGORITHM = "DESede";
    /** 加解密演算法／模式／填充方式：CBC 模式搭配 PKCS5 填充 */
    private static final String CIPHER_ALGORITEM = "DESede/CBC/PKCS5Padding";
    // IV 長度（DESede 區塊大小固定為 8 bytes）
    private static final int IV_SIZE_BYTE = 8;

    /**
     * 將 16 進位格式的密鑰字串轉換為 JCE {@link Key} 物件。
     *
     * @param key 16 進位格式的 3DES 密鑰字串
     * @return 可供 {@link Cipher} 使用的 {@link Key} 物件
     * @throws Exception 密鑰格式不合法或演算法不支援時拋出
     */
    private static Key getKey(String key) throws Exception {
        byte[] keyByte = HexUtil.hex2byte(key);
        DESedeKeySpec dks = new DESedeKeySpec(keyByte);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        return skf.generateSecret(dks);
    }

    /**
     * 產生隨機 8-byte IV。
     */
    private static byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE_BYTE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 加密
     *
     * @param content 需加密內容
     * @return 已加密內容：Hex(IV || ciphertext)
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
     * @return 已加密內容：Hex(IV || ciphertext)
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

            // 每次加密皆產生隨機 IV
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITEM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(secretKey), new IvParameterSpec(iv));
            byte[] cipherByte = cipher.doFinal(encryptByte);

            // 輸出格式：IV || ciphertext，再以 Hex 編碼
            byte[] ivAndCipher = new byte[iv.length + cipherByte.length];
            System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
            System.arraycopy(cipherByte, 0, ivAndCipher, iv.length, cipherByte.length);

            return HexUtil.byte2hex(ivAndCipher);
        } catch (Exception e) {
            throw new RuntimeException("DESedeUtil:encrypt fail!", e);
        }
    }

    /**
     * 解密，使用系統預設字元編碼還原明文。
     *
     * @param content 需解密內容：Hex(IV || ciphertext)
     * @return 已解密的明文字串
     */
    @Override
    public String getDecode(String content) {
        return getDecode(content, null);
    }

    /**
     * 解密
     *
     * @param content 需解密內容：Hex(IV || ciphertext)
     * @param charset 字符
     * @return 已解密內容
     */
    @Override
    public String getDecode(String content, String charset) {
        if (secretKey.length() != PRIVATE_KEY_SIZE_BYTE) {
            throw new RuntimeException("DESedeUtil:Invalid 3DES secretKey length (must be 48 bytes)");
        }

        try {
            byte[] ivAndCipher = HexUtil.hex2byte(content);
            if (ivAndCipher == null || ivAndCipher.length <= IV_SIZE_BYTE) {
                throw new RuntimeException("DESedeUtil:Invalid cipher text (missing IV prefix)");
            }

            // 取前 8 bytes 還原 IV，其餘為密文本體
            byte[] iv = new byte[IV_SIZE_BYTE];
            byte[] cipherByte = new byte[ivAndCipher.length - IV_SIZE_BYTE];
            System.arraycopy(ivAndCipher, 0, iv, 0, IV_SIZE_BYTE);
            System.arraycopy(ivAndCipher, IV_SIZE_BYTE, cipherByte, 0, cipherByte.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITEM);
            cipher.init(Cipher.DECRYPT_MODE, getKey(secretKey), new IvParameterSpec(iv));
            byte[] plainByte = cipher.doFinal(cipherByte);

            if (StringUtils.isBlank(charset)) {
                return new String(plainByte);
            }
            return new String(plainByte, charset);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("DESedeUtil:decrypt fail!", e);
        }
    }

}
