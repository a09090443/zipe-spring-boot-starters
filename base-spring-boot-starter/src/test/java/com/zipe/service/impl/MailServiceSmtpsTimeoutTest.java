package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * REQ-MAIL-FAILOVER-020 情境測試：隱式 TLS（{@code transport-protocol=smtps}，例如 465 埠）組別的
 * 連線／讀取／寫入逾時與整體截止須真正生效，對應情境測試計畫 SC-024 ~ SC-029、SC-031（smtps 送達部分）。
 *
 * <p>修復前 {@code buildSender()}／{@code senderForAttempt()} 僅寫入 {@code mail.smtp.*} 前綴，
 * 而 JavaMail／Angus 對 smtps 組別實際讀取 {@code mail.smtps.*}，導致該組全部逾時與整體截止機制落空。
 * 本測試涵蓋：smtps 專屬前綴的設定雙寫繫結（白箱）、連線逾時與整體逾時對永不完成交握的端點確實生效、
 * smtp 與 smtps 混用清單互不干擾、修復後仍以真實 SSL 交握（含主機名稱驗證）而非退化為明文
 * （回應 REQ-MAIL-FAILOVER-020 的 CONFIRMED HIGH 回歸風險；GreenMail 內建自簽憑證無 SAN，
 * 無法用於驗證「主機名稱相符時成功送達」的正面案例，僅能驗證「交握與驗證機制確實運作」）。</p>
 */
class MailServiceSmtpsTimeoutTest {

    @RegisterExtension
    static GreenMailExtension greenMailSmtps = new GreenMailExtension(ServerSetupTest.SMTPS.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("smtpsUser", "smtpsPw"));

