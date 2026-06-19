package com.zipe.service.impl;

import com.zipe.config.MailPropertyConfig;
import com.zipe.model.Mail;
import com.zipe.service.MailService;
import com.zipe.util.crypto.Base64Util;
import com.zipe.util.crypto.CryptoUtil;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * 郵件服務實作類別。
 * <p>
 * 基於 Jakarta Mail（JavaMail）與 Spring Mail 實作 {@link MailService} 介面，
 * 支援純文字信件、HTML 富文字信件、附件信件及群發多收件人等發送情境。
 * 郵件伺服器連線參數由 {@link MailPropertyConfig} 提供，並在每次發送前透過
 * {@link #setInitData()} 完成 SMTP 連線初始化。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/04/26 下午 14:00
 **/
@Slf4j
public class MailServiceImpl implements MailService {

    /** 郵件相關設定屬性，由外部注入 */
    private final MailPropertyConfig mailPropertyConfig;

    /** Spring JavaMail 發送器，於 {@link #setInitData()} 初始化後方可使用 */
    private JavaMailSenderImpl mailSender;

    /**
     * 建構子，注入郵件設定屬性。
     *
     * @param mailPropertyConfig 郵件伺服器連線設定
     */
    public MailServiceImpl(MailPropertyConfig mailPropertyConfig) {
        this.mailPropertyConfig = mailPropertyConfig;
    }

    /**
     * 初始化郵件發送資料，建立並測試 SMTP 連線。
     * <p>
     * 依照 {@link MailPropertyConfig} 設定建立 {@link JavaMailSenderImpl}，
     * 包含認證資訊、TLS 設定、逾時參數，最後呼叫 {@code testConnection()} 驗證連線是否正常。
     * 若設定中啟用加密，會先對密碼進行 Base64 解碼後再設定至發送器。
     * </p>
     *
     * @throws MessagingException 當 SMTP 連線測試失敗時拋出
     */
    @Override
    public void setInitData() throws MessagingException {
        // 建立郵件發送伺服器實例
        mailSender = new JavaMailSenderImpl();
        mailSender.setProtocol(mailPropertyConfig.getTransportProtocol());
        String mailPassword;
        Properties javaMailProperties = new Properties();

        // mail server 認證開啟
        if (mailPropertyConfig.getSmtpAuthEnable()) {
            javaMailProperties.put("mail.smtp.auth", mailPropertyConfig.getSmtpAuthEnable());
            mailSender.setUsername(mailPropertyConfig.getUsername());
            mailPassword = mailPropertyConfig.getPa55word();
            // 若設定檔將密碼加密開啟，需先解碼才能正確驗證
            if (mailPropertyConfig.getEncryptEnable() && StringUtils.isNotBlank(mailPassword)) {
                CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
                mailPassword = cryptoUtil.decode(mailPassword);
            }
            mailSender.setPassword(mailPassword);

        }

        // 加入 TLS 加密傳輸與 SSL Socket 認證機制
        javaMailProperties.put("mail.smtp.starttls.enable", mailPropertyConfig.getSmtpStartTlsEnable());
        javaMailProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        javaMailProperties.put("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        javaMailProperties.put("mail.debug", mailPropertyConfig.getDebugEnable());
        mailSender.setHost(mailPropertyConfig.getHost());
        mailSender.setPort(Integer.parseInt(mailPropertyConfig.getPort()));


        // 設定連線逾時、讀取逾時與寫入逾時，避免 SMTP 連線無限期阻塞
        javaMailProperties.put("mail.smtp.connectiontimeout", 5000);
        javaMailProperties.put("mail.smtp.timeout", 3000);
        javaMailProperties.put("mail.smtp.writetimeout", 5000);
        mailSender.setJavaMailProperties(javaMailProperties);
        mailSender.testConnection();
        log.info("初始化郵件服務完成");
    }

    /**
     * 發送 HTML 格式的 MIME 信件。
     * <p>
     * 使用 {@link MimeMessageHelper} 組裝收件人、主旨與 HTML 內容後發送。
     * 若發生 {@link MessagingException}，僅記錄錯誤日誌，不向外拋出例外。
     * </p>
     *
     * @param mail 包含寄件人、收件人、主旨及 HTML 內容的郵件資料物件
     */
    @Override
    public void sendEmail(Mail mail) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            mimeMessageHelper.setSubject(mail.getMailSubject());
            mimeMessageHelper.setFrom(mail.getMailFrom());
            mimeMessageHelper.setTo(mail.getMailTo());
            mimeMessageHelper.setCc(mail.getMailCc());
            mimeMessageHelper.setText(mail.getMailContent(), true);

            mailSender.send(mimeMessageHelper.getMimeMessage());
        } catch (MessagingException e) {
            log.error("Sent mail error:{}", e.getMessage());
        }
    }

    /**
     * 發送純文字格式的簡易信件。
     * <p>
     * 使用 {@link SimpleMailMessage} 組裝信件後發送，適用於不需要 HTML 或附件的簡單通知場景。
     * </p>
     *
     * @param mail 包含寄件人、收件人、主旨及純文字內容的郵件資料物件
     */
    @Override
    public void simpleMailSend(Mail mail) {
        // 建立純文字信件物件並設定欄位
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mail.getMailFrom());
        message.setTo(mail.getMailTo());
        message.setSubject(mail.getMailSubject());
        message.setText(mail.getMailContent());
        // 發送信件
        mailSender.send(message);
        log.info("發送成功");
    }

    /**
     * 發送含附件的信件，支援多個附件。
     * <p>
     * 使用 JavaMail 的 {@link MimeMessage}，支援更複雜的郵件格式與內容。
     * {@link MimeMessageHelper} 設定為多部分模式（multipart=true），以便加入附件檔案。
     * </p>
     *
     * @param mail 包含收件人、主旨、內容及附件清單的郵件資料物件
     * @throws MessagingException 當信件組裝或發送失敗時拋出
     */
    @Override
    public void attachedSend(Mail mail) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        // 建立 MimeMessageHelper 物件，作為處理 MimeMessage 的輔助類別
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        // 使用輔助類別 MimeMessageHelper 設定信件參數
        mimeMessageHelper.setFrom(mail.getMailFrom());
        mimeMessageHelper.setTo(mail.getMailTo());
        mimeMessageHelper.setCc(mail.getMailCc());
        mimeMessageHelper.setSubject(mail.getMailSubject());
        mimeMessageHelper.setText(mail.getMailContent());

        if (null != mail.getAttachments()) {
            for (File file : mail.getAttachments()) {
                mimeMessageHelper.addAttachment(file.getName(), file);
            }
        }
        // 發送信件
        mailSender.send(mimeMessage);
        log.info("發送成功");
    }

    /**
     * 發送 HTML 富文字信件，支援多張內嵌圖片與附件。
     * <p>
     * 使用 {@link MimeMessageHelper} 將郵件內容設定為 HTML 模式（{@code setText(..., true)}），
     * 可透過 {@code cid:} 參照在 HTML 中嵌入圖片資源，並同時支援附件。
     * 若副本收件人（CC）清單為空則略過設定，避免傳入空陣列導致例外。
     * </p>
     *
     * @param mail 包含收件人、副本、主旨、HTML 內容及附件清單的郵件資料物件
     * @throws MessagingException 當信件組裝或發送失敗時拋出
     */
    @Override
    public void richContentSend(Mail mail) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

        mimeMessageHelper.setFrom(mail.getMailFrom());
        mimeMessageHelper.setTo(mail.getMailTo());
        // 僅在 CC 清單非空時才設定，避免傳入空陣列引發 MessagingException
        if (!Objects.isNull(mail.getMailCc()) && mail.getMailCc().length > 0) {
            mimeMessageHelper.setCc(mail.getMailCc());
        }
        mimeMessageHelper.setSubject(mail.getMailSubject());
        // 第二個參數 true 表示 text 的內容為 HTML；注意 <img/> 標籤中 src='cid:file'，
        // 'cid' 是 contentId 的縮寫，'file' 是一個識別標記，
        // 需在後續程式碼中呼叫 MimeMessageHelper 的 addInline 方法將其替換為實際檔案
        mimeMessageHelper.setText(mail.getMailContent(), true);
        // 檔案路徑相對於 src 目錄（如有需要可取消下方範例的注解）
        // ClassPathResource file = new ClassPathResource("logo.png");

        if (null != mail.getAttachments()) {
            for (File file : mail.getAttachments()) {
                mimeMessageHelper.addAttachment(file.getName(), file);
            }
        }
        // 發送信件
        mailSender.send(mimeMessage);
        log.info("發送成功");
    }

    /**
     * 群發信件至多位收件人，並支援多個附件。
     * <p>
     * 當附件清單不為空時，手動建立 {@link MimeMultipart} 結構，
     * 以 {@link MimeBodyPart} 分別存放 HTML 內文與各附件，最終組裝為完整的 MIME 信件。
     * 若無附件，則直接透過 {@link MimeMessageHelper} 設定 HTML 內容。
     * 收件人採用 {@link InternetAddress} 陣列形式設定，以正確支援多位收件人。
     * </p>
     *
     * @param mail 包含多位收件人、主旨、HTML 內容及附件清單的郵件資料物件
     * @throws Exception 當信件組裝（含附件檔名編碼）或發送失敗時拋出
     */
    @Override
    public void sendBatchMailWithFile(Mail mail) throws Exception {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        mimeMessageHelper.setFrom(new InternetAddress(MimeUtility.encodeText(mail.getMailFrom())));
        mimeMessageHelper.setSubject(mail.getMailSubject());
        if (CollectionUtils.isNotEmpty(mail.getAttachments())) {
            // 建立一個存放信件內文的 BodyPart 物件
            BodyPart mdp = new MimeBodyPart();
            // 設定 BodyPart 的內容為 HTML 格式，並指定字元編碼為 UTF-8
            mdp.setContent(mail.getMailContent(), "text/html;charset=UTF-8");
            // 建立 MimeMultipart 物件，用來存放多個 BodyPart（內文與附件）
            Multipart mm = new MimeMultipart();
            // 將 HTML 內文的 BodyPart 加入 MimeMultipart（可加入多個 BodyPart）
            mm.addBodyPart(mdp);
            // 宣告附件相關區域變數，逐一加入附件
            MimeBodyPart filePart;
            FileDataSource filedatasource;
            // 逐個加入附件至 MimeMultipart
            for (int j = 0; j < mail.getAttachments().size(); j++) {
                filePart = new MimeBodyPart();
                filedatasource = new FileDataSource(mail.getAttachments().get(j));
                filePart.setDataHandler(new DataHandler(filedatasource));
                try {
                    // 對附件檔名進行 MIME 編碼，確保中文或特殊字元的檔名能正確傳輸
                    filePart.setFileName(MimeUtility.encodeText(filedatasource.getName()));
                } catch (Exception e) {
                    log.error("Add attachment error : {}", e.getMessage());
                }
                mm.addBodyPart(filePart);
            }
            mimeMessage.setContent(mm);
        } else {
            mimeMessageHelper.setText(mail.getMailContent(), true);
        }

        // 不可使用 String 陣列直接設定收件人，需轉為 InternetAddress 陣列才能正確支援多位收件人
        List<InternetAddress> list = new ArrayList<>();
        for (int i = 0; i < mail.getMailTo().length; i++) {
            list.add(new InternetAddress(mail.getMailTo()[i]));
        }
        InternetAddress[] address = list.toArray(new InternetAddress[list.size()]);

        mimeMessage.setRecipients(Message.RecipientType.TO, address);
        mimeMessage = mimeMessageHelper.getMimeMessage();

        // 發送信件
        mailSender.send(mimeMessage);
        log.info("發送成功");
    }

}
