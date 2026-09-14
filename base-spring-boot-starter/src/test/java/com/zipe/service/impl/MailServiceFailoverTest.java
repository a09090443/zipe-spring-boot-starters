package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.htmlMail;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.exception.MailFailoverException;
import com.zipe.model.Mail;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

/**
 * 多 SMTP 容錯切換核心行為情境測試，對應情境測試計畫 SC-02 ~ SC-09。
 *
 * <p>以 GreenMail 作為「可正常送達」的假 SMTP，搭配指向未監聽埠的「保證連線失敗」伺服器組，
 * 驗證 REQ-MAIL-FAILOVER-002（依序容錯切換）、REQ-003（全部失敗須明確回報）、
 * REQ-004（五個發送方法全覆蓋容錯）。</p>
 */
class MailServiceFailoverTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("backupUser", "backupPw"));

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    private MailPropertyConfig twoServerConfig() {
        return config(
                unreachableServer("primary"),
                greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
    }

    /** SC-02：第一組連線失敗時自動改用第二組並成功送出，呼叫端未見任何例外。 */
    @Test
    void simpleMailSend_firstServerDown_fallsBackToSecond() throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        assertThatCode(() -> service.simpleMailSend(plainTextMail("SC-02"))).doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-02");
    }

    /** SC-03：前兩組皆失敗時依序改用第三組；每次呼叫皆從清單第一組開始嘗試（優先序而非輪詢分流）。 */
    @Test
    void simpleMailSend_firstTwoServersDown_fallsBackToThird_andEveryCallRetriesFromFirst() throws Exception {
        MailPropertyConfig cfg = config(
                unreachableServer("primary", 1),
                unreachableServer("secondary", 2),
                greenMailServer("tertiary", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        service.simpleMailSend(plainTextMail("SC-03-first"));
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        greenMail.purgeEmailFromAllMailboxes();

        // 第二次呼叫：候選清單不因上次成功而改變順序，本次應再次歷經 primary/secondary 失敗後由 tertiary 送達
        service.simpleMailSend(plainTextMail("SC-03-second"));
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-03-second");
    }

    /** SC-04：所有已設定 SMTP 皆嘗試失敗時，不可靜默視為成功，須拋出含各組失敗原因摘要的例外。 */
    @Test
    void richContentSend_allServersDown_throwsAggregatedFailoverException() {
        MailPropertyConfig cfg = config(unreachableServer("primary", 1), unreachableServer("secondary", 2));
        MailServiceImpl service = new MailServiceImpl(cfg);

        // 兩組皆不可用：setInitData 依 REQ-005/SC-11 全部失敗仍拋例外，但候選清單保留，服務仍可再次嘗試發送
        assertThatThrownBy(service::setInitData).isInstanceOf(jakarta.mail.MessagingException.class);

        assertThatThrownBy(() -> service.richContentSend(htmlMail("SC-04")))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining("richContentSend")
                .hasMessageContaining("primary")
                .hasMessageContaining("secondary");

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    /** SC-04：simpleMailSend 的所有 SMTP 都失敗時，須彙整每一組識別與失敗原因。 */
    @Test
    void simpleMailSend_allServersDown_throwsAggregatedFailoverException() {
        MailServiceImpl service = allServersDownService();

        assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-04-simple")))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining("simpleMailSend")
                .hasMessageContaining("primary")
                .hasMessageContaining("secondary")
                .hasMessageContaining("MailSendException")
                .satisfies(error -> assertThat(error.getSuppressed()).hasSize(2));
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    /** REQ-003：attachedSend 的所有 SMTP 都失敗時，也必須回報完整的彙整例外。 */
    @Test
    void attachedSend_allServersDown_throwsAggregatedFailoverException(@TempDir Path tempDir) throws Exception {
        MailServiceImpl service = allServersDownService();
        Path attachment = tempDir.resolve("all-down-attachment.txt");
        Files.writeString(attachment, "all-down", StandardCharsets.UTF_8);
        Mail mail = plainTextMail("all-down-attached");
        mail.setAttachments(List.of(attachment.toFile()));

        assertThatThrownBy(() -> service.attachedSend(mail))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining("attachedSend")
                .hasMessageContaining("primary")
                .hasMessageContaining("secondary");
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    /** REQ-003：sendBatchMailWithFile 的所有 SMTP 都失敗時，也必須回報完整的彙整例外。 */
    @Test
    void sendBatchMailWithFile_allServersDown_throwsAggregatedFailoverException(@TempDir Path tempDir)
            throws Exception {
        MailServiceImpl service = allServersDownService();
        Path attachment = tempDir.resolve("all-down-batch.txt");
        Files.writeString(attachment, "all-down-batch", StandardCharsets.UTF_8);
        Mail mail = htmlMail("all-down-batch");
        mail.setMailTo(new String[] {"b1@test.local", "b2@test.local"});
        mail.setAttachments(List.of(attachment.toFile()));

        assertThatThrownBy(() -> service.sendBatchMailWithFile(mail))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining("sendBatchMailWithFile")
                .hasMessageContaining("primary")
                .hasMessageContaining("secondary");
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    /** SC-05：sendEmail 第一組失敗、第二組成功時仍不拋出例外（維持既有不外拋行為），備援組實際收信。 */
    @Test
    void sendEmail_firstServerDown_fallsBackToSecond_stillNoException() throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        assertThatCode(() -> service.sendEmail(htmlMail("SC-05-sendEmail"))).doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-05-sendEmail");
    }

    /** SC-06：simpleMailSend 第一組失敗、第二組成功時，備援組收到與原內容一致的純文字信。 */
    @Test
    void simpleMailSend_firstServerDown_deliversPlainTextContentToBackup() throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        Mail mail = plainTextMail("SC-06-simple");
        service.simpleMailSend(mail);

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-06-simple");
        assertThat(GreenMailUtil.getBody(messages[0])).contains("body-SC-06-simple");
    }

    /** SC-07：attachedSend 第一組失敗、第二組成功時，備援組收到之信件含正確附件。 */
    @Test
    void attachedSend_firstServerDown_deliversAttachmentToBackup(@TempDir Path tempDir) throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        Path attachment = tempDir.resolve("sc07-attachment.txt");
        Files.writeString(attachment, "hello-attachment", StandardCharsets.UTF_8);

        Mail mail = plainTextMail("SC-07-attached");
        mail.setAttachments(List.of(attachment.toFile()));

        assertThatCode(() -> service.attachedSend(mail)).doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-07-attached");
        assertAttachment(messages[0], "sc07-attachment.txt", "hello-attachment");
    }

    /** SC-08：richContentSend 第一組失敗、第二組成功時，備援組收到之 HTML 內容與附件皆正確。 */
    @Test
    void richContentSend_firstServerDown_deliversHtmlAndAttachmentToBackup(@TempDir Path tempDir) throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        Path attachment = tempDir.resolve("sc08-attachment.txt");
        Files.writeString(attachment, "sc08-content", StandardCharsets.UTF_8);

        Mail mail = htmlMail("SC-08-rich");
        mail.setAttachments(List.of(attachment.toFile()));

        assertThatCode(() -> service.richContentSend(mail)).doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-08-rich");
        assertThat(textContent(messages[0])).contains("<p>SC-08-rich</p>");
        assertAttachment(messages[0], "sc08-attachment.txt", "sc08-content");
    }

    /** SC-09：sendBatchMailWithFile 第一組失敗、第二組成功時，多位收件人皆收到含附件的信。 */
    @Test
    void sendBatchMailWithFile_firstServerDown_deliversToMultipleRecipients(@TempDir Path tempDir) throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        Path attachment = tempDir.resolve("sc09-attachment.txt");
        Files.writeString(attachment, "sc09-content", StandardCharsets.UTF_8);

        Mail mail = htmlMail("SC-09-batch");
        mail.setMailTo(new String[] {"b1@test.local", "b2@test.local"});
        mail.setAttachments(List.of(attachment.toFile()));

        assertThatCode(() -> service.sendBatchMailWithFile(mail)).doesNotThrowAnyException();

        // GreenMail 依「收件者信箱」各自儲存一份，2 位收件人各自的信箱皆應收到 1 封（合計 2 筆）
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(2);
        assertThat(messages[0].getSubject()).isEqualTo("SC-09-batch");
        assertThat(messages[1].getSubject()).isEqualTo("SC-09-batch");
        assertThat(messages[0].getAllRecipients()).hasSize(2);
        for (MimeMessage message : messages) {
            assertThat(textContent(message)).contains("<p>SC-09-batch</p>");
            assertAttachment(message, "sc09-attachment.txt", "sc09-content");
        }
    }

    private static MailServiceImpl allServersDownService() {
        MailPropertyConfig cfg = config(unreachableServer("primary", 1), unreachableServer("secondary", 2));
        MailServiceImpl service = new MailServiceImpl(cfg);
        assertThatThrownBy(service::setInitData).isInstanceOf(jakarta.mail.MessagingException.class);
        return service;
    }

    private static void assertAttachment(MimeMessage message, String expectedName, String expectedContent)
            throws Exception {
        Part attachment = findAttachment(message, expectedName);
        assertThat(attachment).as("附件 %s", expectedName).isNotNull();
        assertThat(new String(attachment.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo(expectedContent);
    }

    private static Part findAttachment(Part part, String expectedName) throws Exception {
        if (expectedName.equals(part.getFileName())) {
            return part;
        }
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                Part found = findAttachment(bodyPart, expectedName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String textContent(Part part) throws Exception {
        if (part.isMimeType("text/*")) {
            return String.valueOf(part.getContent());
        }
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(textContent(multipart.getBodyPart(i)));
            }
            return text.toString();
        }
        return "";
    }
}
