package com.zipe.util.crypto;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 上午 09:30
 **/
public class CryptoUtil {
    private Crypto crypto;

    public CryptoUtil(Crypto crypto) {
        this.crypto = crypto;
    }

    public String encrypt(String str) {
        return this.encrypt(str, (String)null);
    }

    public String encrypt(String str, String charCode) {
        return this.crypto.getEncrypt(str, charCode);
    }

    public String decode(String str) {
        return this.decode(str, (String)null);
    }

    public String decode(String str, String charCode) {
        return this.crypto.getDecode(str, charCode);
    }
}
