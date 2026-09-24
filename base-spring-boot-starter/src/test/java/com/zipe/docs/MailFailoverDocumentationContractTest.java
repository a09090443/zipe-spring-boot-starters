package com.zipe.docs;

import static org.assertj.core.api.Assertions.assertThat;

import com.zipe.service.MailService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * doc-site 文件語意契約測試，對應情境測試計畫 SC-030、SC-031、SC-032。
 *
 * <p>REQ-MAIL-FAILOVER-007／008／009／013／020 要求 doc-site 的文件敘述須與
 * 多組 SMTP 容錯切換的實際實作一致，且不得強於實作保證（例如宣稱 exactly-once 或
 * 保證中止在途投遞）。既有的 {@code qa-docs-install}／{@code qa-docs-build} 固定操作
 * 只驗證文件建置成功，無法讓「文件內容被弱化或改錯」的變更被攔截，因此本測試直接讀取
 * {@code doc-site/docs/base-starter/} 下的原始 Markdown 內容進行語意斷言。</p>
 */
class MailFailoverDocumentationContractTest {

    private static Path docsDir;

    @BeforeAll
    static void locateDocSiteDocs() {
        // 測試以模組根目錄（base-spring-boot-starter/）為工作目錄執行，doc-site 為其同層目錄。
        Path moduleDir = Paths.get("").toAbsolutePath();
        docsDir = moduleDir.resolveSibling("doc-site").resolve("docs").resolve("base-starter");
        assertThat(Files.isDirectory(docsDir))
                .as("doc-site/docs/base-starter 目錄應存在：%s", docsDir)
                .isTrue();
    }

    private static String read(String fileName) throws IOException {
        return Files.readString(docsDir.resolve(fileName), StandardCharsets.UTF_8);
    }

    /** AC-007-01：configuration.md 須明示至少一次投遞語意與重複寄送可能性。 */
    @Test
    void configuration_documentsAtLeastOnceDeliveryAndDuplicateRisk() throws IOException {
        String content = read("configuration.md");
        assertThat(content).contains("至少一次");
        assertThat(content).contains("重複");
        assertThat(content).contains("DATA");
    }

    /** AC-007-01／AC-007-04：不得反過來宣稱系統保證不重複投遞或保證中止在途連線。 */
    @Test
    void configuration_doesNotOverclaimExactlyOnceOrGuaranteedAbort() throws IOException {
        String content = read("configuration.md");
        assertThat(content).doesNotContainIgnoringCase("exactly-once");
        assertThat(content).doesNotContain("保證不會重複");
        assertThat(content).doesNotContain("保證不重複");
        assertThat(content).doesNotContain("一定不會重複");
        assertThat(content).doesNotContain("保證中止");
        assertThat(content).doesNotContain("保證已送達的郵件會被撤回");
    }

    /** AC-007-02：architecture.md 須說明 DATA 階段之後失敗切換會造成重複寄送的機制原因。 */
    @Test
    void architecture_explainsDataStageDuplicationMechanism() throws IOException {
        String content = read("architecture.md");
        assertThat(content).contains("DATA");
        assertThat(content).contains("重複");
        assertThat(content).contains("至少一次投遞語意");
    }

    /** AC-007-03：examples.md 須提供重複寄送風險的因應建議（呼叫端自行冪等控制）。 */
    @Test
    void examples_recommendsCallerSideIdempotencyMitigation() throws IOException {
        String content = read("examples.md");
        assertThat(content).contains("重複寄送風險");
        assertThat(content).containsAnyOf("冪等", "去重");
    }

    /**
     * AC-007-04：文件對整體逾時與在途連線的敘述須與實作一致——截止到期會關閉 socket 並讓呼叫端結束，
     * 但不保證背景在途的該次 SMTP 對話一定未送達（D7 取捨）。architecture.md 須說明「關閉 socket」機制，
     * 且不得宣稱一定能撤回已在途送出的內容。
     */
    @Test
    void architecture_describesSocketDeadlineWithoutOverclaimingCancellation() throws IOException {
        String content = read("architecture.md");
        assertThat(content).contains("關閉");
        assertThat(content).contains("socket");
        assertThat(content).doesNotContain("保證取消已送達");
        assertThat(content).doesNotContain("保證不會送達");
    }

    /** AC-008-06：configuration.md 須明示 overall-timeout 的預設值（30000），使既有使用者可預期。 */
    @Test
    void configuration_documentsOverallTimeoutDefault() throws IOException {
        String content = read("configuration.md");
        assertThat(content).contains("30000");
        assertThat(content).contains("overall-timeout");
    }

