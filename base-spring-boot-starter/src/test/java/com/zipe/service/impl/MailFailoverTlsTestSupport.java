package com.zipe.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

/**
 * REQ-MAIL-FAILOVER-020／SC-024～026、SC-028、SC-029 共用的 smtps 測試基礎設施。
 *
 * <p>以 JDK 內建 {@code keytool}（不下載任何相依、不連外）在測試執行時產生一張
 * 含 {@code SAN=dns:localhost} 的自簽憑證，供本機 {@link SSLServerSocket} 端點使用。
 * 與既有 {@code MailServiceSmtpsTimeoutTest} 使用的 GreenMail 內建憑證不同——GreenMail
 * 憑證無 SAN、CN 非主機名稱格式，結構上無法通過主機名稱驗證，因此無法用於驗證
 * 「合法憑證＋正確主機名稱時應成功送達」的正面案例；本類別產生的憑證以
 * {@code localhost} 為主機名稱，可與生產程式碼 {@code DeadlineSocketFactory} 的標準
 * HTTPS 主機名稱驗證規則相符。</p>
 */
final class MailFailoverTlsTestSupport {

    static final String CERT_HOST = "localhost";

    private MailFailoverTlsTestSupport() {}

    /** 產生自簽憑證並回傳可分別用於伺服器端與客戶端（信任錨）的 {@link SSLContext}。 */
    static TlsFixture generateSelfSignedFixture(Path tempDir) throws Exception {
        Path keystorePath = tempDir.resolve("smtps-test.p12");
        String password = "changeit";
        String keytool = System.getProperty("java.home") + java.io.File.separator + "bin" + java.io.File.separator + "keytool";

        List<String> command = List.of(
                keytool,
                "-genkeypair",
                "-alias",
                "smtps-test",
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "3650",
                "-storetype",
                "PKCS12",
                "-keystore",
                keystorePath.toString(),
                "-storepass",
                password,
                "-keypass",
                password,
                "-dname",
                "CN=" + CERT_HOST + ", OU=test, O=test, L=test, ST=test, C=TW",
                "-ext",
                "SAN=dns:" + CERT_HOST + ",ip:127.0.0.1");

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            throw new IllegalStateException(
                    "keytool 於離線測試環境產生自簽憑證失敗（exitFinished=" + finished + "）：" + output);
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keystorePath)) {
            keyStore.load(in, password.toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password.toCharArray());
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        // 自簽憑證本身即作為信任錨：keystore 內只有這一組公私鑰對，可直接當作 truststore 使用。
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        SSLContext clientContext = SSLContext.getInstance("TLS");
        clientContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

        return new TlsFixture(serverContext, clientContext);
    }

    record TlsFixture(SSLContext serverContext, SSLContext clientContext) {}

    /** 逐連線行為可客製化的最小 SMTP-over-TLS 伺服器，供各 smtps 情境測試模擬不同協定階段的行為。 */
    static final class ScriptableTlsSmtpServer implements AutoCloseable {

        private final SSLServerSocket serverSocket;
        private final ConnectionHandler handler;
        private final List<Socket> clients = new CopyOnWriteArrayList<>();
        private final AtomicInteger connectionCount = new AtomicInteger();

        ScriptableTlsSmtpServer(SSLContext serverContext, ConnectionHandler handler) throws IOException {
            SSLServerSocketFactory factory = serverContext.getServerSocketFactory();
            this.serverSocket = (SSLServerSocket) factory.createServerSocket(0, 50, InetAddress.getByName(CERT_HOST));
            this.handler = handler;
            Thread acceptor = new Thread(this::acceptLoop, "scriptable-tls-smtp-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        int connectionCount() {
            return connectionCount.get();
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();
                    clients.add(socket);
                    connectionCount.incrementAndGet();
                    Thread client = new Thread(() -> serve(socket), "scriptable-tls-smtp-client");
                    client.setDaemon(true);
                    client.start();
                } catch (IOException ignored) {
                    // close() 會關閉 ServerSocket，使 accept() 正常結束。
                }
            }
        }

        private void serve(SSLSocket socket) {
            try (socket;
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    BufferedWriter writer =
                            new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII))) {
                socket.startHandshake();
                handler.handle(reader, writer);
            } catch (IOException ignored) {
                // 客戶端逾時取消或測試結束關閉連線時的正常結束路徑。
            } finally {
                clients.remove(socket);
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            for (Socket socket : clients) {
                socket.close();
            }
        }

        static void reply(BufferedWriter writer, String response) throws IOException {
            writer.write(response);
            writer.write("\r\n");
            writer.flush();
        }

        @FunctionalInterface
        interface ConnectionHandler {
            void handle(BufferedReader reader, BufferedWriter writer) throws IOException;
        }
    }
}
