package com.zipe.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 明文 SMTP（非 TLS）協定層情境測試共用基礎設施，補齊 {@link MailFailoverTlsTestSupport}
 * 只涵蓋 smtps 的缺口：驗證讀取黑洞、535 認證失敗、DATA 階段拒收等「非連線失敗」類型的
 * SMTP 錯誤仍會被容錯機制視為該組失敗並切換下一組（情境測試計畫 SC-009、SC-010、SC-011）。
 */
final class MailFailoverPlainProtocolTestSupport {

    private MailFailoverPlainProtocolTestSupport() {}

    /** 逐連線行為可客製化的最小明文 SMTP 伺服器。 */
    static final class ScriptablePlainSmtpServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final ConnectionHandler handler;
        private final List<Socket> clients = new CopyOnWriteArrayList<>();
        private final AtomicInteger connectionCount = new AtomicInteger();

        ScriptablePlainSmtpServer(ConnectionHandler handler) throws IOException {
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            this.handler = handler;
            Thread acceptor = new Thread(this::acceptLoop, "scriptable-plain-smtp-acceptor");
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
                    Socket socket = serverSocket.accept();
                    clients.add(socket);
                    connectionCount.incrementAndGet();
                    Thread client = new Thread(() -> serve(socket), "scriptable-plain-smtp-client");
                    client.setDaemon(true);
                    client.start();
                } catch (IOException ignored) {
                    // close() 會關閉 ServerSocket，使 accept() 正常結束。
                }
            }
        }

        private void serve(Socket socket) {
            try (socket;
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    BufferedWriter writer =
                            new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII))) {
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
