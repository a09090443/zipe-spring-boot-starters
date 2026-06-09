package com.zipe.service;

import com.zipe.model.Mail;

import jakarta.mail.MessagingException;

/**
 * 郵件服務介面，定義各種電子郵件發送行為。
 *
 * <p>提供純文字、附件、富文字（HTML/內嵌圖片）及批次附件等多種發送模式，
 * 各業務系統可透過注入此介面使用統一的郵件發送能力。
 * 實作類別需先呼叫 {@link #setInitData()} 完成郵件伺服器連線初始化。</p>
 */
public interface MailService {

	/**
	 * 初始化郵件伺服器連線設定。
	 *
	 * <p>實作類別應於此方法中讀取設定檔（主機、埠、帳號、密碼等），
	 * 並建立 JavaMail Session，以供後續各發送方法使用。</p>
	 *
	 * @throws MessagingException 若連線設定有誤或無法建立 Session 時拋出
	 */
	void setInitData() throws MessagingException;

	/**
	 * 通用郵件發送入口，由實作類別依 {@link com.zipe.model.Mail#getContentType()} 決定實際發送策略。
	 *
	 * @param mail 包含寄件者、收件者、主旨與內容等資訊的郵件資料物件
	 */
	void sendEmail(Mail mail);

	/**
	 * 發送純文字郵件（無附件、無 HTML 格式）。
	 *
	 * <p>適用於內容單純的系統通知或警示訊息。</p>
	 *
	 * @param mail 包含寄件者、收件者、主旨與純文字內容的郵件資料物件
	 */
	void simpleMailSend(Mail mail);

	/**
	 * 發送含附件的郵件。
	 *
	 * <p>透過 {@link com.zipe.model.Mail#getAttachments()} 取得附件清單，
	 * 以 MIME multipart/mixed 格式組裝並發送。</p>
	 *
	 * @param mail 包含附件清單的郵件資料物件
	 * @throws MessagingException 若附件讀取失敗或 MIME 訊息組裝異常時拋出
	 */
	void attachedSend(Mail mail) throws MessagingException;

	/**
	 * 發送富文字（HTML）郵件，支援內嵌圖片或 HTML 格式內容。
	 *
	 * <p>適用於需要排版美化的行銷信件或系統報表，
	 * 內容透過 {@link com.zipe.model.Mail#getMailContent()} 傳入 HTML 字串。</p>
	 *
	 * @param mail 包含 HTML 格式郵件內容的郵件資料物件
	 * @throws MessagingException 若 MIME 訊息組裝或發送失敗時拋出
	 */
	void richContentSend(Mail mail) throws MessagingException;

	/**
	 * 批次發送含附件的郵件。
	 *
	 * <p>適用於需要對多位收件者循環寄送相同附件的情境，
	 * 例如每月報表派送或大量通知信件。</p>
	 *
	 * @param mail 包含收件者清單與附件資訊的郵件資料物件
	 * @throws Exception 若附件讀取、訊息組裝或發送過程中發生任何例外時拋出
	 */
	void sendBatchMailWithFile(Mail mail) throws Exception;
}
