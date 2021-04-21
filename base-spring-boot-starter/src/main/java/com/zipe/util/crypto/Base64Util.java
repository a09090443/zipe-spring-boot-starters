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
    public String getEncrypt(String str) {
        return this.getEncrypt(str, (String) null);
    }

    @Override
    public String getDecode(String str) {
        return this.getDecode(str, (String) null);
    }

    @Override
    public String getEncrypt(String str, String charCode) {
        log.info("into getBase64Encrypt orgString:{}", str);
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
                    log.error(var5.getMessage());
                }
            }

            return new String(Base64.getEncoder().encode(b));
        }
    }

    @Override
    public String getDecode(String str, String charCode) {
        log.info("into getBase64Decode encString:{}", str);
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
                    log.error(var5.getMessage());
                }
            }

            return new String(Base64.getDecoder().decode(b));
        }
    }
}
