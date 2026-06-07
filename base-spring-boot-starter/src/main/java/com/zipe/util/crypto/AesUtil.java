package com.zipe.util.crypto;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * @author Gary Tsai
 * @Description: AES加密和解密工具類
 * <p>
 * 採用 AES/CBC/PKCS5Padding，每次加密皆以 {@link SecureRandom} 產生隨機 16-byte IV，
 * 字串輸出格式為 Base64(IV || ciphertext)，檔案輸出則在密文前以明文寫入 16-byte IV。
 * 因此相同明文每次加密皆會得到不同密文，避免 CBC 退化為確定性加密。
 */
public class AesUtil implements Crypto {

    public AesUtil(String secretKey) {
        this.secretKey = secretKey;
    }

    // 密鑰：建立後不可變更
    private final String secretKey;

    // 算法方式
    private static final String KEY_ALGORITHM = "AES";

    // 算法/模式/填充
    private static final String CIPHER_ALGORITHM_CBC = "AES/CBC/PKCS5Padding";

    // 私鑰大小，僅支援 128bits 即 16bytes
    private static final Integer PRIVATE_KEY_SIZE_BYTE = 16;

    // IV 長度（AES 區塊大小固定為 16 bytes）
    private static final int IV_SIZE_BYTE = 16;

    public static final int BUFFER_SIZE = 512;

