package com.zipe.enums;

import org.apache.commons.lang.StringUtils;

import java.util.stream.Stream;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/16 下午 05:10
 **/
public enum VerificationTypeEnum {
    BASIC, LDAP, CUSTOM;

    public static VerificationTypeEnum getEnum(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        for (VerificationTypeEnum type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        return null;
    }
}
