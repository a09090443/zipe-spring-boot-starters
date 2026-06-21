package com.example.util.crypto

import com.zipe.util.crypto.AesUtil
import com.zipe.util.crypto.Base64Util
import com.zipe.util.crypto.CryptoUtil
import com.zipe.util.crypto.DESedeUtil
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

/**
 * base-starter 加解密工具測試：分別以 AES、3DES、Base64 編解碼後，
 * 驗證解密結果與原始內容一致。
 */
class CryptoUtilTest : FunSpec({

    val log = LoggerFactory.getLogger(CryptoUtilTest::class.java)

    test("aesTest") {
        // The secret key must have 16 bytes
        val secretKey = "1234567890123456"
        val oriContent = "Gary"
        val cryptoUtil = CryptoUtil(AesUtil(secretKey))
        val encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name())
        log.info("AESEncrypt:{}", encryptMsg)

        val decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name())
        log.info("AESDecrypt:{}", decryptMsg)

        decryptMsg shouldBe oriContent
    }

    test("tripleTest") {
        // The secret key must have 48 bytes
        val secretKey = "098f6bcd4621d373cade4e832627b4f62017121611345734"
        val oriContent = "Gary"
        val cryptoUtil = CryptoUtil(DESedeUtil(secretKey))
        val encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name())
        log.info("3DESEncrypt:{}", encryptMsg)

        val decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name())
        log.info("3DESDecrypt:{}", decryptMsg)

        decryptMsg shouldBe oriContent
    }

    test("base64Test") {
        val oriContent = "Gary"
        val cryptoUtil = CryptoUtil(Base64Util())
        val encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name())
        log.info("Base64Encrypt:{}", encryptMsg)

        val decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name())
        log.info("Base64Decrypt:{}", decryptMsg)

        decryptMsg shouldBe oriContent
    }
})
