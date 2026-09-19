package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import com.zipe.model.Mail;
import com.zipe.service.impl.MailFailoverPlainProtocolTestSupport.ScriptablePlainSmtpServer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

/**
 * 明文 SMTP 協定層各種失敗型態的容錯切換情境測試，對應情境測試計畫 SC-009、SC-010、SC-011、
 * SC-025、SC-027。與 {@link MailServiceFailoverTest}（連線層失敗）及
 * {@link MailServiceSmtpsScenariosTest}（smtps 協定層失敗）互補，涵蓋純 SMTP（非 TLS）
 * 在「已連線之後」的各種失敗型態：讀取黑洞、AUTH 535、DATA 階段 550 拒收，
 * 以及連線階段（connection-timeout）與 DATA 寫入階段（write-timeout）的實際黑洞計時驗證。
 */
class MailServiceProtocolFailoverTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("backupUser", "backupPw"));

    private final List<ScriptablePlainSmtpServer> servers = new CopyOnWriteArrayList<>();
    private ServerSocket connectionBlackHole;
    private final List<Socket> blackHoleFillerConnections = new CopyOnWriteArrayList<>();

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void closeServers() throws IOException {
        for (ScriptablePlainSmtpServer server : servers) {
            server.close();
        }
        servers.clear();
        for (Socket socket : blackHoleFillerConnections) {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
        blackHoleFillerConnections.clear();
        if (connectionBlackHole != null && !connectionBlackHole.isClosed()) {
            connectionBlackHole.close();
        }
    }

    private ScriptablePlainSmtpServer startServer(ScriptablePlainSmtpServer.ConnectionHandler handler)
            throws IOException {
        ScriptablePlainSmtpServer server = new ScriptablePlainSmtpServer(handler);
        servers.add(server);
        return server;
    }

    private static MailServerProperty plainServer(String name, int port) {
        MailServerProperty server = new MailServerProperty();
        server.setName(name);
        server.setHost("127.0.0.1");
        server.setPort(String.valueOf(port));
        server.setSmtpAuthEnable(false);
        server.setTransportProtocol("smtp");
        return server;
    }

    /**
     * SC-009：第一組完成 TCP 連線但完全不回應（讀取黑洞，非連線被拒），套用 read-timeout 後
     * 仍須視為該組失敗並切換至第二組，由第二組實際送達。
     */
    @Test
    void readBlackHole_afterConnect_fallsBackToSecondServer_andDelivers() throws Exception {
        ScriptablePlainSmtpServer hanging = startServer((reader, writer) -> sleepQuietly(15_000));

        MailServerProperty primary = plainServer("read-blackhole-primary", hanging.port());
        MailServerProperty backup =
                greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw");
        MailPropertyConfig cfg = config(primary, backup);
        cfg.setReadTimeout(400);
        cfg.setConnectionTimeout(1000);
        cfg.setWriteTimeout(1000);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // primary 於初始化階段亦會因讀取黑洞逾時失敗，仍保留候選清單供後續發送嘗試。
        }

        service.simpleMailSend(plainTextMail("SC-009"));

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-009");
    }

    /**
     * SC-010：第一組於 AUTH 交換後遭伺服器回覆 535 認證失敗（非連線層失敗），仍須切換至
     * 第二組並由其實際送達。
     */
    @Test
    void authenticationRejected535_fallsBackToSecondServer_andDelivers() throws Exception {
        ScriptablePlainSmtpServer authFailing = startServer((reader, writer) -> {
            ScriptablePlainSmtpServer.reply(writer, "220 localhost ESMTP ready");
            String line;
            while ((line = reader.readLine()) != null) {
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    ScriptablePlainSmtpServer.reply(writer, "250-localhost\r\n250 AUTH LOGIN PLAIN");
                } else if (upper.startsWith("AUTH")) {
                    ScriptablePlainSmtpServer.reply(writer, "334 VXNlcm5hbWU6");
                    reader.readLine();
                    ScriptablePlainSmtpServer.reply(writer, "334 UGFzc3dvcmQ6");
                    reader.readLine();
                    ScriptablePlainSmtpServer.reply(writer, "535 5.7.8 Authentication failed");
                    return;
                } else if (upper.equals("QUIT")) {
                    ScriptablePlainSmtpServer.reply(writer, "221 Bye");
                    return;
                } else {
                    ScriptablePlainSmtpServer.reply(writer, "250 OK");
                }
            }
        });

        MailServerProperty primary = plainServer("auth-535-primary", authFailing.port());
        primary.setSmtpAuthEnable(true);
        primary.setUsername("wrongUser");
        primary.setPa55word("wrongPassword");
        MailServerProperty backup =
                greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw");
        MailPropertyConfig cfg = config(primary, backup);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // primary 於初始化階段測試連線亦會因認證失敗而失敗，仍保留候選清單供後續發送嘗試。
        }

        service.simpleMailSend(plainTextMail("SC-010"));

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-010");
    }

    /**
     * SC-011：第一組完成信封交換（MAIL FROM／RCPT TO）並進入 DATA 階段後遭伺服器回覆 550
     * 拒收（而非在 DATA 前就失敗），仍須切換至第二組並由其實際送達完整信件。
     */
    @Test
    void dataPhaseRejected550_fallsBackToSecondServer_andDelivers() throws Exception {
        AtomicBoolean reachedDataPhase = new AtomicBoolean(false);
        ScriptablePlainSmtpServer dataRejecting = startServer((reader, writer) -> {
            ScriptablePlainSmtpServer.reply(writer, "220 localhost ESMTP ready");
            String line;
            while ((line = reader.readLine()) != null) {
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    ScriptablePlainSmtpServer.reply(writer, "250-localhost\r\n250 8BITMIME");
                } else if (upper.startsWith("MAIL FROM") || upper.startsWith("RCPT TO")) {
                    ScriptablePlainSmtpServer.reply(writer, "250 OK");
                } else if (upper.equals("DATA")) {
                    reachedDataPhase.set(true);
                    ScriptablePlainSmtpServer.reply(writer, "550 5.7.1 Mailbox unavailable, rejected");
                } else if (upper.equals("QUIT")) {
                    ScriptablePlainSmtpServer.reply(writer, "221 Bye");
                    return;
                } else {
                    ScriptablePlainSmtpServer.reply(writer, "250 OK");
                }
            }
        });

        MailServerProperty primary = plainServer("data-550-primary", dataRejecting.port());
        MailServerProperty backup =
                greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw");
        MailPropertyConfig cfg = config(primary, backup);

        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        Mail mail = plainTextMail("SC-011");
        service.simpleMailSend(mail);

        assertThat(reachedDataPhase.get()).as("伺服器須已進入 DATA 階段，此測試才具意義").isTrue();
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-011");
    }

    /**
     * SC-025：mail.connection-timeout 覆寫為短值時，須實際限制 TCP 連線建立（而非讀取）階段的
     * 等待時間。以「backlog 已被單一未 accept 連線佔滿」的方式製造連線階段黑洞——後續 connect()
     * 會在 TCP 層被靜默丟棄，只能靠客戶端自身的 connect timeout 結束等待，藉此與 read-timeout
     * 情境（{@link MailServiceTimeoutAndLimitsTest#readTimeout_isConfigurable_andTakesEffect}）
     * 明確區分測的是連線階段。
     */
    @Test
    void connectionTimeout_isConfigurable_andBoundsConnectStageSpecifically() throws Exception {
        // backlog 設為 1，但不同平台／核心對「已完成握手但未 accept()」佇列的實際容量詮釋不一致
        // （常見於 backlog 值被視為下限而非精確值），因此以遠高於常見預設值的連線數量填滿佇列，
        // 確保後續連線在 TCP 層被靜默丟棄、只能靠客戶端自身 connect timeout 結束等待，
        // 藉此與 read-timeout 情境（已完成連線、僅未收到回應）明確區分測的是連線建立階段。
        connectionBlackHole = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        int port = connectionBlackHole.getLocalPort();
        // 佇列一旦真正填滿，後續連線嘗試本身就會卡住；以短逾時偵測「已填滿」並停止繼續嘗試，
        // 而非預先假設固定連線數一定足夠或不會過量（過量會導致填充迴圈本身也逾時失敗）。
        for (int i = 0; i < 64; i++) {
            Socket filler = new Socket();
            try {
                filler.connect(new java.net.InetSocketAddress("127.0.0.1", port), 300);
                blackHoleFillerConnections.add(filler);
            } catch (IOException fullQueue) {
                filler.close();
                break;
            }
        }

        MailServerProperty server = plainServer("connect-blackhole", port);
        MailPropertyConfig cfg = config(server);
        cfg.setConnectionTimeout(400);
        cfg.setReadTimeout(6000);
        cfg.setWriteTimeout(6000);

        MailServiceImpl service = new MailServiceImpl(cfg);

        long start = System.currentTimeMillis();
        assertThatThrownBy(service::setInitData).isInstanceOf(MessagingException.class);
        long elapsed = System.currentTimeMillis() - start;

        // 預設 connectionTimeout 為 5000ms、readTimeout 覆寫為刻意遠大於它的 6000ms；
        // 覆寫 connectionTimeout 為 400ms 後理應遠早於兩者失敗，證明覆寫值確實作用於
        // 連線建立階段本身（而非等到讀取逾時或整體逾時才結束）。
        assertThat(elapsed).isLessThan(4000);
    }

    /**
     * SC-027：純 SMTP（非 smtps）組別於 DATA 階段寫入大附件時，若對端不再讀取任何位元組，
     * write-timeout 覆寫值須實際限制寫入等待時間，補齊既有 smtps 對應測試
     * （{@link MailServiceSmtpsScenariosTest#smtpsWriteTimeout_isConfigurable_andTakesEffect_duringDataPhase}）
     * 在純 SMTP 協定下的對應案例。
     */
    @Test
    void writeTimeout_isConfigurable_andTakesEffect_duringPlainSmtpDataPhase(@TempDir Path tempDir) throws Exception {
        AtomicBoolean reachedDataPhase = new AtomicBoolean(false);
        ScriptablePlainSmtpServer server = startServer((reader, writer) -> {
            ScriptablePlainSmtpServer.reply(writer, "220 localhost ESMTP ready");
            String line;
            while ((line = reader.readLine()) != null) {
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    ScriptablePlainSmtpServer.reply(writer, "250-localhost\r\n250 8BITMIME");
                } else if (upper.startsWith("MAIL FROM") || upper.startsWith("RCPT TO")) {
                    ScriptablePlainSmtpServer.reply(writer, "250 OK");
                } else if (upper.equals("DATA")) {
                    ScriptablePlainSmtpServer.reply(writer, "354 End data with <CR><LF>.<CR><LF>");
                    reachedDataPhase.set(true);
                    // 進入 DATA 階段後刻意不再讀取任何位元組，讓客戶端持續寫入的大附件
                    // 在 TCP 接收緩衝區填滿後阻塞於底層 socket 寫入。
                    sleepQuietly(15_000);
                    return;
                } else {
                    ScriptablePlainSmtpServer.reply(writer, "250 OK");
                }
            }
        });

        Path bigAttachment = tempDir.resolve("plain-big-attachment.bin");
        byte[] payload = new byte[24 * 1024 * 1024];
        new Random(7).nextBytes(payload);
        Files.write(bigAttachment, payload);

        MailServerProperty candidate = plainServer("plain-write-blackhole", server.port());
        MailPropertyConfig cfg = config(candidate);
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

        Mail mail = plainTextMail("SC-027");
        mail.setAttachments(List.of(bigAttachment.toFile()));

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> service.attachedSend(mail)).isInstanceOf(MailFailoverException.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(reachedDataPhase.get()).as("伺服器須已送出 354 進入 DATA 階段，此測試才具意義").isTrue();
        // write-timeout 覆寫為 800ms，遠小於 8000ms 的整體逾時；若未真正生效，只能靠整體逾時兜底。
        assertThat(elapsed).isLessThan(4000);
    }

    private static void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
