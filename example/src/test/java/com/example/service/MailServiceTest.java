package com.example.service;

import com.example.base.TestBase;
import com.zipe.model.Mail;
import com.zipe.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.MessagingException;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/26 下午 03:04
 **/
public class MailServiceTest extends TestBase {

    @Autowired
    public MailService mailService;

    @Test
    public void sendSimpleContextMailTest()
        throws MessagingException, jakarta.mail.MessagingException {
        Mail mail = new Mail();
        mail.setMailTo(new String[]{"gary_tsai@fglife.com.tw"});
        mail.setMailCc(new String[]{"gary_tsai@fglife.com.tw"});
        mail.setMailContent("測試內容");
        mail.setMailSubject("測試寄信");
        mailService.setInitData();
        mailService.simpleMailSend(mail);
    }
}