    /** AC-009-05：configuration.md 須明示 encrypt-enable 使用的 Base64 僅為編碼、並非加密。 */
    @Test
    void configuration_documentsBase64IsEncodingNotEncryption() throws IOException {
        String content = read("configuration.md");
        assertThat(content).contains("Base64");
        assertThat(content).containsAnyOf("並非加密", "非加密");
    }

    /** AC-013-01：configuration.md 的屬性表須涵蓋多組 SMTP 與容錯切換的全部設定鍵。 */
    @Test
    void configuration_propertyTableCoversAllFailoverKeys() throws IOException {
        String content = read("configuration.md");
        List<String> requiredKeys = List.of(
                "mail.servers",
                "mail.servers[i].name",
                "mail.servers[i].host",
                "mail.servers[i].port",
                "mail.servers[i].transport-protocol",
                "mail.servers[i].encrypt-enable",
                "mail.connection-timeout",
                "mail.read-timeout",
                "mail.write-timeout",
                "mail.failover.max-attempts",
                "mail.failover.overall-timeout");
        for (String key : requiredKeys) {
            assertThat(content).as("configuration.md 應記載屬性鍵 %s", key).contains(key);
        }
    }

    /**
     * AC-020-13：configuration.md 須說明逾時與整體上限對 smtp 與 smtps 兩種協定皆生效，
     * 且不得殘留「smtps 需要額外設定 SSL factory」等已修復前的錯誤敘述（回歸防護）。
     */
    @Test
    void configuration_documentsTimeoutsApplyToBothSmtpAndSmtps() throws IOException {
        String content = read("configuration.md");
        assertThat(content).contains("smtp");
        assertThat(content).contains("smtps");
        assertThat(content).containsAnyOf("皆生效", "皆會生效");
        assertThat(content).doesNotContain("smtps 需要額外設定");
        assertThat(content).doesNotContain("smtps 需另外設定 SSL");
    }

    /** AC-020-13／AC-013-02：architecture.md 須說明 smtp／smtps 協定前綴雙寫機制，且與實作一致。 */
    @Test
    void architecture_describesProtocolPrefixMechanismForSmtpAndSmtps() throws IOException {
        String content = read("architecture.md");
        assertThat(content).contains("protocolPrefix");
        assertThat(content).contains("mail.smtp.*");
        assertThat(content).contains("mail.smtps.*");
        assertThat(content).contains("DeadlineSocketFactory");
    }

    /** AC-013-02：architecture.md 說明不得比實作更強，仍須保留主機名稱驗證與明文 fallback 限制的正確敘述。 */
    @Test
    void architecture_describesHostnameVerificationAndNoPlaintextFallbackForSmtps() throws IOException {
        String content = read("architecture.md");
        assertThat(content).contains("主機名稱");
        assertThat(content).containsAnyOf("fallbackToPlainSocket", "退回明文 socket");
    }

    /**
     * SC-040：configuration.md 須明示連線／讀取／寫入逾時預設值為本次新增的行為，升級前無此限制，
     * 避免使用者誤以為套用預設值後與升級前行為完全相同（design 階段核實發現的既有文件缺口）。
     */
    @Test
    void configuration_documentsTimeoutDefaultsAreNewBehaviorNotPresentBeforeUpgrade() throws IOException {
        String content = read("configuration.md");
        assertThat(content).contains("升級前").contains("無此限制");
        assertThat(content).contains("mail.connection-timeout").contains("mail.read-timeout").contains("mail.write-timeout");
    }

    /**
     * SC-042：architecture.md 的 {@code Mail} 欄位表須涵蓋 {@code inlineResources}，並明確區分
     * 其與 {@code attachments} 的差異（{@code addInline} vs {@code addAttachment}），
     * 避免文件落後於 richContentSend 內嵌資源修正後的實際行為。
     */
    @Test
    void architecture_documentsInlineResourcesFieldDistinctFromAttachments() throws IOException {
        String content = read("architecture.md");
        assertThat(content).contains("inlineResources");
        assertThat(content).contains("addInline");
        assertThat(content).contains("addAttachment");
    }

    /** SC-041：examples.md 須提供 richContentSend 使用 inlineResources 產生真正內嵌資源的範例。 */
    @Test
    void examples_demonstratesInlineResourcesUsage() throws IOException {
        String content = read("examples.md");
        assertThat(content).contains("inlineResources");
        assertThat(content).contains("cid:");
    }

