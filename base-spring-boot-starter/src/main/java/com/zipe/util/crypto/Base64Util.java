package com.zipe.util.crypto;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 上午 09:33
 **/
public class Base64Util implements Crypto {
    private static final Logger logger = LoggerFactory.getLogger(Base64Util.class);

    public Base64Util() {
    }

    @Override
    public String getEncrypt(String str) {
        return this.getEncrypt(str, (String) null);
    }

    @Override
    public String getDecode(String str) {
        return this.getDecode(str, (String) null);
    }

    @Override
    public String getEncrypt(String str, String charCode) {
        logger.info("into getBase64Encrypt orgString:{}", str);
        if (StringUtils.isBlank(str)) {
            return "";
        } else {
            byte[] b;
            if (StringUtils.isBlank(charCode)) {
                b = str.getBytes();
            } else {
                try {
                    b = str.getBytes(charCode);
                } catch (UnsupportedEncodingException var5) {
                    b = str.getBytes();
                    logger.error(var5.getMessage());
                }
            }

            return new String(Base64.getEncoder().encode(b));
        }
    }

    @Override
    public String getDecode(String str, String charCode) {
        logger.info("into getBase64Decode encString:{}", str);
        if (StringUtils.isBlank(str)) {
            return "";
        } else {
            byte[] b;
            if (StringUtils.isBlank(charCode)) {
                b = str.getBytes();
            } else {
                try {
                    b = str.getBytes(charCode);
                } catch (UnsupportedEncodingException var5) {
                    b = str.getBytes();
                    logger.error(var5.getMessage());
                }
            }

            return new String(Base64.getDecoder().decode(b));
        }
    }
}
