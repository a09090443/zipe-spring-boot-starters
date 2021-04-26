package com.zipe.service.impl;

import com.zipe.config.MailPropertyConfig;
import com.zipe.model.Mail;
import com.zipe.service.MailService;
import com.zipe.util.crypto.Base64Util;
import com.zipe.util.crypto.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/04/26 下午 14:00
 **/
@Slf4j
public class MailServiceImpl implements MailService {

    private final MailPropertyConfig mailPropertyConfig;
    private JavaMailSenderImpl mailSender;

    public MailServiceImpl(MailPropertyConfig mailPropertyConfig) {
        this.mailPropertyConfig = mailPropertyConfig;
    }

    /**
     * 初始化郵件發送數據
     */
    @Override
    public void setInitData() throws MessagingException {
        //創建郵件發送服務器
        mailSender = new JavaMailSenderImpl();
        mailSender.setProtocol(mailPropertyConfig.getTransportProtocol());
        String mailPassword;
        Properties javaMailProperties = new Properties();

        // mail server 認證開啟
        if (mailPropertyConfig.getSmtpAuthEnable()) {
            javaMailProperties.put("mail.smtp.auth", mailPropertyConfig.getSmtpAuthEnable());
            mailSender.setUsername(mailPropertyConfig.getUsername());
            mailPassword = mailPropertyConfig.getPa55word();
            // 如設定檔將編碼加密開啟，需解碼
            if (mailPropertyConfig.getEncryptEnable() && StringUtils.isNotBlank(mailPassword)) {
                CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
                mailPassword = cryptoUtil.decode(mailPassword);
            }
            mailSender.setPassword(mailPassword);

        }

        // 加入認證機制
        javaMailProperties.put("mail.smtp.starttls.enable", mailPropertyConfig.getSmtpStartTlsEnable());
        javaMailProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        javaMailProperties.put("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        javaMailProperties.put("mail.debug", mailPropertyConfig.getDebugEnable());
        mailSender.setHost(mailPropertyConfig.getHost());
        mailSender.setPort(Integer.parseInt(mailPropertyConfig.getPort()));


        // Timeout 機制
        javaMailProperties.put("mail.smtp.connectiontimeout", 5000);
        javaMailProperties.put("mail.smtp.timeout", 3000);
        javaMailProperties.put("mail.smtp.writetimeout", 5000);
        mailSender.setJavaMailProperties(javaMailProperties);
        mailSender.testConnection();
        log.info("初始化郵件服務完成");
    }

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
     * 發送一般信件
     */
    @Override
    public void simpleMailSend(Mail mail) {
        // 創件信件內容
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
     * 發送附件,支持多附件 //使用JavaMail的MimeMessage，支付更加複雜的郵件格式和內容
     * MimeMessages為複雜郵件模板，支持文本、附件、html、圖片等。
     */
    @Override
    public void attachedSend(Mail mail) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        // 創建MimeMessageHelper對象，處理MimeMessage的輔助類
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        // 使用辅助类MimeMessage设定参数
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
     * 發送html文件，支持多圖片
     */
    @Override
    public void richContentSend(Mail mail) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

        mimeMessageHelper.setFrom(mail.getMailFrom());
        mimeMessageHelper.setTo(mail.getMailTo());
        if (!Objects.isNull(mail.getMailCc()) && mail.getMailCc().length > 0) {
            mimeMessageHelper.setCc(mail.getMailCc());
        }
        mimeMessageHelper.setSubject(mail.getMailSubject());
        // 第二個參數true，表示text的內容為html，然後注意<img/>標籤，src='cid:file'，'cid'是contentId的縮寫，'file'是一個標記，
        // 需要在後面的代碼中調用MimeMessageHelper的addInline方法替代成文件
        mimeMessageHelper.setText(mail.getMailContent(), true);
        // 文件地址相對應src目錄
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
     * 群發多人，且多附件
     */
    @Override
    public void sendBatchMailWithFile(Mail mail) throws Exception {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        mimeMessageHelper.setFrom(new InternetAddress(MimeUtility.encodeText(mail.getMailFrom())));
        mimeMessageHelper.setSubject(mail.getMailSubject());
        if (CollectionUtils.isNotEmpty(mail.getAttachments())) {
            // 新建一個存放信件內容的BodyPart對象
            BodyPart mdp = new MimeBodyPart();
            // 給BodyPart對象設置內容和格式/編碼方式
            mdp.setContent(mail.getMailContent(), "text/html;charset=UTF-8");
            // 新建一個MimeMultipart對像用來存放BodyPart對象
            Multipart mm = new MimeMultipart();
            // 將BodyPart加入到MimeMultipart對像中(可以加入多個BodyPart)
            mm.addBodyPart(mdp);
            // 把mm作為消息對象的內容
            MimeBodyPart filePart;
            FileDataSource filedatasource;
            // 逐個加入附件
            for (int j = 0; j < mail.getAttachments().size(); j++) {
                filePart = new MimeBodyPart();
                filedatasource = new FileDataSource(mail.getAttachments().get(j));
                filePart.setDataHandler(new DataHandler(filedatasource));
                try {
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

        // 不能使用string類型的類型，這樣只能發送一個收件人
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
