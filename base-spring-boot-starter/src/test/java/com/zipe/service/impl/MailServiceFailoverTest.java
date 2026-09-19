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

    /**
     * SC-004：mail.servers 非空時，扁平 mail.host 即使指向實際可連線的伺服器，也絕不能被當成候選
     * 之一嘗試——扁平端點的實際連線數必須為 0，且發送仍依 servers 清單失敗結果回報。
     */
    @Test
    void flatHostReachable_butServersConfigured_flatEndpointNeverContacted() throws Exception {
        try (MailFailoverTestSupport.CountingTcpServer flatEndpoint = new MailFailoverTestSupport.CountingTcpServer()) {
            MailPropertyConfig cfg = config(unreachableServer("primary", 1));
            cfg.setHost("127.0.0.1");
            cfg.setPort(String.valueOf(flatEndpoint.port()));
            cfg.setUsername("shouldNotBeUsed");
            cfg.setPa55word("shouldNotBeUsed");

            MailServiceImpl service = new MailServiceImpl(cfg);
            try {
                service.setInitData();
            } catch (jakarta.mail.MessagingException ignored) {
                // servers 清單中唯一一組不可用，仍保留候選清單供後續嘗試。
            }

            assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-004")))
                    .isInstanceOf(MailFailoverException.class)
                    .hasMessageContaining("primary");

            flatEndpoint.waitBriefly(200);
            assertThat(flatEndpoint.connectionCount()).isZero();
        }
    }

    /**
     * SC-005：三組皆可用時只嘗試第一組，第二、第三組完全不被連線；成功後不重複投遞給後續組別。
     */
    @Test
    void allServersAvailable_onlyFirstIsContacted_othersRemainUntouched() throws Exception {
        try (MailFailoverTestSupport.CountingTcpServer secondary = new MailFailoverTestSupport.CountingTcpServer();
                MailFailoverTestSupport.CountingTcpServer tertiary = new MailFailoverTestSupport.CountingTcpServer()) {
            MailPropertyConfig cfg = config(
                    greenMailServer("primary", greenMail.getSmtp().getPort(), "backupUser", "backupPw"),
                    MailFailoverTestSupport.server("secondary", "127.0.0.1", secondary.port(), "u", "p"),
                    MailFailoverTestSupport.server("tertiary", "127.0.0.1", tertiary.port(), "u", "p"));
            MailServiceImpl service = new MailServiceImpl(cfg);
            service.setInitData();
            // setInitData() 依 REQ-007 會逐一測試每組連線（含 secondary／tertiary），
            // 故只計數「實際發送」這一階段是否連線，避免把初始化階段的連線測試
            // 誤判為發送階段不應發生的連線。
            secondary.resetCount();
            tertiary.resetCount();

            service.simpleMailSend(plainTextMail("SC-005"));

            MimeMessage[] messages = greenMail.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo("SC-005");

            secondary.waitBriefly(200);
            assertThat(secondary.connectionCount()).isZero();
            assertThat(tertiary.connectionCount()).isZero();
        }
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

    /**
     * SC-015：sendEmail 經備援組送達時，不只主旨，To、Cc 收件人與 HTML 內容皆須與原始 Mail 資料一致，
     * 而非僅有信件數量與主旨層級的弱驗證。
     */
    @Test
    void sendEmail_firstServerDown_fallsBackToSecond_deliversToAndCcAndHtmlContentIntact() throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        Mail mail = htmlMail("SC-015-sendEmail");
        mail.setMailTo(new String[] {"to-receiver@test.local"});
        mail.setMailCc(new String[] {"cc-receiver@test.local"});

        assertThatCode(() -> service.sendEmail(mail)).doesNotThrowAnyException();

        // GreenMail 依收件者信箱各自儲存一份：To 與 Cc 各一封，皆帶有完整的 To／Cc 標頭與 HTML 內容。
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(2);
        for (MimeMessage message : messages) {
            assertThat(message.getSubject()).isEqualTo("SC-015-sendEmail");
            assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.TO))
                    .extracting(Object::toString)
                    .containsExactly("to-receiver@test.local");
            assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.CC))
                    .extracting(Object::toString)
                    .containsExactly("cc-receiver@test.local");
            assertThat(textContent(message)).contains("<p>SC-015-sendEmail</p>");
        }
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
        assertThat(messages[0].getFrom()).extracting(Object::toString).containsExactly(mail.getMailFrom());
        assertThat(messages[0].getAllRecipients())
                .extracting(Object::toString)
                .containsExactly(mail.getMailTo());
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

    /**
     * SC-017：attachedSend 經備援組送達時，Unicode 檔名（非純 ASCII）與非文字二進位內容
     * 皆須逐位元與來源一致，而非僅驗證純文字檔名與純文字內容的弱案例。
     */
    @Test
    void attachedSend_firstServerDown_deliversUnicodeNamedBinaryAttachmentToBackup_byteForByte(@TempDir Path tempDir)
            throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        String unicodeFileName = "附件-日本語-😀.bin";
        Path attachment = tempDir.resolve("sc17-binary-source.bin");
        byte[] binaryContent = new byte[4096];
        new java.util.Random(2024).nextBytes(binaryContent);
        Files.write(attachment, binaryContent);

        Mail mail = plainTextMail("SC-17-attached-unicode");
        java.io.File renamedCopy = tempDir.resolve(unicodeFileName).toFile();
        Files.copy(attachment, renamedCopy.toPath());
        mail.setAttachments(List.of(renamedCopy));

        assertThatCode(() -> service.attachedSend(mail)).doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        Part attachmentPart = findAttachment(messages[0], unicodeFileName);
        assertThat(attachmentPart).as("Unicode 檔名附件 %s", unicodeFileName).isNotNull();
        byte[] receivedBytes = attachmentPart.getInputStream().readAllBytes();
        assertThat(receivedBytes).isEqualTo(binaryContent);
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

    /**
     * SC-018（部分）：richContentSend 經備援組送達時，HTML 內容中 {@code cid:} 參照文字與隨附的
     * 二進位資源皆須位元正確送達。
     *
     * <p><b>已知缺口（非本次容錯切換範圍）</b>：{@link MailServiceImpl#richContentSend} 目前對
     * {@code attachments} 一律呼叫 {@code MimeMessageHelper.addAttachment}，並未呼叫
     * {@code addInline} 設定 Content-ID，因此 HTML 中的 {@code cid:} 參照實際上無法被信件用戶端
     * 解析為內嵌圖片（僅會顯示為一般附件）。這是 {@code richContentSend} 既有（容錯切換之前即存在）
     * 的行為缺口，而非本次多 SMTP 容錯切換引入的問題；{@code Mail} 模型也未區分「內嵌資源」與
     * 「一般附件」，要修正需先決定兩者的 API 區分方式，屬需另行核准的產品設計取捨，
     * 故本測試僅驗證現況下可驗證的部分（HTML 文字與附件位元正確性），不驗證真正的 Content-ID 解析。</p>
     */
    @Test
    void richContentSend_firstServerDown_deliversHtmlCidReferenceAndBinaryResourceToBackup_byteForByte(
            @TempDir Path tempDir) throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        byte[] inlineImageBytes = new byte[512];
        new java.util.Random(99).nextBytes(inlineImageBytes);
        Path inlineImage = tempDir.resolve("inline-logo.png");
        Files.write(inlineImage, inlineImageBytes);

        Mail mail = plainTextMail("SC-018-inline");
        mail.setContentType("text/html");
        mail.setMailContent("<p>SC-018-inline</p><img src='cid:inline-logo.png'/>");
        mail.setAttachments(List.of(inlineImage.toFile()));

        assertThatCode(() -> service.richContentSend(mail)).doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(textContent(messages[0])).contains("cid:inline-logo.png");
        Part attachmentPart = findAttachment(messages[0], "inline-logo.png");
        assertThat(attachmentPart).as("內嵌資源檔案 inline-logo.png").isNotNull();
        assertThat(attachmentPart.getInputStream().readAllBytes()).isEqualTo(inlineImageBytes);
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
