package com.zipe.service.impl;

import static com.zipe.service.impl.MailFailoverTestSupport.config;
import static com.zipe.service.impl.MailFailoverTestSupport.greenMailServer;
import static com.zipe.service.impl.MailFailoverTestSupport.plainTextMail;
import static com.zipe.service.impl.MailFailoverTestSupport.unreachableServer;
import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.zipe.config.MailPropertyConfig;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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

        List<String> subjects =
                List.of(messages).stream().map(this::subjectOf).collect(Collectors.toList());
        List<String> expected = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> "SC-20-thread-" + i)
                .collect(Collectors.toList());
        assertThat(subjects).containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * SC-039：某執行緒的整體逾時截止不得以共享狀態（例如靜態或實例欄位）洩漏給其他執行緒，
     * 每次呼叫的截止時間須各自獨立計算。以「稍晚才開始呼叫」的第二執行緒仍需完整經歷一次
     * overall-timeout 才失敗（而非因誤用前一次呼叫已過期的截止時間而立即失敗）佐證。
     */
    @Test
    void overallTimeoutDeadline_isNotSharedAcrossConcurrentCalls() throws Exception {
        java.net.ServerSocket blackHole = new java.net.ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"));
        try {
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

            com.zipe.config.MailServerProperty hanging = new com.zipe.config.MailServerProperty();
            hanging.setName("hanging");
            hanging.setHost("127.0.0.1");
            hanging.setPort(String.valueOf(blackHole.getLocalPort()));
            hanging.setSmtpAuthEnable(false);

            MailPropertyConfig cfg = config(hanging);
            cfg.setReadTimeout(5000);
            cfg.setConnectionTimeout(5000);
            cfg.getFailover().setOverallTimeout(500L);
            MailServiceImpl service = new MailServiceImpl(cfg);
            try {
                service.setInitData();
            } catch (jakarta.mail.MessagingException ignored) {
                // 黑洞候選於初始化階段亦會逾時失敗，仍保留候選清單供後續發送嘗試。
            }

            java.util.concurrent.atomic.AtomicLong firstElapsed = new java.util.concurrent.atomic.AtomicLong();
            java.util.concurrent.atomic.AtomicLong secondElapsed = new java.util.concurrent.atomic.AtomicLong();

            Thread first = new Thread(() -> {
                long start = System.nanoTime();
                try {
                    service.simpleMailSend(plainTextMail("SC-039-first"));
                } catch (RuntimeException ignored) {
                    // 預期以 MailFailoverException 結束
                }
                firstElapsed.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            });
            first.start();
            // 確保第二次呼叫在第一次呼叫「進行中」才開始，藉此驗證兩者截止時間互不干擾。
            Thread.sleep(200);
            Thread second = new Thread(() -> {
                long start = System.nanoTime();
                try {
                    service.simpleMailSend(plainTextMail("SC-039-second"));
                } catch (RuntimeException ignored) {
                    // 預期以 MailFailoverException 結束
                }
                secondElapsed.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            });
            second.start();

            first.join(3000);
            second.join(3000);

            // 若截止時間被共享（例如以靜態欄位保存第一次呼叫的絕對截止時間），較晚開始的第二次呼叫
            // 會遠早於自身的 500ms 上限即結束；正確實作下，第二次呼叫仍須耗時接近其自身的 500ms 上限。
            assertThat(secondElapsed.get()).isGreaterThanOrEqualTo(400);
            assertThat(firstElapsed.get()).isGreaterThanOrEqualTo(400);
        } finally {
            blackHole.close();
        }
    }

    private String subjectOf(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
