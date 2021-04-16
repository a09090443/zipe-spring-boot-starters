package com.zipe.enums;

import java.util.stream.Stream;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/16 下午 05:10
 **/
public enum VerificationTypeEnum {
    NONE, DEFAULT, LDAP;

    public static Stream<VerificationTypeEnum> stream() {
        return Stream.of(VerificationTypeEnum.values());
    }

}
