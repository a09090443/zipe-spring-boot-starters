package com.zipe.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.model.Mail;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    /**
     * 連接埠設定為非數字字串的伺服器組，用於模擬 {@code buildSender} 階段
     * （寄件器建立本身，而非連線測試）就失敗的設定錯誤情境。
     */
    static MailServerProperty invalidPortServer(String name) {
        MailServerProperty server = server(name, "127.0.0.1", 1, "user", "pass");
        server.setPort("not-a-port");
        return server;
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

    /**
     * 只用於計數「是否曾被連線」的最小 TCP 伺服器：接受連線後立即計數並關閉，
     * 供驗證「候選清單中某組不應被嘗試」（連線數須為 0）的情境使用。
     */
    static final class CountingTcpServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final AtomicInteger connectionCount = new AtomicInteger();

        CountingTcpServer() throws IOException {
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            Thread acceptor = new Thread(this::acceptLoop, "counting-tcp-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        int connectionCount() {
            return connectionCount.get();
        }

        /**
         * 重設連線計數，供「先完成 setInitData()（依 REQ-007 會逐一測試每組連線），
         * 再只針對後續實際發送階段計數」的情境使用，避免把初始化階段的連線測試
         * 誤判為發送階段不應發生的連線。
         */
        void resetCount() {
            connectionCount.set(0);
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    connectionCount.incrementAndGet();
                } catch (IOException ignored) {
                    // close() 會關閉 ServerSocket，使 accept() 正常結束。
                }
            }
        }

        /** 給測試一小段寬限時間讓「不應發生」的連線有機會發生，呼叫端隨後應自行斷言連線數為 0。 */
        void waitBriefly(long millis) throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(millis);
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }

    /**
     * 可完整轉送 SMTP 對話並計數連線的本機 TCP proxy。與 {@link CountingTcpServer} 不同，
     * 此工具後方連接真正可投遞的 SMTP，供「候選可用但成功後不得被連線」的反例驗證。
     */
    static final class CountingSmtpProxy implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final String upstreamHost;
        private final int upstreamPort;
        private final AtomicInteger connectionCount = new AtomicInteger();
        private final ExecutorService executor = Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "counting-smtp-proxy");
            thread.setDaemon(true);
            return thread;
        });

        CountingSmtpProxy(String upstreamHost, int upstreamPort) throws IOException {
            this.upstreamHost = upstreamHost;
            this.upstreamPort = upstreamPort;
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            executor.submit(this::acceptLoop);
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        int connectionCount() {
            return connectionCount.get();
        }

        void resetCount() {
            connectionCount.set(0);
        }

        void waitBriefly(long millis) throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(millis);
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    connectionCount.incrementAndGet();
                    executor.submit(() -> forward(client));
                } catch (IOException ignored) {
                    // close() 會關閉 ServerSocket，使 accept() 正常結束。
                }
            }
        }

        private void forward(Socket client) {
            try (client; Socket upstream = new Socket(upstreamHost, upstreamPort)) {
                Future<?> clientToUpstream = executor.submit(() -> pump(client, upstream));
                Future<?> upstreamToClient = executor.submit(() -> pump(upstream, client));
                clientToUpstream.get();
                upstreamToClient.get();
            } catch (Exception ignored) {
                // 測試關閉連線或 SMTP 對話結束時，任一方向先結束皆屬正常清理。
            }
        }

        private static void pump(Socket source, Socket target) {
            try {
                InputStream input = source.getInputStream();
                OutputStream output = target.getOutputStream();
                input.transferTo(output);
                output.flush();
                target.shutdownOutput();
            } catch (IOException ignored) {
                // 另一方向關閉 socket 時，阻塞中的轉送會在此正常結束。
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            executor.shutdownNow();
        }
    }
}
