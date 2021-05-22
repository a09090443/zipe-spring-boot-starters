package com.example.util.crypto

import com.zipe.util.crypto.AesUtil
import com.zipe.util.crypto.Base64Util
import com.zipe.util.crypto.CryptoUtil
import com.zipe.util.crypto.DESedeUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class CryptoUtilTest {

    @Test
    @Order(1)
    fun `Aes Test`() {
        val secretKey = "1234567890123456"
        val oriContent = "Gary"

        val cryptoUtil = CryptoUtil(AesUtil(secretKey))
        val encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name())

        val decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name())
        Assertions.assertEquals(oriContent, decryptMsg)
    }

    @Test
    @Order(2)
    fun `Triple Des Test`(){
        val secretKey = "098f6bcd4621d373cade4e832627b4f62017121611345734"
        val oriContent = "Gary"

        val cryptoUtil = CryptoUtil(DESedeUtil(secretKey))
        val encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name())

        val decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name())
        Assertions.assertEquals(oriContent, decryptMsg)
    }

    @Test
    @Order(3)
    fun `Base64 Test`(){
        val oriContent = "Gary"

        val cryptoUtil = CryptoUtil(Base64Util())
        val encryptMsg = cryptoUtil.encrypt(oriContent, StandardCharsets.UTF_8.name())

        val decryptMsg = cryptoUtil.decode(encryptMsg, StandardCharsets.UTF_8.name())
        Assertions.assertEquals(oriContent, decryptMsg)
    }
}