    @RegisterExtension
    static GreenMailExtension greenMailSmtp = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("mixedUser", "mixedPw"));

    private ServerSocket blackHole;
    private SSLContext previousDefaultSslContext;

    @BeforeEach
    void purgeMailboxes() throws Exception {
        greenMailSmtps.purgeEmailFromAllMailboxes();
        greenMailSmtp.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void cleanup() throws IOException {
        if (blackHole != null && !blackHole.isClosed()) {
            blackHole.close();
        }
        if (previousDefaultSslContext != null) {
            SSLContext.setDefault(previousDefaultSslContext);
            previousDefaultSslContext = null;
        }
    }

    /**
     * SC-024：smtps 組別的 connection-timeout 真正生效——修復前固定套用 {@code mail.smtp.*}，
     * smtps 讀不到覆寫值，只能靠背景 socket 自然逾時或整體逾時兜底；修復後應於本組設定值
     * （遠小於整體逾時）加容忍值內結束。
     */
    @Test
    void smtpsConnectionTimeout_isConfigurable_andTakesEffect() throws Exception {
        int port = startBlackHoleServer();
        MailServerProperty server = smtpsServer("hanging-smtps", "127.0.0.1", port);

        MailPropertyConfig cfg = config(server);
        cfg.setConnectionTimeout(300);
        cfg.setReadTimeout(5000);
        cfg.setWriteTimeout(5000);
        cfg.getFailover().setOverallTimeout(10000L);
        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // 黑洞端點永不完成交握，初始化階段預期逾時失敗，仍保留候選清單供後續發送嘗試
        }

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> service.simpleMailSend(plainTextMail("SC-024")))
                .isInstanceOf(MailFailoverException.class);
        long elapsed = System.currentTimeMillis() - start;

        // connection-timeout 覆寫為 300ms，遠小於 10000ms 的整體逾時；若 smtps 前綴未生效，
        // 只能靠整體逾時兜底，耗時將遠高於此處的寬鬆上限
        assertThat(elapsed).isLessThan(2000);
        // SC-050：下限確保確實等待了 connection-timeout（而非被連線拒絕等其他成因瞬間失敗，
        // 使上限斷言在錯誤情境下也恰好通過）——若誤套用 read-timeout（5000ms）反而會等更久，
        // 上限 2000ms 已可攔截；此下限進一步確認耗時貼近本組設定的 300ms 而非近乎 0ms。
        assertThat(elapsed).isGreaterThanOrEqualTo(250);
    }

    /**
     * SC-026：smtps 組別全部逾時時，整體切換時間上限同樣可限制單次呼叫的總耗時，且錯誤訊息明示整體逾時；
     * 重複量測 3 次排除單次執行的偶發排程抖動。
     */
    @Test
    void smtpsOverallTimeout_boundsTotalElapsedTime_repeatedly() throws Exception {
        int port = startBlackHoleServer();

        MailPropertyConfig cfg = new MailPropertyConfig();
        for (int i = 0; i < 5; i++) {
            cfg.getServers().add(smtpsServer("hanging-smtps-" + i, "127.0.0.1", port));
        }
        cfg.setConnectionTimeout(300);
        cfg.setReadTimeout(300);
        cfg.setWriteTimeout(300);
        cfg.getFailover().setOverallTimeout(500L);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // 全部候選在初始化階段皆逾時失敗，仍保留候選清單供後續發送嘗試
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            String subject = "SC-026-" + attempt;
            long start = System.currentTimeMillis();
            assertThatThrownBy(() -> service.simpleMailSend(plainTextMail(subject)))
                    .isInstanceOf(MailFailoverException.class)
                    .hasMessageContaining("整體逾時");
            long elapsed = System.currentTimeMillis() - start;

            // 若無整體上限，5 組個別耗時 300ms 累加將達 1500ms；有整體上限（500ms）應明顯低於此值
            assertThat(elapsed).isLessThanOrEqualTo(1200);
        }
    }

    /** SC-027：同一清單混用 smtp 與 smtps，各自依自身協定套用正確前綴，smtps 逾時後依序切換至可用 smtp 組別送達。 */
    @Test
    void mixedSmtpAndSmtpsServers_smtpsTimesOut_thenFallsBackToSmtp() throws Exception {
        int port = startBlackHoleServer();
        MailServerProperty smtpsCandidate = smtpsServer("primary-smtps", "127.0.0.1", port);

        MailPropertyConfig cfg = config(
                smtpsCandidate, greenMailServer("backup-smtp", greenMailSmtp.getSmtp().getPort(), "mixedUser", "mixedPw"));
        cfg.setConnectionTimeout(300);
        cfg.setReadTimeout(300);
        cfg.setWriteTimeout(300);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // smtps 候選連線階段即逾時失敗，仍保留候選清單供後續發送嘗試
        }

        long start = System.currentTimeMillis();
        service.simpleMailSend(plainTextMail("SC-027"));
        long elapsed = System.currentTimeMillis() - start;

        MimeMessage[] messages = greenMailSmtp.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("SC-027");
        // smtps 組別須確實在自身 connection-timeout（300ms）內結束才會切換，而非拖到更大的預設值
        assertThat(elapsed).isLessThan(3000);
    }

    /**
     * SC-053（黑箱／真實生產路徑，非反射）：smtps 組別若實際指向一個只會明文對話的 SMTP 伺服器
     * （未實作 TLS），必須因 TLS handshake 失敗而視為該組失敗並容錯切換至下一組，
     * 絕不能悄悄退化為明文並與該伺服器完成完整 SMTP 對話——即使該明文伺服器完全正常運作、
     * 也「願意」接受明文連線。本測試不透過反射直接建構 {@code DeadlineSocketFactory}
     * （見 {@link #smtpsHandshake_enforcesHostnameVerification_ratherThanFallingBackToPlaintext()}
     * 的白箱驗證），而是完整走過 {@link MailServiceImpl} 的公開發送路徑，證明
     * {@code senderForAttempt()} 依協定自動計算的 {@code fallbackToPlainSocket=false}
     * 確實在真實情境下生效。
     */
    @Test
    void smtpsCandidate_pointedAtPlaintextServer_neverCompletesPlaintextDialogue_fallsBackToRealBackup()
            throws Exception {
        java.util.concurrent.atomic.AtomicBoolean plaintextDialogueCompleted =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        try (ServerSocket plaintextServer = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            int plaintextPort = plaintextServer.getLocalPort();
            Thread acceptor = new Thread(() -> handlePlaintextImposter(plaintextServer, plaintextDialogueCompleted));
            acceptor.setDaemon(true);
            acceptor.start();

            MailServerProperty imposter = smtpsServer("plaintext-imposter", "127.0.0.1", plaintextPort);
            MailPropertyConfig cfg = config(
                    imposter, greenMailServer("backup-smtp", greenMailSmtp.getSmtp().getPort(), "mixedUser", "mixedPw"));
            cfg.setConnectionTimeout(1000);
            cfg.setReadTimeout(1000);
            cfg.setWriteTimeout(1000);

            MailServiceImpl service = new MailServiceImpl(cfg);
            try {
                service.setInitData();
            } catch (MessagingException ignored) {
                // imposter 於初始化階段的 TLS handshake 預期失敗，仍保留候選清單供後續發送嘗試
            }

            service.simpleMailSend(plainTextMail("SC-053"));

            MimeMessage[] messages = greenMailSmtp.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo("SC-053");
            assertThat(plaintextDialogueCompleted.get())
                    .as("smtps 組別不得因委派工廠找不到憑證／TLS 協定不符而退化為明文，與明文伺服器完成完整 SMTP 對話")
                    .isFalse();
        }
    }

    /**
     * SC-030（STARTTLS 分支）：smtp 組別啟用 STARTTLS，伺服器同意升級（回 220）後卻送出非 TLS 位元組，
     * 使 handshake 失敗。該組必須視為失敗並切換至下一組，同一候選不得略過 TLS 繼續以明文送出
     * MAIL FROM／RCPT／DATA。若實作在 handshake 失敗後重建未啟用 STARTTLS 的 sender 或忽略錯誤續傳，
     * 明文郵件指令計數會大於 0 而失敗；STARTTLS 請求計數則確認確實走到握手階段，而非其他成因提早失敗。
     */
    @Test
    void startTlsHandshakeFailure_neverSendsMailCommandsInPlaintext_andFailsOverToBackup() throws Exception {
        java.util.concurrent.atomic.AtomicInteger startTlsRequests = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger plaintextMailCommands =
                new java.util.concurrent.atomic.AtomicInteger();
        try (ServerSocket startTlsServer = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            Thread acceptor = new Thread(
                    () -> handleBrokenStartTls(startTlsServer, startTlsRequests, plaintextMailCommands));
            acceptor.setDaemon(true);
            acceptor.start();

            MailServerProperty broken = MailFailoverTestSupport.server(
                    "broken-starttls", "127.0.0.1", startTlsServer.getLocalPort(), "user", "pass");
            broken.setSmtpAuthEnable(false);
            broken.setSmtpStartTlsEnable(true);
            MailPropertyConfig cfg = config(
                    broken, greenMailServer("backup-smtp", greenMailSmtp.getSmtp().getPort(), "mixedUser", "mixedPw"));

            MailServiceImpl service = new MailServiceImpl(cfg);
            try {
                service.setInitData();
            } catch (MessagingException ignored) {
                // 初始化連線測試結果不影響本情境；只觀察實際發送階段
            }
            startTlsRequests.set(0);
            plaintextMailCommands.set(0);

            service.simpleMailSend(plainTextMail("SC-030-starttls"));

            MimeMessage[] messages = greenMailSmtp.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).isEqualTo("SC-030-starttls");
            assertThat(startTlsRequests.get())
                    .as("第一組須確實送出 STARTTLS 並進入 handshake，失敗成因才是 TLS 握手")
                    .isPositive();
            assertThat(plaintextMailCommands.get())
                    .as("STARTTLS 握手失敗後，同一組不得以明文送出 MAIL FROM／RCPT／DATA")
                    .isZero();
        }
    }

    /**
     * 宣告支援 STARTTLS、收到 STARTTLS 後回 220 卻送出非 TLS 位元組並關閉連線的 SMTP 伺服器；
     * 同時記錄任何明文郵件交易指令，供斷言同一候選未降級為明文。
     */
    private static void handleBrokenStartTls(
            ServerSocket serverSocket,
            java.util.concurrent.atomic.AtomicInteger startTlsRequests,
            java.util.concurrent.atomic.AtomicInteger plaintextMailCommands) {
        while (!serverSocket.isClosed()) {
            try (Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII))) {
                reply(writer, "220 broken-starttls ESMTP ready");
                String line;
                while ((line = reader.readLine()) != null) {
                    String upper = line.toUpperCase(Locale.ROOT);
                    if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                        reply(writer, "250-broken-starttls\r\n250 STARTTLS");
                    } else if (upper.equals("STARTTLS")) {
                        startTlsRequests.incrementAndGet();
                        reply(writer, "220 Ready to start TLS");
                        reply(writer, "this-is-not-a-tls-record");
                        break;
                    } else if (upper.startsWith("MAIL FROM") || upper.startsWith("RCPT TO") || upper.equals("DATA")) {
                        plaintextMailCommands.incrementAndGet();
                        reply(writer, "250 OK");
                    } else if (upper.equals("QUIT")) {
                        reply(writer, "221 Bye");
                        break;
                    } else {
                        reply(writer, "250 OK");
                    }
                }
            } catch (IOException ignored) {
                // SSL 探測連線或 handshake 失敗後的中斷屬預期；單次連線失敗不影響後續接受。
            }
        }
    }

    /**
     * 模擬一個完全正常、願意接受明文連線的 SMTP 伺服器；若 smtps 組別誤退化為明文，會在此完成整段對話。
     * 迴圈接受多次連線（setInitData 的連線測試與實際發送各會連線一次），避免第二次連線因無人 accept
     * 而拖到逾時，讓測試更快、更穩定地反映真實失敗原因（TLS handshake 失敗，而非單純逾時）。
     */
    private static void handlePlaintextImposter(
            ServerSocket serverSocket, java.util.concurrent.atomic.AtomicBoolean dialogueCompleted) {
        while (!serverSocket.isClosed()) {
            try (Socket socket = serverSocket.accept()) {
                handlePlaintextConnection(socket, dialogueCompleted);
            } catch (IOException ignored) {
                // close() 會關閉 ServerSocket，使 accept() 正常結束；單次連線失敗不影響後續接受。
            }
        }
    }

    private static void handlePlaintextConnection(
            Socket socket, java.util.concurrent.atomic.AtomicBoolean dialogueCompleted) {
        try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII))) {
            reply(writer, "220 plaintext-imposter ESMTP ready");
            boolean readingData = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (readingData) {
                    if (".".equals(line)) {
                        dialogueCompleted.set(true);
                        reply(writer, "250 queued");
                        readingData = false;
                    }
                    continue;
                }
                String upper = line.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                    reply(writer, "250-plaintext-imposter\r\n250 8BITMIME");
                } else if (upper.equals("DATA")) {
                    reply(writer, "354 End data with <CR><LF>.<CR><LF>");
                    readingData = true;
                } else if (upper.equals("QUIT")) {
                    reply(writer, "221 Bye");
                    return;
                } else {
                    reply(writer, "250 OK");
                }
            }
        } catch (IOException ignored) {
            // 預期行為：smtps 組別嘗試 TLS handshake 而非明文對話，本明文伺服器收到的位元組
            // 不是合法 TLS ClientHello，讀寫可能提早中止或拋出例外，此為正常結果。
        }
    }

    private static void reply(BufferedWriter writer, String response) throws IOException {
        writer.write(response);
        writer.write("\r\n");
        writer.flush();
    }

    /**
     * 白箱：smtps 組別的 JavaMailProperties 同時保留既有 {@code mail.smtp.*} 鍵與新增的
     * {@code mail.smtps.*} 鍵，兩者數值一致（AC-020-01～03、AC-020-08）；純 smtp 組別不新增
     * 任何 {@code mail.smtps.*} 鍵，且既有 {@code mail.smtp.*} 鍵不受影響（AC-020-07、AC-020-12：
     * 不新增使用者可設定屬性即修好，逾時鍵沿用既有 mail.* 屬性值，僅雙寫前綴）。
     */
    @Test
    void buildSender_dualWritesTimeoutKeys_forSmtpsProtocol_withoutRegressingSmtpKeys() throws Exception {
        MailPropertyConfig cfg = new MailPropertyConfig();
        cfg.getServers().add(smtpsServer("smtps-target", "127.0.0.1", 1));
        cfg.getServers().add(unreachableServer("smtp-target", 2));
        cfg.setConnectionTimeout(1111);
        cfg.setReadTimeout(2222);
        cfg.setWriteTimeout(3333);

        MailServiceImpl service = new MailServiceImpl(cfg);
        try {
            service.setInitData();
        } catch (MessagingException ignored) {
            // 兩組皆指向未監聽埠，初始化預期全部失敗，仍保留候選清單
        }

        List<?> candidates = (List<?>) ReflectionTestUtils.getField(service, "candidates");
        assertThat(candidates).hasSize(2);
        JavaMailSenderImpl smtpsSender = (JavaMailSenderImpl) ReflectionTestUtils.getField(candidates.get(0), "sender");
        JavaMailSenderImpl smtpSender = (JavaMailSenderImpl) ReflectionTestUtils.getField(candidates.get(1), "sender");

        assertThat(String.valueOf(smtpsSender.getJavaMailProperties().get("mail.smtp.connectiontimeout"))).isEqualTo("1111");
        assertThat(String.valueOf(smtpsSender.getJavaMailProperties().get("mail.smtp.timeout"))).isEqualTo("2222");
        assertThat(String.valueOf(smtpsSender.getJavaMailProperties().get("mail.smtp.writetimeout"))).isEqualTo("3333");
        assertThat(String.valueOf(smtpsSender.getJavaMailProperties().get("mail.smtps.connectiontimeout"))).isEqualTo("1111");
        assertThat(String.valueOf(smtpsSender.getJavaMailProperties().get("mail.smtps.timeout"))).isEqualTo("2222");
        assertThat(String.valueOf(smtpsSender.getJavaMailProperties().get("mail.smtps.writetimeout"))).isEqualTo("3333");
        assertThat(smtpsSender.getJavaMailProperties().get("mail.smtps.socketFactory.class"))
                .isEqualTo("javax.net.ssl.SSLSocketFactory");

        assertThat(String.valueOf(smtpSender.getJavaMailProperties().get("mail.smtp.connectiontimeout"))).isEqualTo("1111");
        assertThat(smtpSender.getJavaMailProperties().get("mail.smtps.connectiontimeout")).isNull();
        assertThat(smtpSender.getJavaMailProperties().get("mail.smtps.timeout")).isNull();
        assertThat(smtpSender.getJavaMailProperties().get("mail.smtps.writetimeout")).isNull();
        assertThat(smtpSender.getJavaMailProperties().get("mail.smtps.socketFactory.class")).isNull();
    }

    /**
     * SC-031 基準：確認 smtps 埠確實以真實 TLS 憑證交握（而非埠號設定錯誤或明文監聽），
     * 作為下一個測試「委派工廠不得退化為明文」的比對基準。GreenMail 內建自簽憑證無 SAN、
     * CN 亦非主機名稱格式（見下一測試說明），故僅能在停用主機名稱比對下驗證憑證確實存在且可交握。
     */
    @Test
    void greenMailSmtpsEndpoint_presentsRealTlsCertificate() throws Exception {
        installTrustAllSslContext();
        SSLContext sslContext = SSLContext.getDefault();
        try (SSLSocket socket = (SSLSocket)
                sslContext.getSocketFactory().createSocket("127.0.0.1", greenMailSmtps.getSmtps().getPort())) {
            socket.startHandshake();
            X509Certificate cert = (X509Certificate) socket.getSession().getPeerCertificates()[0];
            assertThat(cert.getSubjectX500Principal().getName()).contains("GreenMail");
        }
    }

    /**
     * SC-028／SC-031（委派工廠部分）：修復後 smtps 組別由 {@link MailServiceImpl} 內部的
     * {@code DeadlineSocketFactory} 真正發起 SSL 交握並依主機名稱驗證結果決定成敗，而非略過驗證、
     * 或於委派失敗時悄悄退回明文 socket（AC-020-09）。
     * <p>
     * GreenMail 內建自簽憑證僅用於測試、不含 Subject Alternative Name，且 CN 為描述性文字
     * （見 {@link #greenMailSmtpsEndpoint_presentsRealTlsCertificate()} 已核實其存在），並非任何
     * 主機名稱，結構上無法通過任何主機名稱的標準 HTTPS 端點識別比對；這是 GreenMail 測試憑證本身
     * 的限制，非本次程式修正的缺陷。本測試因此驗證「失敗原因必須是憑證／主機名稱驗證失敗」
     * （證明委派工廠確實完成了真實 TLS 交握並執行主機名稱驗證），而不是協定不符或連線被拒
     * 等明文退化的徵狀——若委派失敗時真的退回明文 socket，錯誤型態將是協定層級錯誤而非
     * 憑證驗證例外。
     * </p>
     */
    @Test
    void smtpsHandshake_enforcesHostnameVerification_ratherThanFallingBackToPlaintext() throws Exception {
        installTrustAllSslContext();

        Class<?> factoryType = Class.forName(MailServiceImpl.class.getName() + "$DeadlineSocketFactory");
        var constructor = factoryType.getDeclaredConstructor(
                long.class, SocketFactory.class, boolean.class, int.class, String.class, int.class, boolean.class);
        constructor.setAccessible(true);

        int port = greenMailSmtps.getSmtps().getPort();
        // fallbackToPlainSocket=false、implicitSsl=true，與生產程式碼中 smtps 強制關閉明文退回的行為一致
        SocketFactory factory = (SocketFactory) constructor.newInstance(
                System.nanoTime() + TimeUnit.SECONDS.toNanos(10),
                SSLSocketFactory.getDefault(),
                false,
                2000,
                "127.0.0.1",
                port,
                true);

        assertThatThrownBy(() -> factory.createSocket("127.0.0.1", port))
                .isInstanceOf(IOException.class)
                .satisfies(e -> assertThat(rootCause(e)).isInstanceOf(CertificateException.class));
    }

    /** 沿 cause 鏈往下找出根本原因，用於核實委派失敗確實源自憑證／主機名稱驗證，而非其他協定錯誤。 */
    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    /** 建立一個「可連線但永不回應／永不完成交握」的本機伺服器，模擬 smtps 端點卡在連線或交握階段。 */
    private int startBlackHoleServer() throws IOException {
        blackHole = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        int port = blackHole.getLocalPort();
        Thread acceptor = new Thread(() -> {
            while (!blackHole.isClosed()) {
                try {
                    Socket socket = blackHole.accept();
                    // 刻意不寫回任何位元組，讓客戶端的 SSL handshake 等待至 connection-timeout
                    socket.getClass();
                } catch (IOException ignored) {
                    // ServerSocket 關閉時 accept() 拋出例外屬正常結束
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        return port;
    }

    /** 測試專用：安裝一個信任所有憑證的 SSLContext 作為 JVM 預設值，僅用於連線 GreenMail 內建自簽憑證。 */
    private void installTrustAllSslContext() throws Exception {
        previousDefaultSslContext = SSLContext.getDefault();
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        SSLContext.setDefault(sslContext);
    }

    private static MailServerProperty smtpsServer(String name, String host, int port) {
        MailServerProperty server = new MailServerProperty();
        server.setName(name);
        server.setHost(host);
        server.setPort(String.valueOf(port));
        server.setSmtpAuthEnable(false);
        server.setTransportProtocol("smtps");
        return server;
    }
}
