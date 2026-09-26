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
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import com.zipe.model.Mail;
import com.zipe.service.impl.MailFailoverTestSupport.SwitchableSmtpEndpoint;
import com.zipe.util.crypto.Base64Util;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mail.javamail.JavaMailSenderImpl;

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

    @RegisterExtension
    static GreenMailExtension secondaryGreenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("secondaryUser", "secondaryPw"));

    @RegisterExtension
    static GreenMailExtension tertiaryGreenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("tertiaryUser", "tertiaryPw"));

    /** 三組全敗情境的整體時間上限，須足以涵蓋前兩組的快速失敗，並使未具名第三組因整體逾時截止。 */
    private static final long ALL_DOWN_OVERALL_TIMEOUT_MS = 2500L;

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
        secondaryGreenMail.purgeEmailFromAllMailboxes();
        tertiaryGreenMail.purgeEmailFromAllMailboxes();
    }

    private MailPropertyConfig twoServerConfig() {
        return config(
                unreachableServer("primary"),
                greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
    }

    /**
     * SC-001：同一候選清單混用明文與 Base64 密碼時，實際建立出的 sender 必須逐組保留或解碼，
     * 不能只驗證 encrypt-enable 設定旗標。
     */
    @Test
    void setInitData_mixedPasswordEncoding_buildsEachSenderWithItsEffectivePassword() throws Exception {
        String plainPassword = "plain-password-for-primary";
        String decodedPassword = "decoded-password-for-secondary";
        String encodedPassword = new Base64Util().getEncrypt(decodedPassword);
        MailServerProperty primary = unreachableServer("primary", 1);
        primary.setPa55word(plainPassword);
        MailServerProperty secondary = unreachableServer("secondary", 2);
        secondary.setPa55word(encodedPassword);
        secondary.setEncryptEnable(true);
        MailServiceImpl service = new MailServiceImpl(config(primary, secondary));

        assertThatThrownBy(service::setInitData).isInstanceOf(jakarta.mail.MessagingException.class);

        assertThat(candidateSenders(service))
                .extracting(JavaMailSenderImpl::getPassword)
                .containsExactly(plainPassword, decodedPassword);
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
        try (MailFailoverTestSupport.CountingTcpServer primary = new MailFailoverTestSupport.CountingTcpServer();
                MailFailoverTestSupport.CountingTcpServer secondary =
                        new MailFailoverTestSupport.CountingTcpServer()) {
            MailPropertyConfig cfg = config(
                    MailFailoverTestSupport.server("primary", "127.0.0.1", primary.port(), "user", "pass"),
                    MailFailoverTestSupport.server("secondary", "127.0.0.1", secondary.port(), "user", "pass"),
                    greenMailServer("tertiary", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
            MailServiceImpl service = new MailServiceImpl(cfg);
            service.setInitData();
            primary.resetCount();
            secondary.resetCount();

            service.simpleMailSend(plainTextMail("SC-03-first"));
            assertThat(greenMail.getReceivedMessages()).hasSize(1);
            int primaryAfterFirstCall = primary.connectionCount();
            int secondaryAfterFirstCall = secondary.connectionCount();
            assertThat(primaryAfterFirstCall).as("第一次呼叫必須先嘗試 primary").isPositive();
            assertThat(secondaryAfterFirstCall).as("第一次呼叫必須再嘗試 secondary").isPositive();
            greenMail.purgeEmailFromAllMailboxes();

            // 第二次呼叫仍須重新從 primary 開始。若實作黏著上次成功的 tertiary，兩個計數都不會增加。
            service.simpleMailSend(plainTextMail("SC-03-second"));
            MimeMessage[] messages = greenMail.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo("SC-03-second");
            assertThat(primary.connectionCount())
                    .as("第二次呼叫不得略過 primary 而黏著 tertiary")
                    .isGreaterThan(primaryAfterFirstCall);
            assertThat(secondary.connectionCount())
                    .as("第二次呼叫仍須依序嘗試 secondary")
                    .isGreaterThan(secondaryAfterFirstCall);
        }
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
        try (MailFailoverTestSupport.CountingSmtpProxy secondary = new MailFailoverTestSupport.CountingSmtpProxy(
                        "127.0.0.1", secondaryGreenMail.getSmtp().getPort());
                MailFailoverTestSupport.CountingSmtpProxy tertiary = new MailFailoverTestSupport.CountingSmtpProxy(
                        "127.0.0.1", tertiaryGreenMail.getSmtp().getPort())) {
            MailPropertyConfig cfg = config(
                    greenMailServer("primary", greenMail.getSmtp().getPort(), "backupUser", "backupPw"),
                    greenMailServer("secondary", secondary.port(), "secondaryUser", "secondaryPw"),
                    greenMailServer("tertiary", tertiary.port(), "tertiaryUser", "tertiaryPw"));
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
            assertThat(secondaryGreenMail.getReceivedMessages()).isEmpty();
            assertThat(tertiaryGreenMail.getReceivedMessages()).isEmpty();
        }
    }

    /**
     * SC-021／SC-023：richContentSend 於兩個具名、一個未具名共三組候選全敗時，須拋出完整彙整例外；
     * 未具名候選恢復後，不重新 setInitData 直接再次呼叫即可投遞，且 HTML 與附件完整。
     */
    @Test
    void richContentSend_threeServersDown_throwsAggregatedFailoverException_thenRecovers(@TempDir Path tempDir)
            throws Exception {
        Path attachment = tempDir.resolve("all-down-rich.txt");
        Files.writeString(attachment, "all-down-rich", StandardCharsets.UTF_8);
        Mail mail = htmlMail("SC-021-rich");
        mail.setAttachments(List.of(attachment.toFile()));

        try (SwitchableSmtpEndpoint unnamed = unnamedEndpoint()) {
            MailServiceImpl service = allServersDownService(unnamed);

            assertThatThrownBy(() -> service.richContentSend(mail))
                    .isInstanceOf(MailFailoverException.class)
                    .satisfies(error -> assertThreeCandidateFailure(error, "richContentSend", unnamed.port()));
            assertNothingDelivered();

            unnamed.switchTo(SwitchableSmtpEndpoint.Mode.FORWARD);
            assertThatCode(() -> service.richContentSend(mail)).doesNotThrowAnyException();

            MimeMessage message = singleRecoveredMessage("SC-021-rich");
            assertThat(textContent(message)).contains("<p>SC-021-rich</p>");
            assertAttachment(message, "all-down-rich.txt", "all-down-rich");
        }
    }

    /**
     * SC-021／SC-023：simpleMailSend 於三組候選（含未具名）全敗時，須彙整每一組識別與失敗原因；
     * 未具名候選恢復後可再次投遞完整純文字信件。
     */
    @Test
    void simpleMailSend_threeServersDown_throwsAggregatedFailoverException_thenRecovers() throws Exception {
        Mail mail = plainTextMail("SC-021-simple");

        try (SwitchableSmtpEndpoint unnamed = unnamedEndpoint()) {
            MailServiceImpl service = allServersDownService(unnamed);

            assertThatThrownBy(() -> service.simpleMailSend(mail))
                    .isInstanceOf(MailFailoverException.class)
                    .satisfies(error -> assertThreeCandidateFailure(error, "simpleMailSend", unnamed.port()));
            assertNothingDelivered();

            unnamed.switchTo(SwitchableSmtpEndpoint.Mode.FORWARD);
            assertThatCode(() -> service.simpleMailSend(mail)).doesNotThrowAnyException();

            MimeMessage message = singleRecoveredMessage("SC-021-simple");
            assertThat(GreenMailUtil.getBody(message)).isEqualTo("body-SC-021-simple");
            assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly(mail.getMailTo());
        }
    }

    /**
     * SC-021／SC-023：attachedSend 於三組候選（含未具名）全敗時，也必須回報完整的彙整例外；
     * 未具名候選恢復後可再次投遞，附件內容逐位元一致。
     */
    @Test
    void attachedSend_threeServersDown_throwsAggregatedFailoverException_thenRecovers(@TempDir Path tempDir)
            throws Exception {
        Path attachment = tempDir.resolve("all-down-attachment.txt");
        Files.writeString(attachment, "all-down", StandardCharsets.UTF_8);
        Mail mail = plainTextMail("SC-021-attached");
        mail.setAttachments(List.of(attachment.toFile()));

        try (SwitchableSmtpEndpoint unnamed = unnamedEndpoint()) {
            MailServiceImpl service = allServersDownService(unnamed);

            assertThatThrownBy(() -> service.attachedSend(mail))
                    .isInstanceOf(MailFailoverException.class)
                    .satisfies(error -> assertThreeCandidateFailure(error, "attachedSend", unnamed.port()));
            assertNothingDelivered();

            unnamed.switchTo(SwitchableSmtpEndpoint.Mode.FORWARD);
            assertThatCode(() -> service.attachedSend(mail)).doesNotThrowAnyException();

            MimeMessage message = singleRecoveredMessage("SC-021-attached");
            assertAttachmentBytes(message, "all-down-attachment.txt", "all-down".getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * SC-021／SC-023：sendBatchMailWithFile 對三位收件人、於三組候選（含未具名）全敗時，也必須回報完整的
     * 彙整例外；未具名候選恢復後三個信箱各恰好收到一封。
     */
    @Test
    void sendBatchMailWithFile_threeServersDown_throwsAggregatedFailoverException_thenRecovers(
            @TempDir Path tempDir) throws Exception {
        Path attachment = tempDir.resolve("all-down-batch.txt");
        Files.writeString(attachment, "all-down-batch", StandardCharsets.UTF_8);
        Mail mail = htmlMail("SC-021-batch");
        List<String> recipients = List.of("b1@test.local", "b2@test.local", "b3@test.local");
        mail.setMailTo(recipients.toArray(String[]::new));
        mail.setAttachments(List.of(attachment.toFile()));

        try (SwitchableSmtpEndpoint unnamed = unnamedEndpoint()) {
            MailServiceImpl service = allServersDownService(unnamed);

            assertThatThrownBy(() -> service.sendBatchMailWithFile(mail))
                    .isInstanceOf(MailFailoverException.class)
                    .satisfies(error -> assertThreeCandidateFailure(error, "sendBatchMailWithFile", unnamed.port()));
            assertNothingDelivered();

            unnamed.switchTo(SwitchableSmtpEndpoint.Mode.FORWARD);
            assertThatCode(() -> service.sendBatchMailWithFile(mail)).doesNotThrowAnyException();

            assertThat(tertiaryGreenMail.getReceivedMessages()).hasSize(3);
            for (String recipient : recipients) {
                List<MimeMessage> recipientMessages = tertiaryGreenMail
                        .findReceivedMessages(user -> recipient.equals(user.getEmail()), message -> true)
                        .toList();
                assertThat(recipientMessages).as("信箱 %s 必須恰好收到一封", recipient).hasSize(1);
                MimeMessage message = recipientMessages.get(0);
                assertThat(message.getSubject()).isEqualTo("SC-021-batch");
                assertThat(String.valueOf(findFirstMimeType(message, "text/html").getContent()))
                        .isEqualTo("<p>SC-021-batch</p>");
                assertAttachmentBytes(
                        message, "all-down-batch.txt", "all-down-batch".getBytes(StandardCharsets.UTF_8));
            }
        }
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
            Part htmlPart = findFirstMimeType(message, "text/html");
            assertThat(htmlPart).as("sendEmail 備援後必須保留 text/html MIME 類型").isNotNull();
            assertThat(String.valueOf(htmlPart.getContent())).isEqualTo("<p>SC-015-sendEmail</p>");
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
     * SC-018：richContentSend 經備援組送達時，HTML 內容中 {@code cid:} 參照的內嵌資源須以真正的
     * MIME Content-ID（{@code addInline}）送達，而非退化為一般附件（{@code addAttachment}）；
     * 一般附件（{@code attachments}）與內嵌資源（{@code inlineResources}）須能同時共存且互不混淆，
     * 內嵌資源的二進位內容須逐位元正確。
     */
    @Test
    void richContentSend_firstServerDown_deliversHtmlCidReferenceAsRealInlineResource_byteForByte(
            @TempDir Path tempDir) throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        byte[] inlineImageBytes = new byte[512];
        new java.util.Random(99).nextBytes(inlineImageBytes);
        Path inlineImage = tempDir.resolve("inline-logo.png");
        Files.write(inlineImage, inlineImageBytes);

        Path plainAttachment = tempDir.resolve("report.txt");
        Files.writeString(plainAttachment, "SC-018-plain-attachment", StandardCharsets.UTF_8);

        Mail mail = plainTextMail("SC-018-inline");
        mail.setContentType("text/html");
        mail.setMailContent("<p>SC-018-inline</p><img src='cid:inline-logo'/>");
        mail.setInlineResources(java.util.Map.of("inline-logo", inlineImage.toFile()));
        mail.setAttachments(List.of(plainAttachment.toFile()));

        assertThatCode(() -> service.richContentSend(mail)).doesNotThrowAnyException();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].isMimeType("multipart/mixed"))
                .as("含一般附件的富文字郵件外層須為 multipart/mixed")
                .isTrue();
        Multipart mixed = (Multipart) messages[0].getContent();
        assertThat(mixed.getCount()).as("外層須包含 related 內文與一般附件").isEqualTo(2);

        BodyPart relatedContainer = mixed.getBodyPart(0);
        assertThat(relatedContainer.isMimeType("multipart/related"))
                .as("HTML 與 inline 資源須位於 multipart/related 層")
                .isTrue();
        Multipart related = (Multipart) relatedContainer.getContent();
        assertThat(related.getCount()).as("related 層須包含 HTML 與一個 inline 資源").isEqualTo(2);
        BodyPart htmlPart = related.getBodyPart(0);
        assertThat(htmlPart.isMimeType("text/html")).as("富文字本文 MIME 類型須為 text/html").isTrue();
        assertThat(String.valueOf(htmlPart.getContent()))
                .isEqualTo("<p>SC-018-inline</p><img src='cid:inline-logo'/>");

        BodyPart inlinePart = related.getBodyPart(1);
        assertThat(inlinePart.getHeader("Content-ID")).containsExactly("<inline-logo>");
        assertThat(inlinePart).as("內嵌資源須以 Content-ID inline-logo 存在，而非一般附件").isNotNull();
        assertThat(inlinePart.getDisposition()).isEqualToIgnoringCase(Part.INLINE);
        assertThat(inlinePart.getInputStream().readAllBytes()).isEqualTo(inlineImageBytes);

        // 一般附件仍須維持既有 addAttachment 行為，與內嵌資源互不混淆
        BodyPart plainAttachmentPart = mixed.getBodyPart(1);
        assertThat(plainAttachmentPart.getFileName()).isEqualTo("report.txt");
        assertThat(plainAttachmentPart.getDisposition()).isEqualToIgnoringCase(Part.ATTACHMENT);
        assertThat(new String(plainAttachmentPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("SC-018-plain-attachment");
    }

    private static Part findFirstMimeType(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return part;
        }
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                Part found = findFirstMimeType(multipart.getBodyPart(i), mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** SC-09：sendBatchMailWithFile 第一組失敗、第二組成功時，多位收件人皆收到含附件的信。 */
    @Test
    void sendBatchMailWithFile_firstServerDown_deliversToMultipleRecipients(@TempDir Path tempDir) throws Exception {
        MailServiceImpl service = new MailServiceImpl(twoServerConfig());
        service.setInitData();

        Path attachment = tempDir.resolve("sc09-attachment.txt");
        Files.writeString(attachment, "sc09-content", StandardCharsets.UTF_8);

        Mail mail = htmlMail("SC-09-batch");
        mail.setMailTo(new String[] {"b1@test.local", "b2@test.local", "b3@test.local"});
        mail.setAttachments(List.of(attachment.toFile()));

        assertThatCode(() -> service.sendBatchMailWithFile(mail)).doesNotThrowAnyException();

        // 逐信箱核對 SMTP envelope 實際投遞對象，避免只看 To 標頭與總數而漏掉多封都投給同一人的錯誤。
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(3);
        for (String recipient : List.of("b1@test.local", "b2@test.local", "b3@test.local")) {
            List<MimeMessage> recipientMessages = greenMail
                    .findReceivedMessages(user -> recipient.equals(user.getEmail()), message -> true)
                    .toList();
            assertThat(recipientMessages).as("信箱 %s 必須恰好收到一封", recipient).hasSize(1);
            MimeMessage message = recipientMessages.get(0);
            assertThat(message.getSubject()).isEqualTo("SC-09-batch");
            assertThat(message.getAllRecipients()).extracting(Object::toString).contains(recipient);
            Part htmlPart = findFirstMimeType(message, "text/html");
            assertThat(htmlPart).as("信箱 %s 的本文必須保留 text/html MIME 類型", recipient).isNotNull();
            assertThat(String.valueOf(htmlPart.getContent())).isEqualTo("<p>SC-09-batch</p>");
            assertAttachmentBytes(
                    message, "sc09-attachment.txt", "sc09-content".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static List<JavaMailSenderImpl> candidateSenders(MailServiceImpl service) throws Exception {
        Field candidatesField = MailServiceImpl.class.getDeclaredField("candidates");
        candidatesField.setAccessible(true);
        List<?> candidates = (List<?>) candidatesField.get(service);
        Method senderAccessor = candidates.get(0).getClass().getDeclaredMethod("sender");
        senderAccessor.setAccessible(true);
        return candidates.stream()
                .map(candidate -> {
                    try {
                        return (JavaMailSenderImpl) senderAccessor.invoke(candidate);
                    } catch (ReflectiveOperationException e) {
                        throw new AssertionError(e);
                    }
                })
                .toList();
    }

    /**
     * 核對三組候選全敗的彙整例外：摘要含「已嘗試 3 組」與三個識別及原因，suppressed 恰三個且依序與候選
     * 一對一對應（未具名候選只以 host:port 標示），三組原因互異，整段例外鏈不含任何帳號密碼。
     */
    private static void assertThreeCandidateFailure(Throwable error, String operationName, int unnamedPort) {
        MailFailoverException failure = (MailFailoverException) error;
        String primary = "primary(127.0.0.1:1) - MailSendException";
        String secondary =
                "secondary(127.0.0.1:" + greenMail.getSmtp().getPort() + ") - MailAuthenticationException";
        String unnamed = "127.0.0.1:" + unnamedPort + " - 整體逾時（overall-timeout）已到期";

        assertThat(failure.getMessage())
                .startsWith("郵件發送失敗（" + operationName + "）：已嘗試 3 組")
                .endsWith("-> " + primary + "; " + secondary + "; " + unnamed)
                .doesNotContain("null(");
        assertThat(failure.getSuppressed())
                .extracting(Throwable::getMessage)
                .containsExactly(primary, secondary, unnamed);

        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        assertThat(trace.toString())
                .doesNotContain("wrong-user")
                .doesNotContain("wrong-password")
                .doesNotContain("tertiaryUser")
                .doesNotContain("tertiaryPw");
    }

    /** 未具名第三組候選的端點：初始化時立即關閉連線，發送時預設不回應（直到整體逾時）。 */
    private static SwitchableSmtpEndpoint unnamedEndpoint() throws Exception {
        return new SwitchableSmtpEndpoint(
                "127.0.0.1", tertiaryGreenMail.getSmtp().getPort(), SwitchableSmtpEndpoint.Mode.CLOSE);
    }

    /**
     * 建立兩個具名、一個未具名共三組皆會失敗、且失敗原因互異的候選：primary 拒絕連線、secondary 認證失敗、
     * 未具名第三組接受連線但不回應，於整體逾時到期時被截止。第三組端點恢復為轉送後即可投遞。
     */
    private static MailServiceImpl allServersDownService(SwitchableSmtpEndpoint unnamed) {
        MailPropertyConfig cfg = config(
                unreachableServer("primary", 1),
                greenMailServer("secondary", greenMail.getSmtp().getPort(), "wrong-user", "wrong-password"),
                greenMailServer(null, unnamed.port(), "tertiaryUser", "tertiaryPw"));
        // 單項讀取逾時大於整體上限，確保未具名候選的失敗原因是整體逾時而非 socket read timeout。
        cfg.setReadTimeout(10_000);
        cfg.getFailover().setOverallTimeout(ALL_DOWN_OVERALL_TIMEOUT_MS);
        MailServiceImpl service = new MailServiceImpl(cfg);
        assertThatThrownBy(service::setInitData).isInstanceOf(jakarta.mail.MessagingException.class);
        unnamed.switchTo(SwitchableSmtpEndpoint.Mode.HOLD);
        return service;
    }

    private static void assertNothingDelivered() {
        assertThat(greenMail.getReceivedMessages()).isEmpty();
        assertThat(tertiaryGreenMail.getReceivedMessages()).isEmpty();
    }

    private static MimeMessage singleRecoveredMessage(String expectedSubject) throws Exception {
        MimeMessage[] messages = tertiaryGreenMail.getReceivedMessages();
        assertThat(messages).as("恢復後須由未具名候選恰好投遞一封").hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo(expectedSubject);
        assertThat(greenMail.getReceivedMessages()).isEmpty();
        return messages[0];
    }

    private static void assertAttachment(MimeMessage message, String expectedName, String expectedContent)
            throws Exception {
        Part attachment = findAttachment(message, expectedName);
        assertThat(attachment).as("附件 %s", expectedName).isNotNull();
        assertThat(new String(attachment.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo(expectedContent);
    }

    private static void assertAttachmentBytes(MimeMessage message, String expectedName, byte[] expectedContent)
            throws Exception {
        Part attachment = findAttachment(message, expectedName);
        assertThat(attachment).as("附件 %s", expectedName).isNotNull();
        assertThat(attachment.getInputStream().readAllBytes()).isEqualTo(expectedContent);
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
