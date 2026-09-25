package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * 多執行緒併發發送情境測試，對應情境測試計畫 SC-040、SC-041。
 *
 * <p>對應 REQ-012：容錯切換機制不得因共享可變狀態而導致錯用他人的 SMTP 設定、
 * 狀態互相覆寫或產生資料競爭。</p>
 */
class MailServiceConcurrencyTest {

    private static final int THREAD_COUNT = 8;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort())
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("backupUser", "backupPw"));

    @BeforeEach
    void purgeMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    /** SC-040：8 執行緒併發呼叫，第一組不可用環境下，全部郵件皆由備援組正確送達，無遺漏或重複。 */
    @Test
    void concurrentSends_allDeliverExactlyOnceViaBackup_noSharedStateCorruption() throws Exception {
        MailPropertyConfig cfg = config(
                unreachableServer("primary"),
                greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
        MailServiceImpl service = new MailServiceImpl(cfg);
        service.setInitData();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        service.simpleMailSend(plainTextMail("SC-20-thread-" + index));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdown();
        }

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(THREAD_COUNT);

        // 逐封比對「主旨↔內文」配對，而非只比對主旨集合：主旨正確但內文被其他執行緒覆寫
        // （例如共用可變 MimeMessageHelper／MimeMessage 造成的資料競爭）的弱實作，
        // 僅比對主旨集合仍可能通過，須逐封核對內文才能攔截。
        Map<String, String> subjectToBody = new HashMap<>();
        for (MimeMessage message : messages) {
            subjectToBody.put(subjectOf(message), GreenMailUtil.getBody(message));
        }
        assertThat(subjectToBody).hasSize(THREAD_COUNT);

        IntStream.range(0, THREAD_COUNT).forEach(i -> {
            String subject = "SC-20-thread-" + i;
            assertThat(subjectToBody)
                    .as("每封郵件的主旨與內文須配對一致，不因併發而交錯")
                    .containsEntry(subject, "body-" + subject);
        });
    }

    /**
     * SC-041：同一個 {@link MailServiceImpl} 實例上，長預算呼叫 A 與短預算呼叫 B 必須確實重疊，
     * 且兩者的整體截止時間彼此隔離。以受控閘門伺服器建立雙向同步，不依賴任何睡眠或時間窗：
     * <ol>
     *   <li>閘門關閉期間，所有連入的連線都被扣住不回應也不關閉；A 的第一組嘗試連入後即卡在閘門內
     *       （A-entered 事件）。此時 A 已於呼叫開頭以區域變數讀取 overall-timeout=20000ms。</li>
     *   <li>確認 A 已進入後，才把同一份共用設定的 overall-timeout 改為 2000ms 並以同一實例發起 B；
     *       B 的第一組嘗試同樣連入閘門（B-entered 事件，代表 B 已建立自身截止時間並開始嘗試）。</li>
     *   <li>等待 B 因自身預算整體逾時而完全結束，並確認此時 A 仍被扣在閘門內（執行緒存活、尚無結果），
     *       才打開閘門釋放 A。</li>
     * </ol>
     * 閘門保證「B 寫入截止狀態」必定發生在「A 離開受控階段、進入下一輪嘗試」之前。若截止時間存於服務
     * 欄位等共享狀態，A 下一輪檢查剩餘預算時必定讀到 B 已過期的截止時間而失敗；正確實作下 A 依自身
     * 20000ms 預算切換至備援並成功送達。
     */
    @Test
    void overallTimeoutDeadline_isolatedAcrossConcurrentCallsOnSharedInstance_laterShortCallDoesNotStarveEarlierCall()
            throws Exception {
        try (GatedServer gate = new GatedServer()) {
            MailPropertyConfig cfg = config(
                    gatedServer("gated", gate.port()),
                    greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
            // 單項逾時遠長於測試流程，確保 A 只會因閘門釋放而離開第一組，而非自身逾時。
            cfg.setReadTimeout(20000);
            cfg.setConnectionTimeout(20000);
            cfg.setWriteTimeout(20000);
            cfg.getFailover().setOverallTimeout(20000L);
            MailServiceImpl service = new MailServiceImpl(cfg);
            // 初始化時閘門為開啟狀態：連線立即被關閉，第一組初始化快速失敗，備援組初始化成功。
            service.setInitData();
            gate.hold();

            AtomicReference<Throwable> failureA = new AtomicReference<>();
            AtomicReference<Throwable> failureB = new AtomicReference<>();
            Thread threadA = new Thread(() -> {
                try {
                    service.simpleMailSend(plainTextMail("SC-041-A"));
                } catch (Throwable e) {
                    failureA.set(e);
                }
            }, "sc041-A");
            threadA.start();
            assertThat(gate.awaitHeld(1, 10, TimeUnit.SECONDS))
                    .as("A-entered：A 的第一組嘗試須已進入閘門")
                    .isTrue();

            cfg.getFailover().setOverallTimeout(2000L);
            Thread threadB = new Thread(() -> {
                try {
                    service.simpleMailSend(plainTextMail("SC-041-B"));
                } catch (Throwable e) {
                    failureB.set(e);
                }
            }, "sc041-B");
            threadB.start();
            assertThat(gate.awaitHeld(2, 10, TimeUnit.SECONDS))
                    .as("B-entered：B 的第一組嘗試須已進入閘門，證明兩次呼叫確實重疊")
                    .isTrue();

            threadB.join(10000);
            assertThat(threadB.isAlive()).as("B 須依自身 2000ms 預算結束").isFalse();
            assertThat(failureB.get())
                    .as("B 應因自身整體逾時而以容錯彙整例外失敗")
                    .isInstanceOf(MailFailoverException.class);
            assertThat(failureB.get().getMessage())
                    .contains("gated")
                    .contains("整體逾時（overall-timeout）已到期")
                    .contains("已嘗試 1 組");

            // 反向閘門：B 已建立並耗盡自身截止時間，此時 A 必須仍被扣在第一組嘗試內。
            assertThat(threadA.isAlive()).as("釋放前 A 必須仍停留在受控階段").isTrue();
            assertThat(failureA.get()).isNull();
            assertThat(greenMail.getReceivedMessages()).isEmpty();

            gate.release();
            threadA.join(15000);

            assertThat(threadA.isAlive()).isFalse();
            assertThat(failureA.get()).as("A 不得被 B 的短截止時間污染而失敗").isNull();
            MimeMessage[] messages = greenMail.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(subjectOf(messages[0])).isEqualTo("SC-041-A");
            assertThat(GreenMailUtil.getBody(messages[0])).isEqualTo("body-SC-041-A");
        }
    }

    private static MailServerProperty gatedServer(String name, int port) {
        MailServerProperty server = new MailServerProperty();
        server.setName(name);
        server.setHost("127.0.0.1");
        server.setPort(String.valueOf(port));
        server.setSmtpAuthEnable(false);
        return server;
    }

    private String subjectOf(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 受控閘門 TCP 伺服器：開啟狀態下連線一接受即關閉（使該組快速失敗）；
     * {@link #hold()} 後接受的連線一律扣住不回應也不關閉，並計數為「已進入」事件，
     * 直到 {@link #release()} 才全部關閉並回到開啟狀態。
     */
    private static final class GatedServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final List<Socket> held = new CopyOnWriteArrayList<>();
        private final Object lock = new Object();
        private boolean holding;
        private int heldCount;

        private GatedServer() throws IOException {
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            Thread acceptor = new Thread(this::acceptLoop, "gated-smtp-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        private int port() {
            return serverSocket.getLocalPort();
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    synchronized (lock) {
                        if (holding) {
                            held.add(socket);
                            heldCount++;
                            lock.notifyAll();
                            continue;
                        }
                    }
                    socket.close();
                } catch (IOException ignored) {
                    // close() 會關閉 ServerSocket，使 accept() 正常結束。
                }
            }
        }

        private void hold() {
            synchronized (lock) {
                holding = true;
            }
        }

        private boolean awaitHeld(int expected, long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            synchronized (lock) {
                while (heldCount < expected) {
                    long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                    if (remainingMs <= 0) {
                        return false;
                    }
                    lock.wait(remainingMs);
                }
                return true;
            }
        }

        private void release() throws IOException {
            synchronized (lock) {
                holding = false;
            }
            for (Socket socket : held) {
                socket.close();
            }
        }

        @Override
        public void close() throws IOException {
            release();
            serverSocket.close();
        }
    }
}
