package com.example.util.crypto;

import com.zipe.util.crypto.AesUtil;
import com.zipe.util.crypto.Base64Util;
import com.zipe.util.crypto.CryptoUtil;
import com.zipe.util.crypto.DESedeUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

@Slf4j
public class CryptoUtilTest {

    @Test
    public void aesTest() {
        // The secret key must have 16 bytes
        String secretKey = "1234567890123456";
        String oriContent = "Gary";
        CryptoUtil cryptoUtil = new CryptoUtil(new AesUtil(secretKey));
        String encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name());
        log.info("AESEncrypt:{}", encryptMsg);

        String decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name());
        log.info("AESDecrypt:{}", decryptMsg);

        Assertions.assertEquals(oriContent, decryptMsg);
    }

    @Test
    public void tripleTest() {
        // The secret key must have 48 bytes
        String secretKey = "098f6bcd4621d373cade4e832627b4f62017121611345734";
        String oriContent = "Gary";
        CryptoUtil cryptoUtil = new CryptoUtil(new DESedeUtil(secretKey));
        String encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name());
        log.info("3DESEncrypt:{}", encryptMsg);

        String decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name());
        log.info("3DESDecrypt:{}", decryptMsg);

        Assertions.assertEquals(oriContent, decryptMsg);
    }

    @Test
    public void base64Test() {
        String oriContent = "Gary";
        CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
        String encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name());
        log.info("Base64Encrypt:{}", encryptMsg);

        String decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name());
        log.info("Base64Decrypt:{}", decryptMsg);

        Assertions.assertEquals(oriContent, decryptMsg);
    }
}
