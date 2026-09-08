package com.zipe.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.model.Mail;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * 多 SMTP 容錯切換情境測試的共用建構工具。
 *
 * <p>集中提供「不可用伺服器組」「以 GreenMail 埠號組成可用伺服器組」與
 * {@link MailPropertyConfig} 組裝方法，避免各情境測試檔重複樣板程式碼。</p>
 */
final class MailFailoverTestSupport {

    private MailFailoverTestSupport() {}

    /** 保證連線被拒（無服務監聽）的伺服器組，用於模擬連線失敗。 */
    static MailServerProperty unreachableServer(String name) {
        return unreachableServer(name, 1);
    }

    /** 保證連線被拒（無服務監聽）的伺服器組，可指定埠號以區分多組不可用伺服器。 */
    static MailServerProperty unreachableServer(String name, int port) {
        return server(name, "127.0.0.1", port, "user", "pass");
    }

    /** 指向 GreenMail 假 SMTP 伺服器的可用伺服器組。 */
    static MailServerProperty greenMailServer(String name, int port, String username, String password) {
        return server(name, "127.0.0.1", port, username, password);
    }

    static MailServerProperty server(String name, String host, int port, String username, String password) {
        MailServerProperty server = new MailServerProperty();
        server.setName(name);
        server.setHost(host);
        server.setPort(String.valueOf(port));
        server.setUsername(username);
        server.setPa55word(password);
        server.setSmtpAuthEnable(true);
        server.setSmtpStartTlsEnable(false);
        server.setTransportProtocol("smtp");
        return server;
    }

    static MailPropertyConfig config(MailServerProperty... servers) {
        MailPropertyConfig config = new MailPropertyConfig();
        config.getServers().addAll(List.of(servers));
        // 測試用短逾時，避免不可達伺服器造成測試長時間阻塞
        config.setConnectionTimeout(1000);
        config.setReadTimeout(1000);
        config.setWriteTimeout(1000);
        return config;
    }

    static Mail plainTextMail(String subject) {
        Mail mail = new Mail();
        mail.setMailFrom("sender@test.local");
        mail.setMailTo(new String[] {"receiver@test.local"});
        // sendEmail()/attachedSend() 呼叫 MimeMessageHelper.setCc(String[])，未設定時 mailCc 為 null 會拋
        // IllegalArgumentException（既有行為，非本次容錯切換異動範圍），故測試資料一律提供空陣列。
        mail.setMailCc(new String[0]);
        mail.setMailSubject(subject);
        mail.setMailContent("body-" + subject);
        mail.setContentType("text/plain");
        return mail;
    }

    static Mail htmlMail(String subject) {
        Mail mail = plainTextMail(subject);
        mail.setContentType("text/html");
        mail.setMailContent("<p>" + subject + "</p>");
        return mail;
    }

    /** 附掛 Logback ListAppender 至指定類別的 logger，供測試斷言日誌內容。 */
    static ListAppender<ILoggingEvent> attachLogAppender(Class<?> targetClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(targetClass);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
