package com.zipe.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * 使用者帳號尚未啟用的認證例外。
 *
 * <p>當使用者嘗試登入，但其帳號狀態為未啟用（例如尚未通過電子郵件驗證或被管理員停用）時，
 * 應拋出此例外，由 Spring Security 的認證流程捕捉並轉交給登入失敗處理器。</p>
 */
public class UserNotActivatedException extends AuthenticationException {

	/** 序列化版本識別碼，確保跨版本反序列化的相容性。 */
	private static final long serialVersionUID = 1L;

	/**
	 * 建立含有原因例外的 {@code UserNotActivatedException}。
	 *
	 * @param msg 描述帳號未啟用原因的錯誤訊息
	 * @param t   導致此例外發生的根本原因
	 */
	public UserNotActivatedException(String msg, Throwable t) {
		super(msg, t);
	}

	/**
	 * 建立僅含錯誤訊息的 {@code UserNotActivatedException}。
	 *
	 * @param msg 描述帳號未啟用原因的錯誤訊息
	 */
	public UserNotActivatedException(String msg) {
		super(msg);
	}
}
