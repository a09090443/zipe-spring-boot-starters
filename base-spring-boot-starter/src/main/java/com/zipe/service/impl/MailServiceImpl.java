package com.zipe.service.impl;

import com.zipe.config.MailPropertyConfig;
import com.zipe.config.MailServerProperty;
import com.zipe.exception.MailFailoverException;
import com.zipe.model.Mail;
import com.zipe.service.MailService;
import com.zipe.util.crypto.Base64Util;
import com.zipe.util.crypto.CryptoUtil;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * 郵件服務實作類別。
 * <p>
 * 基於 Jakarta Mail（JavaMail）與 Spring Mail 實作 {@link MailService} 介面，
 * 支援純文字信件、HTML 富文字信件、附件信件及群發多收件人等發送情境。
 * 郵件伺服器連線參數由 {@link MailPropertyConfig} 提供，並在每次發送前透過
 * {@link #setInitData()} 完成 SMTP 連線初始化。
 * </p>
 * <p>
 * 自本版本起支援設定多組 SMTP 伺服器（{@link MailPropertyConfig#getServers()}），
 * 五個發送方法皆透過 {@link #executeWithFailover(String, MailSendOperation)} 依清單順序
 * （優先序 failover）逐組嘗試，任一組成功即返回；全部失敗則拋出彙整各組失敗原因的
 * {@link MailFailoverException}。此機制為「至少一次投遞」語意：若某組 SMTP 已在
 * DATA 階段接收完整郵件內容、僅是回報連線層級失敗（例如回應逾時或連線中斷），
 * 切換重送可能導致收件者收到重複郵件，此為「至少一次投遞」機制本質限制，並非缺陷；
 * 呼叫端如需避免重複處理，應自行以業務層識別碼實作冪等或去重。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/04/26 下午 14:00
 **/
@Slf4j
public class MailServiceImpl implements MailService {

    /** 共用 daemon 排程器，在整體截止時間到達時關閉進行中的 SMTP socket。 */
    private static final ScheduledExecutorService DEADLINE_SOCKET_CLOSER =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "mail-failover-socket-deadline");
                thread.setDaemon(true);
                return thread;
            });

    /** 郵件相關設定屬性，由外部注入 */
    private final MailPropertyConfig mailPropertyConfig;

    /**
     * 已初始化的候選伺服器清單（不可變），索引順序即容錯切換的嘗試優先序。
     * 以 volatile 修飾並整份替換（非逐一修改），確保多執行緒讀取不需額外同步。
     */
    private volatile List<MailServerCandidate> candidates;

    /**
     * 建構子，注入郵件設定屬性。
     *
     * @param mailPropertyConfig 郵件伺服器連線設定
     */
    public MailServiceImpl(MailPropertyConfig mailPropertyConfig) {
        this.mailPropertyConfig = mailPropertyConfig;
    }

    /**
     * 初始化郵件發送資料，依序建立並測試每一組 SMTP 伺服器連線。
     * <p>
     * 依 {@link MailPropertyConfig#resolveServers()} 取得候選清單，逐組建立
     * {@link JavaMailSenderImpl} 並呼叫 {@code testConnection()} 驗證連線。
     * 單組測試失敗僅記錄 WARN 並保留於候選清單（供後續發送時仍可再次嘗試），
     * 不會因部分伺服器不可用而中斷初始化；只有全部伺服器皆失敗時才拋出例外。
     * </p>
     * <p>
     * {@link #buildSender(MailServerProperty)} 本身（例如連接埠格式錯誤、主機無法解析等設定錯誤）
     * 與 {@code testConnection()} 失敗一併視為「該組失敗」：兩者皆只記錄 WARN 並繼續處理下一組，
     * 不會中止整個迴圈而遺失其餘尚未處理的正常候選。寄件器建立失敗的組別因無可用的
     * {@link JavaMailSenderImpl} 可保留，不會加入候選清單，但仍計入全部失敗的判定。
     * </p>
     *
     * @throws MessagingException 當全部 SMTP 伺服器皆初始化失敗（含建立寄件器失敗與連線測試失敗）時拋出
     */
    @Override
    public void setInitData() throws MessagingException {
        List<MailServerProperty> servers = mailPropertyConfig.resolveServers();
        List<MailServerCandidate> initialized = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (MailServerProperty server : servers) {
            String label = label(server);
            try {
                JavaMailSenderImpl sender = buildSender(server);
                try {
                    sender.testConnection();
                    log.info("初始化郵件伺服器成功：{}", label);
                } catch (Exception e) {
                    String reason = safeReason(e);
                    failures.add(label + " - " + reason);
                    log.warn("初始化郵件伺服器失敗，將保留於後續嘗試清單：{}，原因：{}", label, reason);
                }
                initialized.add(new MailServerCandidate(label, sender));
            } catch (Exception e) {
                // buildSender 失敗（設定錯誤，例如連接埠非數字）沒有可用的 sender 可保留，
                // 跳過此組並繼續處理其餘候選，不因單組設定錯誤中斷整個初始化迴圈。
                String reason = safeReason(e);
                failures.add(label + " - " + reason);
                log.warn("初始化郵件伺服器時無法建立寄件器，將跳過此組：{}，原因：{}", label, reason);
            }
        }

        // 無論是否全部測試失敗，皆保留候選清單：即使初始化當下全部不可用，仍可能於實際發送時已恢復，
        // 或至少能在發送呼叫時再次以一致的彙整格式回報失敗（見 executeWithFailover），不因此讓服務永久不可用。
        this.candidates = List.copyOf(initialized);

        if (!servers.isEmpty() && failures.size() == servers.size()) {
            throw new MessagingException("所有郵件伺服器初始化皆失敗：" + String.join("; ", failures));
        }
        log.info("初始化郵件服務完成，設定伺服器數：{}", initialized.size());
    }

    /**
     * 依單組 SMTP 設定建立 {@link JavaMailSenderImpl}。
     * <p>
     * 逾時（連線／讀取／寫入）與除錯開關為全域設定，套用至每一組伺服器；
     * 認證、TLS、通訊協定與（若啟用）Base64 密碼解碼則採用該組各自的設定。
     * </p>
     * <p>
     * JavaMail／Angus 依通訊協定名稱決定讀取哪組屬性前綴（例如 {@code transport-protocol=smtps}
     * 讀取 {@code mail.smtps.*}，而非 {@code mail.smtp.*}）。逾時三鍵與
     * {@code socketFactory.class} 因此對 {@code mail.smtp.*} 與該組實際協定前綴
     * （見 {@link #protocolPrefix(String)}）雙寫，使非預設協定（如 smtps）的逾時設定也能真正生效，
     * 且不影響既有 smtp 組別的屬性鍵。
     * </p>
     *
     * @param server 單組 SMTP 伺服器設定
     * @return 已完成設定、尚未測試連線的 {@link JavaMailSenderImpl}
     */
    private JavaMailSenderImpl buildSender(MailServerProperty server) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setProtocol(server.getTransportProtocol());
        Properties javaMailProperties = new Properties();

        if (Boolean.TRUE.equals(server.getSmtpAuthEnable())) {
            javaMailProperties.put("mail.smtp.auth", server.getSmtpAuthEnable());
            sender.setUsername(server.getUsername());
            String mailPassword = server.getPa55word();
            // 若設定檔將密碼加密開啟，需先解碼才能正確驗證
            if (Boolean.TRUE.equals(server.getEncryptEnable()) && StringUtils.isNotBlank(mailPassword)) {
                CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
                mailPassword = cryptoUtil.decode(mailPassword);
            }
            sender.setPassword(mailPassword);
        }

        String protocolPrefix = protocolPrefix(server.getTransportProtocol());

        // 加入 TLS 加密傳輸與 SSL Socket 認證機制
        javaMailProperties.put("mail.smtp.starttls.enable", server.getSmtpStartTlsEnable());
        javaMailProperties.put("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        javaMailProperties.put("mail.debug", mailPropertyConfig.getDebugEnable());
        sender.setHost(server.getHost());
        sender.setPort(Integer.parseInt(server.getPort()));

        // 連線逾時、讀取逾時與寫入逾時，避免 SMTP 連線無限期阻塞（全域設定，套用至每一組）
        putForProtocol(javaMailProperties, protocolPrefix, "connectiontimeout", mailPropertyConfig.getConnectionTimeout());
        putForProtocol(javaMailProperties, protocolPrefix, "timeout", mailPropertyConfig.getReadTimeout());
        putForProtocol(javaMailProperties, protocolPrefix, "writetimeout", mailPropertyConfig.getWriteTimeout());
        putForProtocol(javaMailProperties, protocolPrefix, "socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        sender.setJavaMailProperties(javaMailProperties);
        return sender;
    }

    /**
     * 依 {@code transport-protocol} 正規化推導 JavaMail 屬性前綴（trim、轉小寫），未設定或空白時
     * 退回 {@code smtp}，與 {@link MailServerProperty#getTransportProtocol()} 的預設值一致。
     */
    private static String protocolPrefix(String transportProtocol) {
        String trimmed = StringUtils.trimToNull(transportProtocol);
        return trimmed == null ? "smtp" : trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * 將屬性同時寫入 {@code mail.smtp.<key>} 與該組實際協定前綴的 {@code mail.<protocolPrefix>.<key>}
     * （協定為 smtp 時兩者相同，僅寫入一次），使非預設協定（如 smtps）也能讀到相同設定值，
     * 且不移除既有 smtp 屬性鍵。
     */
    private static void putForProtocol(Properties properties, String protocolPrefix, String key, Object value) {
        properties.put("mail.smtp." + key, value);
        if (!"smtp".equals(protocolPrefix)) {
            properties.put("mail." + protocolPrefix + "." + key, value);
        }
    }

    /**
     * 產生伺服器於日誌中的可辨識標籤，不含帳號密碼等敏感資訊。
     *
     * @param server 伺服器設定
     * @return {@code name(host:port)} 或無 name 時的 {@code host:port}
     */
    private static String label(MailServerProperty server) {
        String hostPort = server.getHost() + ":" + server.getPort();
        return StringUtils.isNotBlank(server.getName()) ? server.getName() + "(" + hostPort + ")" : hostPort;
    }

    /**
     * 取得不含外部伺服器回傳文字的安全失敗摘要。
     * SMTP 例外訊息可能夾帶認證資訊，因此只保留例外類型。
     *
     * @param e 例外物件
     * @return 不含帳號密碼的例外類型
     */
    private static String safeReason(Throwable e) {
        return e.getClass().getSimpleName();
    }

    /**
     * 前一組失敗後整體預算已耗盡、仍有候選未嘗試時，於最後一筆失敗摘要補註整體逾時原因。
     * 不新增摘要筆數，使彙整例外的「已嘗試 N 組」仍等於實際嘗試組數。
     *
     * @param failureSummaries 目前的逐組失敗摘要（可修改）
     * @param operationName    發送方法名稱，用於日誌辨識
     */
    private static void markStoppedByOverallTimeout(List<String> failureSummaries, String operationName) {
        log.warn("郵件發送超過整體時間上限（{}），停止嘗試其餘伺服器", operationName);
        if (!failureSummaries.isEmpty()) {
            int last = failureSummaries.size() - 1;
            failureSummaries.set(
                    last, failureSummaries.get(last) + "（其後整體逾時（overall-timeout）已到期，未再嘗試其餘伺服器）");
        }
    }

    /**
     * 複製候選 sender，並把三種底層 socket 逾時縮到本次剩餘的整體時間內。
     * 不修改共用 sender，避免併發寄信互相覆寫 JavaMail 設定。
     * <p>
     * 截止機制以「委派」方式套用：實際連線仍依原設定的 {@code mail.smtp.socketFactory.class}
     * （例如隱式 TLS 用的 {@link javax.net.ssl.SSLSocketFactory}）建立與（若為 SSL）完成 handshake，
     * {@link DeadlineSocketFactory} 只另外掛上到期關閉排程，不得以一般 TCP socket 取代委派工廠應建立的
     * SSL socket，否則隱式 TLS（如連線至 465 埠）會在寄送階段以明文對談失敗。JavaMail 的
     * {@code SocketFetcher} 對「提供 {@code mail.smtp.socketFactory} 實例」的用法只會呼叫無參數版
     * {@code createSocket()} 並自行完成後續連線，因此連線目標（host/port）須隨建構子一併提供，
     * 讓委派工廠能在無參數版本內就主動連線並完成 handshake，而非等待呼叫端另行連線。
     * </p>
     * <p>
     * 縮限與截止工廠注入同樣依 {@link #protocolPrefix(String)} 對該組實際協定前綴進行（smtp 時
     * 僅有 {@code mail.smtp.*}，不重複寫入）。協定為隱式 TLS（{@code smtps}）時，截止工廠額外注入
     * {@code mail.smtps.ssl.socketFactory}——JavaMail／Angus 對「協定本身即為 SSL」的連線改讀此鍵
     * 取得自訂 socket factory，不同於 STARTTLS 情境讀取的 {@code mail.<prefix>.socketFactory}；
     * 且隱式 TLS 不得如 STARTTLS 情境般退回明文 socket，故 {@code fallbackToPlainSocket} 強制為
     * {@code false}，避免委派失敗時誤判為明文連線成功。
     * </p>
     */
    private static JavaMailSenderImpl senderForAttempt(
            JavaMailSenderImpl configured, long remainingMs, long deadlineNanos) {
        if (remainingMs == Long.MAX_VALUE) {
            return configured;
        }
        int limit = (int) Math.max(1, Math.min(remainingMs, Integer.MAX_VALUE));
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setProtocol(configured.getProtocol());
        sender.setHost(configured.getHost());
        sender.setPort(configured.getPort());
        sender.setUsername(configured.getUsername());
        sender.setPassword(configured.getPassword());
        Properties properties = new Properties();
        properties.putAll(configured.getJavaMailProperties());

        String protocolPrefix = protocolPrefix(configured.getProtocol());
        boolean implicitSsl = "smtps".equals(protocolPrefix);

        capTimeout(properties, "mail.smtp.connectiontimeout", limit);
        capTimeout(properties, "mail.smtp.timeout", limit);
        capTimeout(properties, "mail.smtp.writetimeout", limit);
        if (!"smtp".equals(protocolPrefix)) {
            capTimeout(properties, "mail." + protocolPrefix + ".connectiontimeout", limit);
            capTimeout(properties, "mail." + protocolPrefix + ".timeout", limit);
            capTimeout(properties, "mail." + protocolPrefix + ".writetimeout", limit);
        }

        SocketFactory delegate = resolveDelegateSocketFactory(
                (String) properties.get("mail." + protocolPrefix + ".socketFactory.class"));
        // 隱式 TLS（smtps）不得退回明文 socket，其餘協定維持既有可設定的 fallback 開關
        boolean fallbackToPlainSocket = !implicitSsl
                && !"false"
                        .equalsIgnoreCase(String.valueOf(properties.getOrDefault("mail.smtp.socketFactory.fallback", "true")));
        int connectTimeoutMillis = (int) properties.get("mail." + protocolPrefix + ".connectiontimeout");
        DeadlineSocketFactory deadlineSocketFactory = new DeadlineSocketFactory(
                deadlineNanos,
                delegate,
                fallbackToPlainSocket,
                connectTimeoutMillis,
                configured.getHost(),
                configured.getPort(),
                implicitSsl);
        properties.put("mail." + protocolPrefix + ".socketFactory", deadlineSocketFactory);
        if (implicitSsl) {
            properties.put("mail." + protocolPrefix + ".ssl.socketFactory", deadlineSocketFactory);
        }
        sender.setJavaMailProperties(properties);
        return sender;
    }

    /**
     * 依原設定的 {@code mail.smtp.socketFactory.class} 解析實際負責建立 socket 的委派工廠，
     * 藉此讓截止機制不改變原本的 TLS 語意（例如隱式 TLS 仍取得 SSL socket）。
     * 無法解析時退回一般 {@link SocketFactory}，等同於原本未指定 socketFactory.class 的行為。
     */
    private static SocketFactory resolveDelegateSocketFactory(String factoryClassName) {
        if (StringUtils.isBlank(factoryClassName)) {
            return SocketFactory.getDefault();
        }
        try {
            Class<?> factoryClass = Class.forName(factoryClassName);
            Object instance = factoryClass.getMethod("getDefault").invoke(null);
            if (instance instanceof SocketFactory socketFactory) {
                return socketFactory;
            }
        } catch (ReflectiveOperationException ignored) {
            // 無法反射取得指定 factory 時退回一般 SocketFactory；連線建立仍受 fallback 開關保護。
        }
        return SocketFactory.getDefault();
    }

    private static void capTimeout(Properties properties, String key, int limit) {
        Object configured = properties.get(key);
        int value;
        try {
            value = Integer.parseInt(String.valueOf(configured));
        } catch (NumberFormatException e) {
            value = limit;
        }
        properties.put(key, Math.max(1, Math.min(value, limit)));
    }

    /**
     * 在獨立 daemon 執行緒執行單次寄送，讓呼叫端可對整個 SMTP 對話套用剩餘截止時間。
     * 底層 socket timeout 仍會同步縮限，確保取消後的 JavaMail 工作可在有限時間內結束。
     * <p>
     * {@code deadlineNanos} 為絕對時間點（{@link System#nanoTime()} 基準），實際等待逾時值
     * 在呼叫 {@link Future#get(long, TimeUnit)} 前才即時重新計算，避免建立 sender／executor
     * 期間耗費的時間未被計入剩餘預算，導致單次嘗試的實際牆鐘時間超出整體截止上限。
     * </p>
     */
    private static void sendWithinDeadline(
            MailSendOperation operation, JavaMailSenderImpl sender, long deadlineNanos) throws Exception {
        if (deadlineNanos == Long.MAX_VALUE) {
            operation.send(sender);
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "mail-failover-deadline");
            thread.setDaemon(true);
            return thread;
        });
        Future<?> future = executor.submit(() -> {
            operation.send(sender);
            return null;
        });
        try {
            long timeoutNanos = Math.max(0L, deadlineNanos - System.nanoTime());
            future.get(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new OverallTimeoutException();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 依優先序清單逐組嘗試發送，任一組成功即返回；全部失敗則拋出 {@link MailFailoverException}。
     * <p>
     * 受 {@code mail.failover.max-attempts}（最大嘗試組數）與
     * {@code mail.failover.overall-timeout}（整體切換時間上限，毫秒）限制，避免全部伺服器
     * 皆不可達時無上限累加阻塞呼叫端。候選清單於方法開頭讀取一次區域變數，
     * 不因併發呼叫互相干擾（候選清單本身為不可變物件，各組 {@link JavaMailSenderImpl}
     * 亦不被跨執行緒修改）。
     * </p>
     *
     * @param operationName 發送方法名稱，用於日誌與例外訊息辨識
     * @param operation     實際組裝並送出郵件的操作，接收本次嘗試使用的 {@link JavaMailSenderImpl}
     * @throws IllegalStateException  尚未呼叫 {@link #setInitData()} 或無任何候選伺服器時拋出
     * @throws MailFailoverException  全部候選伺服器皆嘗試失敗時拋出（unchecked，彙整各組失敗原因）
     */
    private void executeWithFailover(String operationName, MailSendOperation operation) {
        List<MailServerCandidate> currentCandidates = this.candidates;
        if (currentCandidates == null || currentCandidates.isEmpty()) {
            throw new IllegalStateException("郵件服務尚未初始化或無可用伺服器，請先呼叫 setInitData()");
        }

        int maxAttempts = Math.min(mailPropertyConfig.getFailover().getMaxAttempts(), currentCandidates.size());
        long overallTimeoutMs = mailPropertyConfig.getFailover().getOverallTimeout();
        long overallTimeoutNanos = overallTimeoutMs > 0 ? TimeUnit.MILLISECONDS.toNanos(overallTimeoutMs) : Long.MAX_VALUE;
        long startedNanos = System.nanoTime();
        // 絕對截止時間點，各次嘗試皆以此為準即時重新計算剩餘預算，不使用迴圈起始時的過期快照。
        long deadlineNanos = overallTimeoutNanos == Long.MAX_VALUE ? Long.MAX_VALUE : startedNanos + overallTimeoutNanos;

        List<String> failureSummaries = new ArrayList<>();
        List<Throwable> failureCauses = new ArrayList<>();
        int attempted = 0;

        for (MailServerCandidate candidate : currentCandidates) {
            long elapsedNanos = System.nanoTime() - startedNanos;
            long remainingNanos = overallTimeoutNanos == Long.MAX_VALUE
                    ? Long.MAX_VALUE
                    : overallTimeoutNanos - elapsedNanos;
            if (attempted >= maxAttempts) {
                break;
            }
            if (remainingNanos <= 0) {
                // 前一組以一般原因失敗時整體預算恰好耗盡：其餘候選因整體逾時未再嘗試，
                // 於最後一筆摘要補註原因（不計入嘗試組數），避免彙整訊息看不出是整體逾時截止。
                markStoppedByOverallTimeout(failureSummaries, operationName);
                break;
            }
            attempted++;
            boolean deadlineReached = false;
            // 無條件進位為毫秒：縮限後的 socket 逾時自本輪起點起算必不早於絕對截止時間到期，
            // 使「因縮限而觸發的 socket 逾時」與「截止時間已到」在時間上一致，可由下方以實際時間判定。
            long remainingMs = remainingNanos == Long.MAX_VALUE
                    ? Long.MAX_VALUE
                    : Math.max(1, (remainingNanos + 999_999L) / 1_000_000L);
            try {
                sendWithinDeadline(
                        operation, senderForAttempt(candidate.sender(), remainingMs, deadlineNanos), deadlineNanos);
                log.info("郵件發送成功（{}），使用伺服器：{}", operationName, candidate.label());
                return;
            } catch (OverallTimeoutException e) {
                deadlineReached = true;
                // 整體逾時為本類別自行拋出、訊息不含外部伺服器回應，可直接明示原因不需經 safeReason 過濾。
                String reason = "整體逾時（overall-timeout）已到期";
                log.warn("郵件發送超過整體時間上限（{}），伺服器：{}", operationName, candidate.label());
                failureSummaries.add(candidate.label() + " - " + reason);
                failureCauses.add(new IllegalStateException(candidate.label() + " - " + reason));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                deadlineReached = true;
                String reason = safeReason(e);
                failureSummaries.add(candidate.label() + " - " + reason);
                failureCauses.add(new IllegalStateException(candidate.label() + " - " + reason));
            } catch (Exception e) {
                // 僅在失敗發生時絕對截止時間確實已到，才歸類為整體逾時。單項逾時短於剩餘預算時
                // （例如 read-timeout 300ms、剩餘 700ms），即使其他單項逾時被縮限，該次逾時仍屬
                // 此伺服器本身的失敗，須繼續嘗試下一組；反之被縮限的 socket 逾時、或截止排程關閉
                // socket 造成的例外，必定發生於截止時間之後，與 Future 層整體逾時判定結果一致。
                if (deadlineNanos != Long.MAX_VALUE && System.nanoTime() - deadlineNanos >= 0) {
                    deadlineReached = true;
                    String reason = "整體逾時（overall-timeout）已到期";
                    log.warn("郵件發送超過整體時間上限（{}），伺服器：{}", operationName, candidate.label());
                    failureSummaries.add(candidate.label() + " - " + reason);
                    failureCauses.add(new IllegalStateException(candidate.label() + " - " + reason));
                } else {
                    String reason = safeReason(e);
                    log.warn(
                            "郵件發送失敗（{}），伺服器：{}，原因：{}，將嘗試下一組",
                            operationName,
                            candidate.label(),
                            reason);
                    failureSummaries.add(candidate.label() + " - " + reason);
                    // 不保留原始 cause/message，避免 SMTP 伺服器把密碼回顯進例外鏈。
                    failureCauses.add(new IllegalStateException(candidate.label() + " - " + reason));
                }
            }
            if (deadlineReached) {
                break;
            }
        }

        log.error("郵件發送最終失敗（{}），已嘗試 {} 組伺服器：{}", operationName, attempted, failureSummaries);
        MailFailoverException aggregate = new MailFailoverException(operationName, failureSummaries);
        failureCauses.forEach(aggregate::addSuppressed);
        throw aggregate;
    }

    /**
     * 發送 HTML 格式的 MIME 信件，套用多 SMTP 容錯切換。
     * <p>
     * 全部候選伺服器皆嘗試失敗時，僅記錄錯誤日誌，不向外拋出例外
     * （維持既有呼叫端不需額外處理例外的行為）。
     * </p>
     *
     * @param mail 包含寄件人、收件人、主旨及 HTML 內容的郵件資料物件
     */
    @Override
    public void sendEmail(Mail mail) {
        try {
            executeWithFailover("sendEmail", sender -> {
                MimeMessage mimeMessage = sender.createMimeMessage();
                MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
                mimeMessageHelper.setSubject(mail.getMailSubject());
                mimeMessageHelper.setFrom(mail.getMailFrom());
                mimeMessageHelper.setTo(mail.getMailTo());
                mimeMessageHelper.setCc(mail.getMailCc());
                mimeMessageHelper.setText(mail.getMailContent(), true);
                sender.send(mimeMessageHelper.getMimeMessage());
            });
        } catch (RuntimeException e) {
            log.error("Sent mail error:{}", e.getMessage());
        }
    }

    /**
     * 發送純文字格式的簡易信件，套用多 SMTP 容錯切換。
     * <p>
     * 使用 {@link SimpleMailMessage} 組裝信件後發送，適用於不需要 HTML 或附件的簡單通知場景。
     * </p>
     *
     * @param mail 包含寄件人、收件人、主旨及純文字內容的郵件資料物件
     */
    @Override
    public void simpleMailSend(Mail mail) {
        executeWithFailover("simpleMailSend", sender -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mail.getMailFrom());
            message.setTo(mail.getMailTo());
            message.setSubject(mail.getMailSubject());
            message.setText(mail.getMailContent());
            sender.send(message);
        });
    }

    /**
     * 發送含附件的信件，支援多個附件，套用多 SMTP 容錯切換。
     * <p>
     * 使用 JavaMail 的 {@link MimeMessage}，支援更複雜的郵件格式與內容。
     * {@link MimeMessageHelper} 設定為多部分模式（multipart=true），以便加入附件檔案。
     * </p>
     *
     * @param mail 包含收件人、主旨、內容及附件清單的郵件資料物件
     */
    @Override
    public void attachedSend(Mail mail) {
        executeWithFailover("attachedSend", sender -> {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            mimeMessageHelper.setFrom(mail.getMailFrom());
            mimeMessageHelper.setTo(mail.getMailTo());
            mimeMessageHelper.setCc(mail.getMailCc());
            mimeMessageHelper.setSubject(mail.getMailSubject());
            mimeMessageHelper.setText(mail.getMailContent());

            if (null != mail.getAttachments()) {
                for (File file : mail.getAttachments()) {
                    mimeMessageHelper.addAttachment(file.getName(), file);
                }
            }
            sender.send(mimeMessage);
        });
    }

    /**
     * 發送 HTML 富文字信件，支援多張內嵌圖片與附件，套用多 SMTP 容錯切換。
     * <p>
     * 使用 {@link MimeMessageHelper} 將郵件內容設定為 HTML 模式（{@code setText(..., true)}），
     * 可透過 {@code cid:} 參照在 HTML 中嵌入圖片資源，並同時支援附件。
     * 若副本收件人（CC）清單為空則略過設定，避免傳入空陣列導致例外。
     * </p>
     *
     * @param mail 包含收件人、副本、主旨、HTML 內容、內嵌資源及附件清單的郵件資料物件
     */
    @Override
    public void richContentSend(Mail mail) {
        executeWithFailover("richContentSend", sender -> {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            mimeMessageHelper.setFrom(mail.getMailFrom());
            mimeMessageHelper.setTo(mail.getMailTo());
            // 僅在 CC 清單非空時才設定，避免傳入空陣列引發 MessagingException
            if (!Objects.isNull(mail.getMailCc()) && mail.getMailCc().length > 0) {
                mimeMessageHelper.setCc(mail.getMailCc());
            }
            mimeMessageHelper.setSubject(mail.getMailSubject());
            // 第二個參數 true 表示 text 的內容為 HTML；<img/> 標籤中 src='cid:xxx' 的 'xxx'
            // 須對應 mail.getInlineResources() 的 key，才會被下方 addInline 產生真正的 Content-ID 內嵌資源，
            // 否則信件用戶端僅會顯示為一般附件，無法解析為內嵌圖片
            mimeMessageHelper.setText(mail.getMailContent(), true);

            if (null != mail.getInlineResources()) {
                for (Map.Entry<String, File> inline : mail.getInlineResources().entrySet()) {
                    mimeMessageHelper.addInline(inline.getKey(), inline.getValue());
                }
            }
            if (null != mail.getAttachments()) {
                for (File file : mail.getAttachments()) {
                    mimeMessageHelper.addAttachment(file.getName(), file);
                }
            }
            sender.send(mimeMessage);
        });
    }

    /**
     * 群發信件至多位收件人，並支援多個附件，套用多 SMTP 容錯切換。
     * <p>
     * 當附件清單不為空時，手動建立 {@link MimeMultipart} 結構，
     * 以 {@link MimeBodyPart} 分別存放 HTML 內文與各附件，最終組裝為完整的 MIME 信件。
     * 若無附件，則直接透過 {@link MimeMessageHelper} 設定 HTML 內容。
     * 收件人採用 {@link InternetAddress} 陣列形式設定，以正確支援多位收件人。
     * </p>
     *
     * @param mail 包含多位收件人、主旨、HTML 內容及附件清單的郵件資料物件
     * @throws Exception 當信件組裝（含附件檔名編碼）失敗時拋出
     */
    @Override
    public void sendBatchMailWithFile(Mail mail) throws Exception {
        executeWithFailover("sendBatchMailWithFile", sender -> {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            mimeMessageHelper.setFrom(new InternetAddress(MimeUtility.encodeText(mail.getMailFrom())));
            mimeMessageHelper.setSubject(mail.getMailSubject());
            if (CollectionUtils.isNotEmpty(mail.getAttachments())) {
                // 建立一個存放信件內文的 BodyPart 物件
                BodyPart mdp = new MimeBodyPart();
                // 設定 BodyPart 的內容為 HTML 格式，並指定字元編碼為 UTF-8
                mdp.setContent(mail.getMailContent(), "text/html;charset=UTF-8");
                // 建立 MimeMultipart 物件，用來存放多個 BodyPart（內文與附件）
                Multipart mm = new MimeMultipart();
                // 將 HTML 內文的 BodyPart 加入 MimeMultipart（可加入多個 BodyPart）
                mm.addBodyPart(mdp);
                // 宣告附件相關區域變數，逐一加入附件
                MimeBodyPart filePart;
                FileDataSource filedatasource;
                // 逐個加入附件至 MimeMultipart
                for (int j = 0; j < mail.getAttachments().size(); j++) {
                    filePart = new MimeBodyPart();
                    filedatasource = new FileDataSource(mail.getAttachments().get(j));
                    filePart.setDataHandler(new DataHandler(filedatasource));
                    try {
                        // 對附件檔名進行 MIME 編碼，確保中文或特殊字元的檔名能正確傳輸
                        filePart.setFileName(MimeUtility.encodeText(filedatasource.getName()));
                    } catch (Exception e) {
                        log.error("Add attachment error : {}", e.getMessage());
                    }
                    mm.addBodyPart(filePart);
                }
                mimeMessage.setContent(mm);
            } else {
                mimeMessageHelper.setText(mail.getMailContent(), true);
            }

            // 不可使用 String 陣列直接設定收件人，需轉為 InternetAddress 陣列才能正確支援多位收件人
            List<InternetAddress> list = new ArrayList<>();
            for (int i = 0; i < mail.getMailTo().length; i++) {
                list.add(new InternetAddress(mail.getMailTo()[i]));
            }
            InternetAddress[] address = list.toArray(new InternetAddress[list.size()]);

            mimeMessage.setRecipients(Message.RecipientType.TO, address);
            sender.send(mimeMessageHelper.getMimeMessage());
        });
    }

    /**
     * 單組 SMTP 候選伺服器：日誌標籤（不含敏感資訊）與已完成設定的發送器。
     *
     * @param label  用於日誌辨識的伺服器標籤（{@code name(host:port)} 或 {@code host:port}）
     * @param sender 已完成設定、可直接用於發送的 {@link JavaMailSenderImpl}
     */
    private record MailServerCandidate(String label, JavaMailSenderImpl sender) {}

    private static final class OverallTimeoutException extends Exception {}

    /**
     * 建立 socket 時委派給原設定的 socket factory（保留其 TLS 語意，例如隱式 TLS 的
     * {@link javax.net.ssl.SSLSocketFactory}），並在本次寄送的絕對截止時間關閉它。
     * <p>
     * 委派為 SSL socket 時，比照 JavaMail 內建 {@code SocketFetcher} 的既有機制，在連線後
     * 立即以連線逾時為上限執行 {@code startHandshake()}：若對方並非 TLS 端點（例如僅支援
     * STARTTLS 的明文 SMTP），handshake 會在逾時內失敗並可視 {@code fallbackToPlainSocket}
     * 退回一般 TCP socket；若在此不主動 handshake，TLS 失敗會延後到實際收發 SMTP 對話時才發生，
     * 屆時已無法退回，隱式 TLS 與明文 fallback 語意都會被破壞。
     * </p>
     */
    private static final class DeadlineSocketFactory extends SocketFactory {

        private final long deadlineNanos;
        private final SocketFactory delegate;
        private final boolean fallbackToPlainSocket;
        private final int connectTimeoutMillis;
        private final String targetHost;
        private final int targetPort;
        private final boolean implicitSsl;

        private DeadlineSocketFactory(
                long deadlineNanos,
                SocketFactory delegate,
                boolean fallbackToPlainSocket,
                int connectTimeoutMillis,
                String targetHost,
                int targetPort,
                boolean implicitSsl) {
            this.deadlineNanos = deadlineNanos;
            this.delegate = delegate;
            this.fallbackToPlainSocket = fallbackToPlainSocket;
            this.connectTimeoutMillis = Math.max(1, connectTimeoutMillis);
            this.targetHost = targetHost;
            this.targetPort = targetPort;
            this.implicitSsl = implicitSsl;
        }

        @Override
        public Socket createSocket() throws IOException {
            // 隱式 TLS（smtps）透過 mail.<prefix>.ssl.socketFactory 註冊同一實例；JavaMail／Angus
            // 對此鍵的用法是直接使用本方法回傳、已連線且已完成 handshake 的 SSLSocket，不會另外
            // 呼叫 connect()，也不會等它 instanceof SSLSocket 才放行——因此隱式 TLS 情境維持原本
            // 就地連線＋handshake 的行為，不能延遲。
            //
            // 明文 smtp（含仍會嘗試 SSL handshake 再退回明文的既有相容邏輯）僅透過
            // mail.smtp.socketFactory 註冊；Angus/JavaMail 的 SocketFetcher 對此鍵的用法只會呼叫
            // 本無參數版本取得「尚未連線」的 socket，之後自行呼叫 socket.connect(SocketAddress, int)。
            // 若在此就先行完成連線，呼叫端這次 connect() 會因 socket 已連線而拋出例外；SocketFetcher
            // 會捕捉此例外並改用它自己完全不受本截止機制保護、也不會被排程關閉的一般 socket，使整個
            // 逾時／TLS 委派機制對這次連線失效（呼叫端仍會在整體逾時內收到例外，但底層 SMTP 對話會
            // 在背景不受限制地繼續進行）。因此明文情境改為回傳延遲連線的包裝物件，實際連線、TLS
            // handshake、fallback 判斷與到期關閉排程皆遞延至呼叫端呼叫 connect() 時才執行。
            if (implicitSsl) {
                return guard(newConnectedSocket(targetHost, targetPort, null, 0));
            }
            return new DeferredConnectSocket();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return guard(newConnectedSocket(host, port, null, 0));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort)
                throws IOException {
            return guard(newConnectedSocket(host, port, localAddress, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return guard(newConnectedSocket(host.getHostAddress(), port, null, 0));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                throws IOException {
            return guard(newConnectedSocket(address.getHostAddress(), port, localAddress, localPort));
        }

        private Socket newConnectedSocket(String host, int port, InetAddress localAddress, int localPort)
                throws IOException {
            Socket delegateSocket = null;
            try {
                delegateSocket = delegate.createSocket();
                return connectAndHandshake(delegateSocket, host, port, localAddress, localPort);
            } catch (IOException e) {
                // 委派工廠已實際建立 TCP 連線（甚至完成部分 handshake）才失敗時，該 socket 對遠端而言
                // 仍是一條存活的連線；若不在此明確關閉，退回明文 socket 後原本這條連線會被靜默丟棄，
                // 造成連線與檔案描述元洩漏，且伺服器端會誤以為該連線仍在等待後續資料。
                if (delegateSocket != null) {
                    closeQuietly(delegateSocket);
                }
                if (!fallbackToPlainSocket) {
                    throw e;
                }
                Socket socket = new Socket();
                if (localAddress != null) {
                    socket.bind(new InetSocketAddress(localAddress, localPort));
                }
                socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
                return socket;
            }
        }

        private Socket connectAndHandshake(
                Socket socket, String host, int port, InetAddress localAddress, int localPort) throws IOException {
            if (localAddress != null) {
                socket.bind(new InetSocketAddress(localAddress, localPort));
            }
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            if (socket instanceof SSLSocket sslSocket) {
                // 本工廠在 JavaMail／Angus 之前就主動完成 handshake，若不在此設定 endpoint
                // identification algorithm，JavaMail 內建的 checkserveridentity 主機名稱驗證
                // 即形同被繞過；比照 JSSE 標準 HTTPS 主機名稱比對規則於 handshake 當下完成驗證。
                SSLParameters sslParameters = sslSocket.getSSLParameters();
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
                sslSocket.setSSLParameters(sslParameters);
                int previousTimeout = socket.getSoTimeout();
                socket.setSoTimeout(connectTimeoutMillis);
                try {
                    sslSocket.startHandshake();
                } finally {
                    // handshake 失敗（例如主機名稱驗證不符）時，JSSE 可能已自行關閉底層 socket；
                    // 此時還原逾時值必然拋出 SocketException，若不吞掉會蓋掉 try 區塊真正的失敗原因，
                    // 讓呼叫端誤以為是逾時還原失敗而非實際的憑證／交握錯誤。
                    try {
                        socket.setSoTimeout(previousTimeout);
                    } catch (IOException ignored) {
                        // 忽略；socket 已因 handshake 失敗而關閉，還原逾時已無意義
                    }
                }
            }
            return socket;
        }

        private Socket guard(Socket socket) {
            long delayNanos = Math.max(0, deadlineNanos - System.nanoTime());
            DEADLINE_SOCKET_CLOSER.schedule(() -> closeQuietly(socket), delayNanos, TimeUnit.NANOSECONDS);
            return socket;
        }

        private static void closeQuietly(Socket socket) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 截止關閉採 best effort；socket 已關閉時不需另外處理。
            }
        }

        /**
         * {@link #createSocket()} 無參數版本回傳的延遲連線包裝物件。
         * <p>
         * 建構時尚未連線；直到呼叫端（JavaMail 的 {@code SocketFetcher}）呼叫
         * {@link #connect(SocketAddress, int)} 時，才實際委派 {@link #newConnectedSocket}
         * 完成連線、TLS handshake 判斷與 fallback，並在成功後才排入到期關閉排程——避免
         * 提早連線導致呼叫端的 {@code connect()} 因「socket 已連線」而失敗，讓真正用於
         * SMTP 對話的連線改由呼叫端自行建立、脫離本截止機制與 TLS 委派語意的保護。
         * 所有實際 I/O 皆委派給連線完成後取得的 {@link #real} socket。
         * </p>
         */
        private final class DeferredConnectSocket extends Socket {

            private volatile Socket real;

            @Override
            public void connect(SocketAddress endpoint, int timeout) throws IOException {
                InetSocketAddress target = (InetSocketAddress) endpoint;
                this.real = guard(newConnectedSocket(
                        target.getHostString(), target.getPort(), null, 0));
            }

            @Override
            public void connect(SocketAddress endpoint) throws IOException {
                connect(endpoint, 0);
            }

            @Override
            public void bind(SocketAddress bindpoint) {
                // 委派工廠回傳的 socket 一律以目標 host/port 直接連線，不支援先行 bind 本地位址；
                // JavaMail 的 SocketFetcher 在無參數版本情境下不會傳入本地位址，故安全忽略。
            }

            private Socket real() throws SocketException {
                Socket socket = real;
                if (socket == null) {
                    throw new SocketException("尚未連線：請先呼叫 connect()");
                }
                return socket;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return real().getInputStream();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return real().getOutputStream();
            }

            @Override
            public void setSoTimeout(int timeout) throws SocketException {
                real().setSoTimeout(timeout);
            }

            @Override
            public int getSoTimeout() throws SocketException {
                return real().getSoTimeout();
            }

            @Override
            public void close() throws IOException {
                Socket socket = real;
                if (socket != null) {
                    socket.close();
                }
            }

            @Override
            public boolean isConnected() {
                Socket socket = real;
                return socket != null && socket.isConnected();
            }

            @Override
            public boolean isClosed() {
                Socket socket = real;
                return socket == null || socket.isClosed();
            }

            @Override
            public boolean isBound() {
                Socket socket = real;
                return socket != null && socket.isBound();
            }

            @Override
            public InetAddress getInetAddress() {
                Socket socket = real;
                return socket != null ? socket.getInetAddress() : null;
            }

            @Override
            public int getPort() {
                Socket socket = real;
                return socket != null ? socket.getPort() : 0;
            }

            @Override
            public int getLocalPort() {
                Socket socket = real;
                return socket != null ? socket.getLocalPort() : -1;
            }

            @Override
            public SocketAddress getRemoteSocketAddress() {
                Socket socket = real;
                return socket != null ? socket.getRemoteSocketAddress() : null;
            }
        }
    }

    /**
     * 單次發送嘗試的操作介面，由呼叫端（各發送方法）以 lambda 提供，
     * 在每次嘗試內以當次候選伺服器的 {@link JavaMailSenderImpl} 重新組裝並送出郵件，
     * 確保訊息組裝與實際送出使用同一組伺服器設定。
     */
    @FunctionalInterface
    private interface MailSendOperation {
        void send(JavaMailSenderImpl sender) throws Exception;
    }
}
