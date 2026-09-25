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
import com.zipe.exception.MailFailoverException;
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
     * 改用「同一個」{@link MailServiceImpl} 實例發起兩個執行緒（review 節點確認的既有缺口：
     * 舊版測試分別建立 serviceA、serviceB 兩個獨立實例，核准反例「deadline 存在服務欄位」
     * 即使成立，兩個實例的欄位仍彼此獨立，該測試結構本身就無法攔截這種弱實作）：
     * <ul>
     *   <li>執行緒 A 先以共用實例呼叫發送。候選為「先黑洞、後可用備援」，呼叫當下讀到的
     *       overall-timeout 為較大的 8000ms，預期經一次黑洞讀取逾時（1500ms）後切換至備援成功送達。</li>
     *   <li>測試主執行緒以黑洞伺服器 accept() 事件（而非固定 {@code sleep}，review 節點確認的既有缺口：
     *       固定睡眠無法證明兩次呼叫確實重疊）作為同步訊號：等到觀察到 A 實際連上黑洞伺服器
     *       （代表 A 已於呼叫當下讀取區域變數 overall-timeout=8000ms，且正卡在黑洞讀取階段，尚未進入
     *       第二組候選），才修改「同一份」共用 {@link MailPropertyConfig} 的
     *       {@code failover.overallTimeout} 為極短的 50ms，再以「同一個」服務實例發起執行緒 B。
     *       黑洞候選的讀逾時特意設得較寬（1500ms），使 accept() 事件的背景執行緒排程延遲
     *       （實測可達數百毫秒）不會侵蝕 A 仍卡在第一組候選、B 得以完成一次嘗試的重疊視窗。
     *       B 因自身極短預算，經一次黑洞逾時後預算即耗盡，無法再嘗試備援，預期以
     *       {@link MailFailoverException} 失敗，且失敗原因須明確標示為整體逾時（overall-timeout）
     *       ——而非任意 {@code RuntimeException}（review 節點確認的既有缺口：舊版測試只斷言
     *       例外不為 null，其他錯誤也能冒充整體逾時）。</li>
     * </ul>
     * 若截止時間正確地在每次呼叫內以區域變數保存（而非存在服務欄位等共享可變狀態），
     * A 應完全依照其呼叫當下讀到的 8000ms 預算完成切換並成功送達，不受 B 之後才寫入同一份
     * 設定物件的極短逾時影響；若截止時間以服務欄位保存並在兩次呼叫間共用，B 覆寫欄位後，
     * A 於下一輪迴圈檢查剩餘時間時會誤讀到 B 的極短截止而提前中止，導致 A 應成功卻改為失敗
     * ——此為本測試實際驗證的分辨點（已以人為注入此弱實作反例手動驗證：注入後本測試確實
     * 失敗，還原後恢復通過，證實本測試具備攔截能力）。
     */
    @Test
    void overallTimeoutDeadline_isolatedAcrossConcurrentCallsOnSharedInstance_laterShortCallDoesNotStarveEarlierCall()
            throws Exception {
        java.net.ServerSocket blackHole =
                new java.net.ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"));
        try {
            // 單一共用設定與單一共用服務實例；A、B 兩個執行緒皆呼叫同一個 service。
            // hanging 候選的讀逾時特意設得較寬（1500ms），確保 A 連上黑洞伺服器後，主執行緒
            // 有充分餘裕觀察到這次連線（背景 accept() 執行緒在測試環境下可能有數百毫秒排程延遲）、
            // 修改共用設定並讓 B 完成一次嘗試，皆發生在 A 仍卡在第一組候選讀取階段的期間內；
            // B 本身的 50ms 整體逾時不受此讀逾時影響（future.get 的逾時遠早於 socket 層讀逾時觸發）。
            MailPropertyConfig cfg = config(
                    hangingServer("hanging", blackHole.getLocalPort()),
                    greenMailServer("backup", greenMail.getSmtp().getPort(), "backupUser", "backupPw"));
            cfg.setReadTimeout(1500);
            cfg.setConnectionTimeout(1500);
            cfg.getFailover().setOverallTimeout(8000L);
            MailServiceImpl service = new MailServiceImpl(cfg);

            // setInitData() 的 testConnection() 本身即會連線黑洞一次；以累計計數區分「初始化階段
            // 的連線」與「A 實際發送階段的連線」，而非只等第一次 accept()（那會是初始化的連線）。
            java.util.concurrent.atomic.AtomicInteger acceptCount = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger baseline = new java.util.concurrent.atomic.AtomicInteger(-1);
            CountDownLatch afterBaselineAccepted = new CountDownLatch(1);
            startBlackHoleAcceptor(blackHole, () -> {
                int c = acceptCount.incrementAndGet();
                int base = baseline.get();
                if (base >= 0 && c > base) {
                    afterBaselineAccepted.countDown();
                }
            });
            try {
                service.setInitData();
            } catch (jakarta.mail.MessagingException ignored) {
                // 黑洞候選於初始化階段亦會逾時失敗，仍保留候選清單供後續發送嘗試。
            }
            // 初始化完成後才設定基準值：此刻尚無任何執行緒在連線黑洞伺服器，故不會與 accept
            // callback 的基準比較發生競態；之後第一次超過基準的 accept()，即代表是 A 的連線。
            baseline.set(acceptCount.get());

            java.util.concurrent.atomic.AtomicReference<RuntimeException> failureA =
                    new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<RuntimeException> failureB =
                    new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicLong elapsedA = new java.util.concurrent.atomic.AtomicLong();

            Thread threadA = new Thread(() -> {
                long start = System.nanoTime();
                try {
                    service.simpleMailSend(plainTextMail("SC-039-A"));
                } catch (RuntimeException e) {
                    failureA.set(e);
                }
                elapsedA.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            });
            threadA.start();

            // 以 A 實際連上黑洞伺服器（累計 accept() 次數超過初始化階段已發生的次數）作為同步訊號，
            // 取代固定 sleep：這證明 A 已在其呼叫當下讀取（區域變數）overall-timeout=8000ms，
            // 且正卡在黑洞讀取階段（尚未切換至第二組候選），此時修改共用設定才具備可觀察的重疊證據，
            // 而非猜測時間窗。
            assertThat(afterBaselineAccepted.await(5, TimeUnit.SECONDS))
                    .as("應能觀察到 A 已連上黑洞伺服器，證明兩次呼叫確實重疊而非依賴猜測的睡眠時間")
                    .isTrue();

            // 直接修改「同一份」共用設定物件（非另建新設定），模擬若容錯機制把上次讀到的截止時間
            // 存在服務欄位、被下一次呼叫覆寫的情境；B 與 A 使用同一個 service 實例。此時 A 仍卡在
            // 黑洞讀取階段（讀逾時為 1500ms，尚未到期），保證這次修改發生在 A 進入第二組候選之前。
            cfg.getFailover().setOverallTimeout(50L);
            Thread threadB = new Thread(() -> {
                try {
                    service.simpleMailSend(plainTextMail("SC-039-B"));
                } catch (RuntimeException e) {
                    failureB.set(e);
                }
            });
            threadB.start();

            threadA.join(8000);
            threadB.join(5000);

            // B 自身預算僅 50ms，經一次黑洞逾時後即耗盡，不足以再嘗試備援，應以彙整例外失敗，
            // 且失敗原因須可辨識為整體逾時（overall-timeout），而非任意 RuntimeException 冒充。
            assertThat(failureB.get())
                    .as("B 應因自身極短的 50ms 整體逾時而失敗，且型別須為容錯彙整例外")
                    .isInstanceOf(MailFailoverException.class);
            assertThat(failureB.get().getMessage())
                    .as("B 的失敗原因須明確標示為整體逾時（overall-timeout），可與伺服器回應錯誤區分")
                    .contains("overall-timeout")
                    .contains("已嘗試 1 組");

            // A 應完全依照呼叫當下讀到的 8000ms 預算完成一次黑洞逾時（約 1500ms）後切換至備援成功送達，
            // 不受 B 之後才寫入同一份設定物件的極短逾時影響。
            assertThat(failureA.get()).as("A 不應被 B 之後才變更的共用設定值提前中止而失敗").isNull();
            assertThat(elapsedA.get())
                    .as("A 應實際經歷一次黑洞讀取逾時（約 1500ms）才切換至備援，證明真的走過容錯路徑")
                    .isBetween(1200L, 6000L);

            MimeMessage[] messages = greenMail.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(subjectOf(messages[0])).isEqualTo("SC-039-A");
            assertThat(GreenMailUtil.getBody(messages[0])).isEqualTo("body-SC-039-A");
        } finally {
            blackHole.close();
        }
    }

    private static void startBlackHoleAcceptor(java.net.ServerSocket blackHole, Runnable onAccept) {
        // 保留每個已接受連線的強參考：若不保留，accept() 回傳的 Socket 在無任何參考時可能被
        // GC 提前回收並關閉底層連線，導致客戶端提早收到連線中斷（而非真正等到讀逾時），
        // 使「黑洞卡住直到逾時」的測試前提失真、耗時變得不穩定。
        java.util.List<java.net.Socket> acceptedSockets = new java.util.concurrent.CopyOnWriteArrayList<>();
        Thread acceptor = new Thread(() -> {
            while (!blackHole.isClosed()) {
                try {
                    acceptedSockets.add(blackHole.accept());
                    onAccept.run();
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