    /** AC-013-03：examples.md 的方法簽章（名稱與參數型別）須與 MailService 介面完全一致，不得使用已不存在的方法。 */
    @Test
    void examples_methodSignaturesMatchMailServiceInterface() throws IOException {
        String content = read("examples.md");
        for (Method method : MailService.class.getDeclaredMethods()) {
            String paramTypes = Arrays.stream(method.getParameterTypes())
                    .map(Class::getSimpleName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            String expectedSignature = method.getName() + "(" + paramTypes + ")";
            assertThat(content)
                    .as("examples.md 應包含與原始碼一致的方法簽章 %s", expectedSignature)
                    .contains(expectedSignature);
        }
    }

    /**
     * SC-042：index.md 須說明循序（非輪詢）容錯切換機制、彙整例外型別 MailFailoverException，
     * 以及 MailService Bean 可由業務系統以 ConditionalOnMissingBean 語意覆寫，
     * 使概覽頁與 architecture.md 對這三項描述一致，不遺漏於其中一份文件。
     */
    @Test
    void index_describesSequentialFailoverMechanismExceptionTypeAndBeanOverride() throws IOException {
        String content = read("index.md");
        assertThat(content).contains("依序").contains("容錯切換");
        assertThat(content).contains("MailFailoverException");
        assertThat(content).containsAnyOf("ConditionalOnMissingBean", "覆寫");
    }

    /**
     * SC-043：quickstart.md 的最小設定步驟須與現行多組 SMTP 屬性一致，且不得殘留
     * 「僅支援單組 SMTP」等已被多組容錯切換取代的過時敘述，避免使用者誤以為無法設定多組伺服器。
     */
    @Test
    void quickstart_mentionsMultiServerOption_withoutClaimingSingleSmtpOnly() throws IOException {
        String content = read("quickstart.md");
        assertThat(content).contains("mail.servers");
        assertThat(content).doesNotContain("僅支援單組 SMTP");
        assertThat(content).doesNotContain("只支援單一 SMTP");
        assertThat(content).doesNotContain("只支援單組 SMTP");
        assertThat(content).doesNotContain("僅支援單一 SMTP");
    }

    /**
     * SC-044：根目錄 README 的模組清單與快速開始段落須與多組 SMTP 容錯切換的實作一致，
     * 涵蓋向後相容與至少一次投遞風險的引導說明，避免版控最外層文件落後於 doc-site。
     */
    @Test
    void rootReadme_describesMultiSmtpFailoverConsistentWithImplementation() throws IOException {
        Path moduleDir = Paths.get("").toAbsolutePath();
        Path rootReadme = moduleDir.resolveSibling("README.md");
        assertThat(Files.isRegularFile(rootReadme)).as("根目錄 README.md 應存在：%s", rootReadme).isTrue();
        String content = Files.readString(rootReadme, StandardCharsets.UTF_8);
        assertThat(content).contains("多組 SMTP").contains("容錯");
        assertThat(content).contains("向後相容");
    }

    /**
     * SC-044：base-spring-boot-starter 模組自身 README 的功能概述與基本設定範例須與多組 SMTP
     * 容錯切換的實作一致，且指向 configuration.md 取得完整屬性與重複寄送風險說明。
     */
    @Test
    void baseStarterReadme_describesMailFailoverFeatureConsistentWithImplementation() throws IOException {
        Path moduleDir = Paths.get("").toAbsolutePath();
        Path moduleReadme = moduleDir.resolve("README.md");
        assertThat(Files.isRegularFile(moduleReadme)).as("base-spring-boot-starter/README.md 應存在：%s", moduleReadme)
                .isTrue();
        String content = Files.readString(moduleReadme, StandardCharsets.UTF_8);
        assertThat(content).contains("多組 SMTP").contains("容錯切換");
        assertThat(content).contains("mail.servers");
    }

    /**
     * SC-045：MailServiceImpl 類別層級註解須揭露「DATA 階段之後失敗切換可能造成重複投遞」的
     * 至少一次投遞語意，且不得反過來宣稱保證不重複（exactly-once），
     * 避免文件字面退化為過度承諾而誤導呼叫端省略冪等處理。
     */
    @Test
    void mailServiceImpl_classJavadocDisclosesDataStageDuplicationRisk_withoutOverclaimingExactlyOnce()
            throws IOException {
        Path moduleDir = Paths.get("").toAbsolutePath();
        Path sourceFile = moduleDir.resolve("src/main/java/com/zipe/service/impl/MailServiceImpl.java");
        assertThat(Files.isRegularFile(sourceFile)).as("MailServiceImpl.java 應存在：%s", sourceFile).isTrue();
        String content = Files.readString(sourceFile, StandardCharsets.UTF_8);
        String classJavadoc = content.substring(0, content.indexOf("public class MailServiceImpl"));

        assertThat(classJavadoc).contains("DATA");
        assertThat(classJavadoc).contains("重複");
        assertThat(classJavadoc).doesNotContainIgnoringCase("exactly-once");
        assertThat(classJavadoc).doesNotContain("保證不會重複");
        assertThat(classJavadoc).doesNotContain("保證不重複");
        assertThat(classJavadoc).doesNotContain("一定不會重複");
    }
}
