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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
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
 * {@link MailFailoverException}。此機制為「至少一次投遞」語意：若 SMTP 已接收郵件內容
 * 才回報失敗，切換重送可能導致收件者收到重複郵件，屬機制本質限制。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/04/26 下午 14:00
 **/
@Slf4j
public class MailServiceImpl implements MailService {

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
     * 不會因部分伺服器不可用而中斷初始化；只有全部伺服器皆測試失敗時才拋出例外。
     * </p>
     *
     * @throws MessagingException 當全部 SMTP 伺服器連線測試皆失敗時拋出
     */
    @Override
    public void setInitData() throws MessagingException {
        List<MailServerProperty> servers = mailPropertyConfig.resolveServers();
        List<MailServerCandidate> initialized = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (MailServerProperty server : servers) {
            String label = label(server);
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
        }

        // 無論是否全部測試失敗，皆保留候選清單：即使初始化當下全部不可用，仍可能於實際發送時已恢復，
        // 或至少能在發送呼叫時再次以一致的彙整格式回報失敗（見 executeWithFailover），不因此讓服務永久不可用。
        this.candidates = List.copyOf(initialized);

        if (!initialized.isEmpty() && failures.size() == initialized.size()) {
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

        // 加入 TLS 加密傳輸與 SSL Socket 認證機制
        javaMailProperties.put("mail.smtp.starttls.enable", server.getSmtpStartTlsEnable());
        javaMailProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        javaMailProperties.put("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        javaMailProperties.put("mail.debug", mailPropertyConfig.getDebugEnable());
        sender.setHost(server.getHost());
        sender.setPort(Integer.parseInt(server.getPort()));

        // 連線逾時、讀取逾時與寫入逾時，避免 SMTP 連線無限期阻塞（全域設定，套用至每一組）
        javaMailProperties.put("mail.smtp.connectiontimeout", mailPropertyConfig.getConnectionTimeout());
        javaMailProperties.put("mail.smtp.timeout", mailPropertyConfig.getReadTimeout());
        javaMailProperties.put("mail.smtp.writetimeout", mailPropertyConfig.getWriteTimeout());
        sender.setJavaMailProperties(javaMailProperties);
        return sender;
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
     * 複製候選 sender，並把三種底層 socket 逾時縮到本次剩餘的整體時間內。
     * 不修改共用 sender，避免併發寄信互相覆寫 JavaMail 設定。
     */
    private static JavaMailSenderImpl senderForAttempt(JavaMailSenderImpl configured, long remainingMs) {
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
        capTimeout(properties, "mail.smtp.connectiontimeout", limit);
        capTimeout(properties, "mail.smtp.timeout", limit);
        capTimeout(properties, "mail.smtp.writetimeout", limit);
        sender.setJavaMailProperties(properties);
        return sender;
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
        long startedNanos = System.nanoTime();

        List<String> failureSummaries = new ArrayList<>();
        List<Throwable> failureCauses = new ArrayList<>();
        int attempted = 0;

        for (MailServerCandidate candidate : currentCandidates) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
            long remainingMs = overallTimeoutMs > 0 ? overallTimeoutMs - elapsedMs : Long.MAX_VALUE;
            if (attempted >= maxAttempts || remainingMs <= 0) {
                break;
            }
            attempted++;
            try {
                operation.send(senderForAttempt(candidate.sender(), remainingMs));
                log.info("郵件發送成功（{}），使用伺服器：{}", operationName, candidate.label());
                return;
            } catch (Exception e) {
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
     * @param mail 包含收件人、副本、主旨、HTML 內容及附件清單的郵件資料物件
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
            // 第二個參數 true 表示 text 的內容為 HTML；注意 <img/> 標籤中 src='cid:file'，
            // 'cid' 是 contentId 的縮寫，'file' 是一個識別標記，
            // 需在後續程式碼中呼叫 MimeMessageHelper 的 addInline 方法將其替換為實際檔案
            mimeMessageHelper.setText(mail.getMailContent(), true);

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
