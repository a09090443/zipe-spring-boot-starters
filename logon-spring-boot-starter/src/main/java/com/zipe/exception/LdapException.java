package com.zipe.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * LDAP 驗證例外，專門用於封裝 LDAP 目錄服務驗證失敗時所拋出的例外。
 * <p>
 * 繼承自 Spring Security 的 {@link AuthenticationException}，
 * 使其可被 Spring Security 的例外處理機制統一攔截，
 * 並交由登入失敗處理器（{@code LoginFailureHandler}）進行對應處理。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/12/15 上午 10:33
 **/
public class LdapException extends AuthenticationException {
    private static final long serialVersionUID = 1L;

    /**
     * 以訊息與原始例外建構 {@code LdapException}。
     *
     * @param msg 描述驗證失敗原因的訊息
     * @param t   導致此例外的原始例外
     */
    public LdapException(String msg, Throwable t) {
        super(msg, t);
    }

    /**
     * 以訊息建構 {@code LdapException}。
     *
     * @param msg 描述驗證失敗原因的訊息
     */
    public LdapException(String msg) {
        super(msg);
    }
}
