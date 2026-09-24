package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import com.zipe.model.Mail;
import com.zipe.service.impl.MailFailoverTlsTestSupport.ScriptableTlsSmtpServer;
import com.zipe.service.impl.MailFailoverTlsTestSupport.TlsFixture;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

/**
 * REQ-MAIL-FAILOVER-020 情境測試（第二批）：smtps 組別的讀取／寫入逾時、多階段累積截止、
 * 合法憑證正面送達，以及可觀測性防洩漏，對應情境測試計畫 SC-024、SC-025、SC-026、SC-028、SC-029。
 *
 * <p>與 {@link MailServiceSmtpsTimeoutTest} 不同，本檔以
 * {@link MailFailoverTlsTestSupport} 於測試執行時以 JDK 內建 {@code keytool}（不下載任何相依、
 * 不連外）產生一張 {@code SAN=dns:localhost} 的自簽憑證，取代 GreenMail 內建、無 SAN 的測試憑證，
 * 使測試伺服器能通過生產程式碼 {@code DeadlineSocketFactory} 的標準主機名稱驗證，
 * 進而驗證「合法憑證＋正確主機名稱時應成功送達」的正面案例（SC-028），
 * 以及需要完整 TLS 交握後才能觀察到的 read-timeout／write-timeout 行為（SC-024、SC-025）。</p>
 */
class MailServiceSmtpsScenariosTest {

