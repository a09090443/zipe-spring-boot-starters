package com.zipe.util.crypto;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 上午 09:33
 **/
@Slf4j
public class Base64Util implements Crypto {

    public Base64Util() {
    }

    @Override
    public String getEncrypt(String content) {
        return this.getEncrypt(content, null);
    }

    @Override
    public String getDecode(String content) {
        return this.getDecode(content, null);
    }

    @Override
    public String getEncrypt(String content, String charset) {
        log.info("into getBase64Encrypt orgString:{}", charset);
        if (StringUtils.isBlank(content)) {
            return "";
        } else {
            byte[] b;
            if (StringUtils.isBlank(charset)) {
                b = content.getBytes();
            } else {
                try {
                    b = content.getBytes(charset);
                } catch (UnsupportedEncodingException var5) {
                    b = content.getBytes();
                    log.error(var5.getMessage());
                }
            }

            return new String(Base64.getEncoder().encode(b));
        }
    }

    @Override
    public String getDecode(String content, String charset) {
        log.info("into getBase64Decode encString:{}", content);
        if (StringUtils.isBlank(content)) {
            return "";
        } else {
            byte[] b;
            if (StringUtils.isBlank(charset)) {
                b = content.getBytes();
            } else {
                try {
                    b = content.getBytes(charset);
                } catch (UnsupportedEncodingException var5) {
                    b = content.getBytes();
                    log.error(var5.getMessage());
                }
            }

            return new String(Base64.getDecoder().decode(b));
        }
    }
}
