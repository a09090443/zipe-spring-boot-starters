package com.zipe.model;

import lombok.Data;

import java.io.File;
import java.util.List;

@Data
public class Mail {

	// 寄件者
	private String mailFrom;

	// 收信者
	private String[] mailTo;

	// 副本
	private String[] mailCc;

	// 特定者
	private String[] mailBcc;

	// 主旨
	private String mailSubject;

	// 內容
	private String mailContent;

	// http 格式
	private String contentType;

	// 附件
	private List<File> attachments;

	public Mail() {
		contentType = "text/plain";
	}

}