    @RegisterExtension
    static GreenMailExtension greenMailSmtp = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("smtpsBackupUser", "smtpsBackupPw"));

    private static TlsFixture tlsFixture;
    private static SSLContext previousDefaultSslContext;

    private final List<ScriptableTlsSmtpServer> servers = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void generateSelfSignedCertificate(@TempDir Path tempDir) throws Exception {
        tlsFixture = MailFailoverTlsTestSupport.generateSelfSignedFixture(tempDir);
        previousDefaultSslContext = SSLContext.getDefault();
        // DeadlineSocketFactory 委派至 SSLSocketFactory.getDefault()，須安裝信任本測試憑證的 JVM 預設 SSLContext。
        SSLContext.setDefault(tlsFixture.clientContext());
    }

    @AfterAll
    static void restoreDefaultSslContext() {
        if (previousDefaultSslContext != null) {
            SSLContext.setDefault(previousDefaultSslContext);
        }
    }

    @AfterEach
    void closeServers() throws IOException {
        for (ScriptableTlsSmtpServer server : servers) {
            server.close();
        }
        servers.clear();
    }

    private ScriptableTlsSmtpServer startServer(ScriptableTlsSmtpServer.ConnectionHandler handler) throws IOException {
        ScriptableTlsSmtpServer server = new ScriptableTlsSmtpServer(tlsFixture.serverContext(), handler);
        servers.add(server);
        return server;
    }

    private static MailServerProperty smtpsServer(String name, int port) {
        MailServerProperty server = new MailServerProperty();
        server.setName(name);
        server.setHost(MailFailoverTlsTestSupport.CERT_HOST);
        server.setPort(String.valueOf(port));
        server.setSmtpAuthEnable(false);
        server.setTransportProtocol("smtps");
        return server;
    }

    /**
     * SC-024／SC-051：smtps 組別的 read-timeout 真正生效——伺服器已完成 TLS 交握（區別於連線／交握階段的
     * connection-timeout），僅是完成交握後不回任何 SMTP 回應，須在 read-timeout 加容忍值內結束並切換至
     * 第二組可用 SMTP 伺服器，由備援組實際收件且內容正確，而不只是拋出彙整例外
     * （AC-020-02、AC-015-02；先前版本僅驗證耗時上下限，未證明逾時後確實透過容錯切換送達）。
     */
    @Test
    void smtpsReadTimeout_isConfigurable_andTakesEffect_afterSuccessfulHandshake() throws Exception {
        AtomicBoolean handshakeCompleted = new AtomicBoolean(false);
        ScriptableTlsSmtpServer server = startServer((reader, writer) -> {
            // ScriptableTlsSmtpServer.serve() 會先呼叫 socket.startHandshake() 且成功返回後才執行本
            // handler，故進入此處即代表 TLS 交握已完成；以旗標明確證實這點，避免僅憑耗時區間
            // 間接推論（耗時區間無法排除「交握卡住後失敗、又剛好落在容忍窗內」的反例，
            // review 節點確認的既有缺口）。
            handshakeCompleted.set(true);
            // 刻意在 TLS 交握完成後不回任何位元組，讓客戶端卡在等待 220 greeting 直到 read-timeout。
            sleepQuietly(15_000);
        });

        MailServerProperty primaryCandidate = smtpsServer("hanging-smtps-read", server.port());
        MailServerProperty backupCandidate = greenMailServer(
                "backup-smtp-after-read-timeout", greenMailSmtp.getSmtp().getPort(), "smtpsBackupUser", "smtpsBackupPw");
        MailPropertyConfig cfg = new MailPropertyConfig();
        cfg.getServers().add(primaryCandidate);
        cfg.getServers().add(backupCandidate);
        cfg.setConnectionTimeout(3000);
        cfg.setReadTimeout(300);
        cfg.setWriteTimeout(3000);
        cfg.getFailover().setOverallTimeout(6000L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // 交握完成、等待回應逾時，初始化階段預期失敗，仍保留候選清單供後續發送嘗試。
        }

        long start = System.currentTimeMillis();
        service.simpleMailSend(plainTextMail("SC-024-SC-051"));
        long elapsed = System.currentTimeMillis() - start;

        assertThat(handshakeCompleted.get())
                .as("伺服器端須已完成 TLS 交握才進入 handler，此測試才能證明是 read-timeout 而非 connection-timeout／交握失敗生效")
                .isTrue();

        MimeMessage[] messages = greenMailSmtp.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-024-SC-051");
        assertThat(GreenMailUtil.getBody(messages[0])).isEqualTo("body-SC-024-SC-051");

        // read-timeout 覆寫為 300ms，遠小於 6000ms 的整體逾時；若未真正生效，只能靠整體逾時兜底，
        // 耗時將遠高於此處的寬鬆上限。
        assertThat(elapsed).isLessThan(3000);
        // 下限確保耗時貼近本組 read-timeout（300ms）本身，而非誤套用遠小的
        // connection-timeout（TLS 交握已於伺服器端完成，不應在交握階段就被截斷）。
        assertThat(elapsed).isGreaterThanOrEqualTo(250);
    }

    /**
     * SC-025：smtps 組別的 write-timeout 在實際 DATA 寫入階段阻塞時生效——伺服器已送出 354，
     * 進入 DATA 階段後刻意不再讀取任何位元組，讓客戶端寫入大附件時因對端無法消化而阻塞於底層
     * socket 寫入，須在 write-timeout 加容忍值內結束（AC-020-03）。
     */
    @Test
    void smtpsWriteTimeout_isConfigurable_andTakesEffect_duringDataPhase(@TempDir Path tempDir) throws Exception {
        AtomicBoolean reachedDataPhase = new AtomicBoolean(false);
        ScriptableTlsSmtpServer server = startServer((reader, writer) -> {
            ScriptableTlsSmtpServer.reply(writer, "220 localhost ESMTP ready");
            String line;
            while ((line = reader.readLine()) != null) {
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    ScriptableTlsSmtpServer.reply(writer, "250-localhost\r\n250 8BITMIME");
                } else if (upper.startsWith("MAIL FROM") || upper.startsWith("RCPT TO")) {
                    ScriptableTlsSmtpServer.reply(writer, "250 OK");
                } else if (upper.equals("DATA")) {
                    ScriptableTlsSmtpServer.reply(writer, "354 End data with <CR><LF>.<CR><LF>");
                    reachedDataPhase.set(true);
                    // 進入 DATA 階段後刻意不再讀取任何位元組（不可再呼叫 readLine），
                    // 讓客戶端持續寫入的大附件在 TCP 接收緩衝區填滿後阻塞於底層 socket 寫入。
                    sleepQuietly(15_000);
                    return;
                } else {
                    ScriptableTlsSmtpServer.reply(writer, "250 OK");
                }
            }
        });

        Path bigAttachment = tempDir.resolve("big-attachment.bin");
        byte[] payload = new byte[24 * 1024 * 1024];
        new Random(42).nextBytes(payload);
        Files.write(bigAttachment, payload);

        MailServerProperty candidate = smtpsServer("hanging-smtps-write", server.port());
        MailPropertyConfig cfg = new MailPropertyConfig();
        cfg.getServers().add(candidate);
        cfg.setConnectionTimeout(3000);
        cfg.setReadTimeout(3000);
        cfg.setWriteTimeout(800);
        cfg.getFailover().setOverallTimeout(8000L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // 初始化測試連線階段沒有附件內容，通常可成功交握，僅記錄以避免遺漏例外。
        }

        Mail mail = plainTextMail("SC-025");
        mail.setAttachments(List.of(bigAttachment.toFile()));

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> service.attachedSend(mail)).isInstanceOf(MailFailoverException.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(reachedDataPhase.get()).as("伺服器須已送出 354 進入 DATA 階段，此測試才具意義").isTrue();
        // write-timeout 覆寫為 800ms，遠小於 8000ms 的整體逾時；若未真正生效，只能靠整體逾時兜底。
        assertThat(elapsed).isLessThan(4000);
    }

    /** SC-026：smtps 組別於多階段（各階段個別未逾時但累積超過 overall-timeout）情境同樣被整體截止。 */
    @Test
    void smtpsOverallTimeout_boundsWholeOperation_acrossMultipleProtocolStages() throws Exception {
        int stageDelayMs = 300;
        AtomicBoolean accepted = new AtomicBoolean(false);
        ScriptableTlsSmtpServer server = startServer((reader, writer) -> {
            sleepQuietly(stageDelayMs);
            ScriptableTlsSmtpServer.reply(writer, "220 localhost ESMTP ready");
            boolean readingData = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (readingData) {
                    if (".".equals(line)) {
                        accepted.set(true);
                        sleepQuietly(stageDelayMs);
                        ScriptableTlsSmtpServer.reply(writer, "250 queued");
                        readingData = false;
                    }
                    continue;
                }
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    sleepQuietly(stageDelayMs);
                    ScriptableTlsSmtpServer.reply(writer, "250-localhost\r\n250 8BITMIME");
                } else if (upper.startsWith("MAIL FROM") || upper.startsWith("RCPT TO")) {
                    sleepQuietly(stageDelayMs);
                    ScriptableTlsSmtpServer.reply(writer, "250 OK");
                } else if (upper.equals("DATA")) {
                    sleepQuietly(stageDelayMs);
                    ScriptableTlsSmtpServer.reply(writer, "354 End data with <CR><LF>.<CR><LF>");
                    readingData = true;
                } else if (upper.equals("QUIT")) {
                    ScriptableTlsSmtpServer.reply(writer, "221 Bye");
                    return;
                } else {
                    ScriptableTlsSmtpServer.reply(writer, "250 OK");
                }
            }
        });

        MailServerProperty candidate = smtpsServer("multi-stage-smtps", server.port());
        MailPropertyConfig cfg = new MailPropertyConfig();
        cfg.getServers().add(candidate);
        cfg.setConnectionTimeout(2000);
        cfg.setReadTimeout(2000);
        cfg.setWriteTimeout(2000);
        cfg.getFailover().setOverallTimeout(500L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-026-multi-stage")))
                .isInstanceOf(MailFailoverException.class)
                .hasMessageContaining("multi-stage-smtps")
                .hasMessageContaining("整體逾時");
        long elapsed = System.currentTimeMillis() - start;

        // 每個階段只延遲 300ms，均低於個別逾時（2000ms）；若只限制各 socket 操作而非整體，
        // 累積延遲（連線＋EHLO＋MAIL＋RCPT＋DATA 至少 5 個階段）將遠超過 1.5 秒。
        assertThat(elapsed).isLessThanOrEqualTo(1500);
    }

    /**
     * SC-028：修正逾時機制後，smtps 組別在合法憑證（主機名稱與憑證 SAN 相符）情境下，
     * 仍能以真實 SSL 交握成功送達郵件，而非因套用逾時或截止機制而退化為連線失敗或明文對談
     * （AC-020-09 正面案例；{@link MailServiceSmtpsTimeoutTest} 僅驗證失敗路徑的主機名稱驗證）。
     */
    @Test
    void smtpsWithLegitimateCertificate_deliversSuccessfully() throws Exception {
        AtomicReference<String> capturedData = new AtomicReference<>("");
        ScriptableTlsSmtpServer server = startServer((reader, writer) -> {
            ScriptableTlsSmtpServer.reply(writer, "220 localhost ESMTP ready");
            StringBuilder dataBuffer = new StringBuilder();
            boolean readingData = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (readingData) {
                    if (".".equals(line)) {
                        capturedData.set(dataBuffer.toString());
                        ScriptableTlsSmtpServer.reply(writer, "250 OK queued");
                        readingData = false;
                    } else {
                        dataBuffer.append(line).append("\n");
                    }
                    continue;
                }
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    ScriptableTlsSmtpServer.reply(writer, "250-localhost\r\n250 8BITMIME");
                } else if (upper.startsWith("MAIL FROM") || upper.startsWith("RCPT TO")) {
                    ScriptableTlsSmtpServer.reply(writer, "250 OK");
                } else if (upper.equals("DATA")) {
                    ScriptableTlsSmtpServer.reply(writer, "354 End data with <CR><LF>.<CR><LF>");
                    readingData = true;
                } else if (upper.equals("QUIT")) {
                    ScriptableTlsSmtpServer.reply(writer, "221 Bye");
                    return;
                } else {
                    ScriptableTlsSmtpServer.reply(writer, "250 OK");
                }
            }
        });

        MailServerProperty candidate = smtpsServer("legit-smtps", server.port());
        MailPropertyConfig cfg = new MailPropertyConfig();
        cfg.getServers().add(candidate);
        cfg.setConnectionTimeout(3000);
        cfg.setReadTimeout(3000);
        cfg.setWriteTimeout(3000);
        cfg.getFailover().setOverallTimeout(8000L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        service.simpleMailSend(plainTextMail("SC-028"));

        assertThat(capturedData.get()).contains("SC-028");
    }

    /**
     * SC-029：smtps 組別逾時或認證失敗時，日誌與彙整例外仍可辨識是哪一組（{@code name(host:port)}）
     * 與安全的失敗原因，且不外洩憑證或伺服器回應原文中的敏感片段（AC-020-10）。涵蓋兩種具名故障：
     * 一組完成交握後無回應（read-timeout），一組完成 AUTH LOGIN 交換後遭伺服器拒絕且回應內夾帶
     * 敏感字串。
     */
    @Test
    void smtpsFailures_areObservableByLabel_withoutLeakingSensitiveResponseText() throws Exception {
        String sensitiveToken = "SECRET_LEAK_TOKEN_SC029";

        ScriptableTlsSmtpServer timeoutServer =
                startServer((reader, writer) -> sleepQuietly(15_000));

        ScriptableTlsSmtpServer authFailureServer = startServer((reader, writer) -> {
            ScriptableTlsSmtpServer.reply(writer, "220 localhost ESMTP ready");
            String line;
            while ((line = reader.readLine()) != null) {
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    ScriptableTlsSmtpServer.reply(writer, "250-localhost\r\n250 AUTH LOGIN PLAIN");
                } else if (upper.startsWith("AUTH")) {
                    ScriptableTlsSmtpServer.reply(writer, "334 VXNlcm5hbWU6");
                    reader.readLine();
                    ScriptableTlsSmtpServer.reply(writer, "334 UGFzc3dvcmQ6");
                    reader.readLine();
                    // 伺服器回應中刻意夾帶敏感字串，驗證此文字不得外洩至日誌或例外鏈。
                    ScriptableTlsSmtpServer.reply(writer, "535 5.7.8 Authentication failed ref=" + sensitiveToken);
                    return;
                } else if (upper.equals("QUIT")) {
                    ScriptableTlsSmtpServer.reply(writer, "221 Bye");
                    return;
                } else {
                    ScriptableTlsSmtpServer.reply(writer, "250 OK");
                }
            }
        });

        MailServerProperty timeoutCandidate = smtpsServer("named-smtps-timeout", timeoutServer.port());
        MailServerProperty authCandidate = smtpsServer("named-smtps-auth-fail", authFailureServer.port());
        authCandidate.setSmtpAuthEnable(true);
        authCandidate.setUsername("smtpsUser");
        authCandidate.setPa55word("WRONG_SMTPS_PASSWORD");

        MailPropertyConfig cfg = new MailPropertyConfig();
        cfg.getServers().add(timeoutCandidate);
        cfg.getServers().add(authCandidate);
        cfg.setConnectionTimeout(2000);
        cfg.setReadTimeout(500);
        cfg.setWriteTimeout(2000);
        cfg.getFailover().setOverallTimeout(6000L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // 兩組皆會於初始化階段失敗，仍保留候選清單供後續發送嘗試。
        }

        ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);

        MailFailoverException failure = (MailFailoverException) org.assertj.core.api.Assertions.catchThrowable(
                        () -> service.simpleMailSend(plainTextMail("SC-029")));

        assertThat(failure).isNotNull();
        assertThat(failure.getMessage()).contains("named-smtps-timeout").contains("named-smtps-auth-fail");
        assertThat(failure.getSuppressed()).hasSize(2);

        String allLogText = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        String exceptionText = exceptionText(failure);

        assertThat(allLogText).contains("named-smtps-timeout").contains("named-smtps-auth-fail");
        assertThat(allLogText).doesNotContain(sensitiveToken).doesNotContain("WRONG_SMTPS_PASSWORD");
        assertThat(exceptionText).doesNotContain(sensitiveToken).doesNotContain("WRONG_SMTPS_PASSWORD");
    }

    private static void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String exceptionText(Throwable failure) {
        java.io.StringWriter text = new java.io.StringWriter();
        failure.printStackTrace(new java.io.PrintWriter(text));
        return text.toString();
    }
}
