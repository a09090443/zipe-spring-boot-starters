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
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * 多執行緒併發發送情境測試，對應情境測試計畫 SC-20。
 *
 * <p>對應 REQ-MAIL-FAILOVER-011：容錯切換機制不得因共享可變狀態而導致錯用他人的 SMTP 設定、
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

    /** SC-20：8 執行緒併發呼叫，第一組不可用環境下，全部郵件皆由備援組正確送達，無遺漏或重複。 */
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
     * SC-039：某執行緒觸發整體逾時截止時，不得耗用或影響另一執行緒的嘗試預算與結果。
     * 恢復核准測試計畫的「成功／失敗交叉」情境（review 節點確認的既有缺口：舊版測試讓
     * 兩個呼叫都走同一黑洞、只各自比對耗時下限，未驗證另一執行緒仍可由備援組成功送達，
     * 若共享截止時間被延後仍可能通過）：
     * <ul>
     *   <li>執行緒 A 使用獨立的 {@link MailServiceImpl} 實例，僅有一組永遠不回應的候選，
     *       overall-timeout 設為較短的 500ms，依自身預算截止並以 {@code MailFailoverException} 失敗。</li>
     *   <li>執行緒 B 使用另一個獨立實例，候選為「先黑洞、後可用備援」，overall-timeout 設為
     *       遠大於 A 的 5000ms，於連線逾時後改用備援並實際成功送達。</li>
     * </ul>
     * 兩者同時執行，B 的成功送達與其耗時（明顯長於 A 的 500ms 上限）證明 B 的截止時間
     * 未被 A 的較短預算污染或提前截斷；若截止時間以共享靜態欄位保存，B 極可能被錯誤地
     * 提前中止而收不到郵件，或 A 反而錯誤沿用 B 較長的截止時間而遲遲不失敗。
     */
    @Test
    void overallTimeoutDeadline_isolatedAcrossConcurrentCalls_shortFailureDoesNotAffectConcurrentBackupSuccess()
            throws Exception {
        java.net.ServerSocket blackHoleA =
                new java.net.ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"));
        java.net.ServerSocket blackHoleB =
                new java.net.ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"));
        try {
            startBlackHoleAcceptor(blackHoleA);
            startBlackHoleAcceptor(blackHoleB);

            // 執行緒 A：獨立實例，僅有黑洞候選、overall-timeout 短（500ms），必然依自身預算失敗。
            MailPropertyConfig cfgA = config(hangingServer("hanging-a", blackHoleA.getLocalPort()));
            cfgA.setReadTimeout(5000);
            cfgA.setConnectionTimeout(5000);
            cfgA.getFailover().setOverallTimeout(500L);
            MailServiceImpl serviceA = new MailServiceImpl(cfgA);
            try {
                serviceA.setInitData();
            } catch (jakarta.mail.MessagingException ignored) {
                // 黑洞候選於初始化階段亦會逾時失敗，仍保留候選清單供後續發送嘗試。
            }

            // 執行緒 B：獨立實例，候選為「先黑洞、後可用備援」，overall-timeout 遠大於 A（5000ms）。
            // 黑洞伺服器會接受連線但不回應問候語，實際卡住的階段是等待伺服器問候的讀取逾時
            // （而非連線建立本身），故 read-timeout 與 connection-timeout 皆設為短值（600ms），
            // 使其能在整體預算內快速完成一次黑洞逾時後切換至備援成功送達。
            MailPropertyConfig cfgB = config(
                    hangingServer("hanging-b", blackHoleB.getLocalPort()),
                    greenMailServer("backup-b", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
            cfgB.setReadTimeout(600);
            cfgB.setConnectionTimeout(600);
            cfgB.getFailover().setOverallTimeout(5000L);
            MailServiceImpl serviceB = new MailServiceImpl(cfgB);
            try {
                serviceB.setInitData();
            } catch (jakarta.mail.MessagingException ignored) {
                // 黑洞候選於初始化階段亦會逾時失敗，仍保留候選清單供後續發送嘗試。
            }

            java.util.concurrent.atomic.AtomicLong elapsedA = new java.util.concurrent.atomic.AtomicLong();
            java.util.concurrent.atomic.AtomicLong elapsedB = new java.util.concurrent.atomic.AtomicLong();
            java.util.concurrent.atomic.AtomicReference<RuntimeException> failureA =
                    new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<RuntimeException> failureB =
                    new java.util.concurrent.atomic.AtomicReference<>();

            Thread threadA = new Thread(() -> {
                long start = System.nanoTime();
                try {
                    serviceA.simpleMailSend(plainTextMail("SC-039-A"));
                } catch (RuntimeException e) {
                    failureA.set(e);
                }
                elapsedA.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            });
            Thread threadB = new Thread(() -> {
                long start = System.nanoTime();
                try {
                    serviceB.simpleMailSend(plainTextMail("SC-039-B"));
                } catch (RuntimeException e) {
                    failureB.set(e);
                }
                elapsedB.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            });

            threadA.start();
            threadB.start();
            threadA.join(3000);
            threadB.join(6000);

            // A 依自身 500ms 預算失敗（給予寬容上限，避免測試環境時間抖動誤判）。
            assertThat(elapsedA.get()).as("A 應接近其自身 500ms 的整體逾時上限").isBetween(400L, 2000L);
            assertThat(failureA.get()).as("A 全部候選（僅黑洞）皆失敗，應拋出彙整例外").isNotNull();

            // B 未受 A 較短截止時間影響，仍完整經歷一次 connection-timeout 才切換至備援，並實際成功送達。
            assertThat(failureB.get()).as("B 應由備援組成功送達，不應拋出例外").isNull();
            assertThat(elapsedB.get())
                    .as("B 的耗時應反映自身 600ms 的 connection-timeout，未被 A 的 500ms 截斷")
                    .isGreaterThanOrEqualTo(500L);
            MimeMessage[] messages = greenMail.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(subjectOf(messages[0])).isEqualTo("SC-039-B");
            assertThat(GreenMailUtil.getBody(messages[0])).isEqualTo("body-SC-039-B");
        } finally {
            blackHoleA.close();
            blackHoleB.close();
        }
    }

    private static void startBlackHoleAcceptor(java.net.ServerSocket blackHole) {
        Thread acceptor = new Thread(() -> {
            while (!blackHole.isClosed()) {
                try {
                    blackHole.accept();
                } catch (java.io.IOException ignored) {
                    // ServerSocket 關閉時 accept() 拋出例外屬正常結束
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private static com.zipe.config.MailServerProperty hangingServer(String name, int port) {
        com.zipe.config.MailServerProperty hanging = new com.zipe.config.MailServerProperty();
        hanging.setName(name);
        hanging.setHost("127.0.0.1");
        hanging.setPort(String.valueOf(port));
        hanging.setSmtpAuthEnable(false);
        return hanging;
    }

    private String subjectOf(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