    /**
     * 初始化密碼器。
     *
     * @param mode 加密模式：{@link Cipher#ENCRYPT_MODE} 或 {@link Cipher#DECRYPT_MODE}
     * @param iv   初始化向量（16 bytes）
     * @return 已初始化的 {@link Cipher}
     */
    private Cipher initParam(int mode, byte[] iv) {
        try {
            // 獲得原始對稱密鑰的字節數組
            byte[] raw = secretKey.getBytes();

            // 根據字節數組生成AES內部密鑰
            SecretKeySpec key = new SecretKeySpec(raw, KEY_ALGORITHM);
            // 根據指定算法"AES/CBC/PKCS5Padding"實例化密碼器
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_CBC);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 初始化AES密碼器
            cipher.init(mode, key, ivSpec);

            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("AESUtil:initParam fail!", e);
        }
    }

    /**
     * 產生隨機 16-byte IV。
     */
    private static byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE_BYTE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 確認密鑰長度是否為 16 bytes。
     */
    private void checkKeyLength() {
        if (secretKey.length() != PRIVATE_KEY_SIZE_BYTE) {
            throw new RuntimeException("AESUtil:Invalid AES secretKey length (must be 16 bytes)");
        }
    }

    /**
     * @param content 明文：要加密的內容
     * @return 密文：Base64(IV || ciphertext)
     * @Description: 加密
     */
    @Override
    public String getEncrypt(String content) {
        return getEncrypt(content, null);
    }

    /**
     * @param content 明文：要加密的內容
     * @param charset 字符
     * @return cipherText 密文：Base64(IV || ciphertext)
     * @Description: 加密
     */
    @Override
    public String getEncrypt(String content, String charset) {
        checkKeyLength();

        try {
            // 每次加密皆產生隨機 IV
            byte[] iv = generateIv();
            Cipher cipher = initParam(Cipher.ENCRYPT_MODE, iv);

            byte[] bytePlainText;
            if (StringUtils.isBlank(charset)) {
                bytePlainText = content.getBytes();
            } else {
                bytePlainText = content.getBytes(charset);
            }

            // 執行加密
            byte[] byteCipherText = cipher.doFinal(bytePlainText);

            // 輸出格式：IV || ciphertext，再以 Base64 編碼
            byte[] ivAndCipher = new byte[iv.length + byteCipherText.length];
            System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
            System.arraycopy(byteCipherText, 0, ivAndCipher, iv.length, byteCipherText.length);

            return Base64.encodeBase64String(ivAndCipher);
        } catch (Exception e) {
            throw new RuntimeException("AESUtil:encrypt fail!", e);
        }
    }

    /**
     * @param content 密文：Base64(IV || ciphertext)，即需要解密的內容
     * @return 明文：解密後的內容
     * @Description: 解密
     */
    @Override
    public String getDecode(String content) {
        return getDecode(content, null);
    }

    /**
     * @param content 密文：Base64(IV || ciphertext)
     * @param charset 字符
     * @return plainText 明文：解密後的內容
     * @Description: 解密
     */
    @Override
    public String getDecode(String content, String charset) {
        checkKeyLength();

        try {
            // 將加密並編碼後的內容解碼成字節數組
            byte[] ivAndCipher = Base64.decodeBase64(content);
            if (ivAndCipher.length <= IV_SIZE_BYTE) {
                throw new RuntimeException("AESUtil:Invalid cipher text (missing IV prefix)");
            }

            // 取前 16 bytes 還原 IV，其餘為密文本體
            byte[] iv = new byte[IV_SIZE_BYTE];
            byte[] byteCipherText = new byte[ivAndCipher.length - IV_SIZE_BYTE];
            System.arraycopy(ivAndCipher, 0, iv, 0, IV_SIZE_BYTE);
            System.arraycopy(ivAndCipher, IV_SIZE_BYTE, byteCipherText, 0, byteCipherText.length);

            Cipher cipher = initParam(Cipher.DECRYPT_MODE, iv);
            byte[] bytePlainText = cipher.doFinal(byteCipherText);

            if (StringUtils.isBlank(charset)) {
                return new String(bytePlainText);
            }
            return new String(bytePlainText, charset);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AESUtil:decrypt fail!", e);
        }
    }

    /**
     * @param source 來源檔案
     * @param target 加密檔案
     * @Description: 檔案加密；輸出檔案開頭為 16-byte 明文 IV，其後為密文。
     */
    public void encryptFile(File source, File target) throws Exception {
        checkKeyLength();
        checkPath(source, target);
        byte[] iv = generateIv();
        Cipher cipher = initParam(Cipher.ENCRYPT_MODE, iv);
        try (FileOutputStream fos = new FileOutputStream(target);
             CipherInputStream cis = new CipherInputStream(Files.newInputStream(source.toPath()), cipher)) {
            // 先寫入明文 IV，解密時據此還原
            fos.write(iv);
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
        }
    }

    /**
     * @param source 加密檔案（開頭為 16-byte 明文 IV）
     * @param target 解密檔案
     * @Description: 檔案解密
     */
    public void decryptFile(File source, File target) throws Exception {
        checkKeyLength();
        checkPath(source, target);
        try (FileInputStream fis = new FileInputStream(source)) {
            byte[] iv = readIv(fis);
            Cipher cipher = initParam(Cipher.DECRYPT_MODE, iv);
            try (CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(target), cipher)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, len);
                }
                cos.flush();
            }
        }
    }

    /**
     * @param source 加密檔案（開頭為 16-byte 明文 IV）
     * @return 解密後內容的 {@link ByteArrayInputStream}
     * @Description: 檔案解密為輸入串流
     */
    public ByteArrayInputStream decryptFile(File source) throws Exception {
        checkKeyLength();
        try (FileInputStream fis = new FileInputStream(source)) {
            byte[] iv = readIv(fis);
            Cipher cipher = initParam(Cipher.DECRYPT_MODE, iv);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (CipherOutputStream cos = new CipherOutputStream(baos, cipher)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, len);
                }
            }
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }

    /**
     * 從串流開頭讀取 16-byte IV。
     */
    private static byte[] readIv(FileInputStream fis) throws IOException {
        byte[] iv = new byte[IV_SIZE_BYTE];
        int read = 0;
        while (read < IV_SIZE_BYTE) {
            int n = fis.read(iv, read, IV_SIZE_BYTE - read);
            if (n == -1) {
                throw new IOException("AESUtil:Invalid encrypted file (missing IV prefix)");
            }
            read += n;
        }
        return iv;
    }

    /**
     * @param source 來源檔案
     * @param target 目標檔案
     * @Description: 確認來源和目標檔案類型及是否存在
     */
    public static void checkPath(File source, File target) throws IOException {
        Objects.requireNonNull(source);
        Objects.requireNonNull(target);
        if (source.isDirectory() || !source.exists()) {
            throw new FileNotFoundException(source.toString());
        }
        if (Objects.equals(source.getCanonicalPath(), target.getCanonicalPath())) {
            throw new IllegalArgumentException("sourceFile equals targetFile");
        }
        File parentDirectory = target.getParentFile();
        if (parentDirectory != null && !parentDirectory.exists()) {
            Files.createDirectories(parentDirectory.toPath());
        }
    }

}
