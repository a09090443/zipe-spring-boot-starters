package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;

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
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
import org.slf4j.LoggerFactory;

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
     * SC-033（AC-012-01、AC-012-02）：同一個 {@link MailServiceImpl} 實例上，兩個成功呼叫 A、B 必須
     * 確實重疊，且各自從第一組開始、依序切換至備援、只投遞一次、內容與收件人不交錯。
     * <ol>
     *   <li>閘門扣住第一組連線：A、B 都必須各自連入第一組並同時停在閘門內（heldCount=2），
     *       此時備援尚未收到任何郵件，證明兩次呼叫確實重疊。</li>
     *   <li>釋放閘門後，兩者的第一組嘗試都失敗並切換至備援送達。</li>
     *   <li>以日誌事件的執行緒名稱逐呼叫核對候選順序：每個呼叫恰為「gated 失敗 → backup 成功」。</li>
     *   <li>A、B 使用不同收件人、主旨與本文，逐封核對收件人↔主旨↔本文，每位收件人恰收到一封。</li>
     * </ol>
     * 共享候選索引而使 B 直接跳至備援的弱實作，會因 heldCount 無法達到 2 或 B 缺少 gated 失敗事件而失敗；
     * 共用可變 MIME 的弱實作會因收件人與內容對應錯誤而失敗。
     */
    @Test
    void overlappingSuccessfulCalls_eachStartFromFirstCandidate_andDeliverOwnContentExactlyOnce() throws Exception {
        try (GatedServer gate = new GatedServer()) {
            MailPropertyConfig cfg = config(
                    gatedServer("gated", gate.port()),
                    greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
            // 單項逾時遠長於測試流程，確保兩個呼叫只會因閘門釋放而離開第一組。
            cfg.setReadTimeout(20000);
            cfg.setConnectionTimeout(20000);
            cfg.setWriteTimeout(20000);
            cfg.getFailover().setOverallTimeout(20000L);
            MailServiceImpl service = new MailServiceImpl(cfg);
            service.setInitData();
            int acceptedBeforeSend = gate.acceptedCount();
            gate.hold();

            ListAppender<ILoggingEvent> appender = MailFailoverTestSupport.attachLogAppender(MailServiceImpl.class);
            try {
                Map<String, Mail> mails = Map.of(
                        "sc033-A", recipientMail("SC-033-A", "alice@test.local"),
                        "sc033-B", recipientMail("SC-033-B", "bob@test.local"));
                Map<String, AtomicReference<Throwable>> failures = new HashMap<>();
                List<Thread> threads = new ArrayList<>();
                for (Map.Entry<String, Mail> entry : mails.entrySet()) {
                    AtomicReference<Throwable> failure = new AtomicReference<>();
                    failures.put(entry.getKey(), failure);
                    threads.add(new Thread(() -> {
                        try {
                            service.simpleMailSend(entry.getValue());
                        } catch (Throwable e) {
                            failure.set(e);
                        }
                    }, entry.getKey()));
                }
                threads.forEach(Thread::start);

                assertThat(gate.awaitHeld(2, 10, TimeUnit.SECONDS))
                        .as("A、B 須同時停在第一組閘門內，證明兩呼叫確實重疊且各自從第一組開始")
                        .isTrue();
                assertThat(threads).allMatch(Thread::isAlive);
                assertThat(greenMail.getReceivedMessages()).as("閘門釋放前不得有任何呼叫已由備援送達").isEmpty();

                gate.release();
                for (Thread thread : threads) {
                    thread.join(15000);
                    assertThat(thread.isAlive()).isFalse();
                }
                failures.forEach((name, failure) ->
                        assertThat(failure.get()).as("%s 應成功送達", name).isNull());
                // 給予寬限讓「不應發生」的額外第一組連線（重試或重複嘗試）有機會被接受迴圈計數。
                TimeUnit.MILLISECONDS.sleep(200);
                assertThat(gate.acceptedCount() - acceptedBeforeSend)
                        .as("發送階段第一組只有 A、B 各自被扣住的那一條連線，釋放後不再重連")
                        .isEqualTo(2);

                for (String threadName : mails.keySet()) {
                    List<String> events = appender.list.stream()
                            .filter(event -> threadName.equals(event.getThreadName()))
                            .map(ILoggingEvent::getFormattedMessage)
                            .filter(message -> message.startsWith("郵件發送"))
                            .toList();
                    assertThat(events).as("%s 的候選順序", threadName).hasSize(2);
                    assertThat(events.get(0)).startsWith("郵件發送失敗").contains("伺服器：gated(");
                    assertThat(events.get(1)).startsWith("郵件發送成功").contains("使用伺服器：backup(");
                }
            } finally {
                ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(MailServiceImpl.class))
                        .detachAppender(appender);
            }

            MimeMessage[] messages = greenMail.getReceivedMessages();
            assertThat(messages).hasSize(2);
            Map<String, MimeMessage> byRecipient = new HashMap<>();
            for (MimeMessage message : messages) {
                Address[] recipients = message.getRecipients(Message.RecipientType.TO);
                assertThat(recipients).hasSize(1);
                assertThat(byRecipient.put(recipients[0].toString(), message))
                        .as("每位收件人只能收到一封")
                        .isNull();
            }
            assertThat(byRecipient).containsOnlyKeys("alice@test.local", "bob@test.local");
            assertThat(subjectOf(byRecipient.get("alice@test.local"))).isEqualTo("SC-033-A");
            assertThat(GreenMailUtil.getBody(byRecipient.get("alice@test.local"))).isEqualTo("body-SC-033-A");
            assertThat(subjectOf(byRecipient.get("bob@test.local"))).isEqualTo("SC-033-B");
            assertThat(GreenMailUtil.getBody(byRecipient.get("bob@test.local"))).isEqualTo("body-SC-033-B");
        }
    }

    private static Mail recipientMail(String subject, String recipient) {
        Mail mail = plainTextMail(subject);
        mail.setMailTo(new String[] {recipient});
        return mail;
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
        private int acceptedCount;

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
                        acceptedCount++;
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

        /** 不論閘門狀態，累計接受過的連線數。 */
        private int acceptedCount() {
            synchronized (lock) {
                return acceptedCount;
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
