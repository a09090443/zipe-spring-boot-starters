package com.zipe.util.crypto;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 上午 09:32
 **/
public interface Crypto {
    String getEncrypt(String var1);

    String getEncrypt(String var1, String var2);

    String getDecode(String var1);

    String getDecode(String var1, String var2);
}
