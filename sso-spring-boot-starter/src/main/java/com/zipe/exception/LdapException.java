package com.zipe.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/12/15 上午 10:33
 **/
public class LdapException extends AuthenticationException {
    private static final long serialVersionUID = 1L;

    public LdapException(String msg, Throwable t) {
        super(msg, t);
    }

    public LdapException(String msg) {
        super(msg);
    }
}
