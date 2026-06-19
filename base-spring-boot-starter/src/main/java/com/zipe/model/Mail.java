package com.zipe.model;

import lombok.Data;

import java.io.File;
import java.util.List;

/**
 * 郵件資料模型，封裝寄送一封電子郵件所需的所有屬性，
 * 包含寄件者、收件者、副本、密件副本、主旨、內文、內容類型與附件清單。
 * <p>
 * 供 {@code MailService} 作為參數傳遞，統一收集郵件發送所需資訊。
 * </p>
 */
@Data
public class Mail {

	// 寄件者電子郵件地址
	private String mailFrom;

	// 收信者電子郵件地址陣列（TO）
	private String[] mailTo;

	// 副本收件者電子郵件地址陣列（CC）
	private String[] mailCc;

	// 密件副本收件者電子郵件地址陣列（BCC），收件者不會看到此名單
	private String[] mailBcc;

	// 郵件主旨
	private String mailSubject;

	// 郵件內文
	private String mailContent;

	// 郵件內容的 MIME 類型，例如 "text/plain" 或 "text/html"
	private String contentType;

	// 附件檔案清單
	private List<File> attachments;

	/**
	 * 建立一個預設的 Mail 實例。
	 * <p>
	 * 預設將 {@code contentType} 初始化為 {@code "text/plain"}，
	 * 避免未設定時發送純文字郵件發生格式錯誤。
	 * </p>
	 */
	public Mail() {
		// 預設以純文字格式傳送，若需 HTML 格式請在建立後呼叫 setContentType("text/html")
		contentType = "text/plain";
	}

}
