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
     * SC-041：examples.md 的重複寄送因應建議須具體到「業務層識別碼」層級（例如訊息 ID 或業務單號），
     * 而非只出現「冪等」「去重」等詞面（review 節點確認的既有缺口：舊版斷言只搜尋詞面，
     * 未核對是否提出具體去重依據）。
     */
    @Test
    void examples_recommendsIdempotencyMitigationWithConcreteBusinessIdentifier() throws IOException {
        String content = read("examples.md");
        assertThat(content).as("應具體建議以訊息 ID 作為去重依據").contains("訊息 ID");
        assertThat(content).as("應具體建議以業務單號作為去重依據").contains("業務單號");
        assertThat(content).as("應明示 MailService 本身不提供去重機制，去重責任在呼叫端").contains("去重機制");
    }

    /**
     * SC-041：examples.md 須提供設定「兩組以上具名 SMTP」與 failover 上限的實際 YAML 範例，
     * 而非只描述文字或單組設定（review 節點確認的既有缺口：舊版測試只搜尋簽章字串，
     * 未核對多組 YAML 範例是否存在）。
     */
    @Test
    void examples_demonstratesMultiServerFailoverYamlWithTwoNamedServers() throws IOException {
        String content = read("examples.md");
        assertThat(content).as("應示範第一組伺服器 name: primary").contains("name: primary");
        assertThat(content).as("應示範第二組伺服器 name: backup").contains("name: backup");
        assertThat(content).as("應示範 failover.max-attempts 設定").contains("max-attempts:");
        assertThat(content).as("應示範 failover.overall-timeout 設定").contains("overall-timeout:");
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
                "mail.servers[i].username",
                "mail.servers[i].pa55word",
                "mail.servers[i].smtp-auth-enable",
                "mail.servers[i].smtp-start-tls-enable",
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
     * SC-040：configuration.md 每組 SMTP 屬性表須逐列記載型別與預設值，且不得只驗證鍵名存在——
     * 刪除任一列（例如 smtp-auth-enable）仍須被本測試攔截（review 節點確認的既有缺口：
     * 舊版 requiredKeys 只檢查鍵名字串，刪除整列表格仍可通過）。
     *
     * <p>transport-protocol 一列比照其餘欄位一併納入完整列（含說明欄）比對，而非只核對到
     * 預設值欄為止：review 節點確認的既有缺口——舊版斷言只檢查到型別與預設值的表格前綴
     * （{@code | String | "smtp" |}），刪除該列的說明文字（僅保留前綴）仍可通過，
     * 無法證明說明欄本身也受文件契約保護。</p>
     */
    @Test
    void configuration_perServerFieldsTableDocumentsExactTypeAndDefault() throws IOException {
        String content = read("configuration.md");
        List<String> requiredRows = List.of(
                "| `mail.servers[i].name` | String | — | 伺服器識別名稱，僅用於日誌辨識，未設定時以 `host:port` 顯示 |",
                "| `mail.servers[i].host` | String | — | 該組 SMTP 主機 |",
                "| `mail.servers[i].port` | String | — | 該組 SMTP 連接埠 |",
                "| `mail.servers[i].username` | String | — | 該組帳號 |",
                "| `mail.servers[i].pa55word` | String | — | 該組密碼 |",
                "| `mail.servers[i].smtp-auth-enable` | Boolean | `true` | 該組是否啟用 SMTP 認證 |",
                "| `mail.servers[i].smtp-start-tls-enable` | Boolean | `false` | 該組是否啟用 STARTTLS |",
                "| `mail.servers[i].encrypt-enable` | Boolean | `false` | 該組密碼是否為 Base64 編碼 |",
                "| `mail.servers[i].transport-protocol` | String | `\"smtp\"` | 該組傳輸協定，可設為 `smtp`"
                        + "（含 STARTTLS）或 `smtps`（隱式 TLS，例如 465 埠）；同一清單可混用兩種協定，"
                        + "逾時與整體截止對兩者皆生效 |");
        for (String row : requiredRows) {
            assertThat(content).as("configuration.md 應逐列記載型別與預設值：%s", row).contains(row);
        }
    }

    /**
     * SC-040：configuration.md 頂層屬性表須逐列記載 {@code mail.servers} 本身、三種底層逾時
     * 與兩種 failover 設定的型別與預設值，不得只驗證鍵名存在（review 節點確認的既有缺口：
     * 舊版 requiredKeys 只涵蓋八個每組欄位的逐列檢查，mail.servers、connection/read/write-timeout、
     * failover.max-attempts、failover.overall-timeout 仍只有鍵名字串斷言，刪除任一列表格
     * 仍可通過）。
     */
    @Test
    void configuration_topLevelFailoverFieldsTableDocumentsExactTypeAndDefault() throws IOException {
        String content = read("configuration.md");
        List<String> requiredRows = List.of(
                "| `mail.servers` | `List<MailServerProperty>` | 空清單 | 多組 SMTP 伺服器（見下節）；設定後以此清單為準，上方單組欄位不再生效 | 否 |",
                "| `mail.connection-timeout` | Integer（毫秒） | `5000` | SMTP 連線逾時，套用至每一組伺服器（含 `transport-protocol: smtps`，見下方說明） | 否 |",
                "| `mail.read-timeout` | Integer（毫秒） | `3000` | SMTP 讀取逾時，套用至每一組伺服器（含 `transport-protocol: smtps`） | 否 |",
                "| `mail.write-timeout` | Integer（毫秒） | `5000` | SMTP 寫入逾時，套用至每一組伺服器（含 `transport-protocol: smtps`） | 否 |",
                "| `mail.failover.max-attempts` | Integer | `2147483647`（即不額外限制） | 單次發送最多嘗試的伺服器組數上限；實際上限為此值與已設定組數的較小者 | 否 |",
                "| `mail.failover.overall-timeout` | Long（毫秒） | `30000` | 單次發送允許的整體切換時間上限；`0` 或負值表示不限制；`smtp` 與 `smtps` 兩種協定皆生效 | 否 |");
        for (String row : requiredRows) {
            assertThat(content).as("configuration.md 應逐列記載型別與預設值：%s", row).contains(row);
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
     * SC-042：architecture.md 須明確說明容錯切換為「依候選清單順序」的循序（非輪詢）機制，
     * 與 index.md 的概覽敘述分開驗證，避免只有其中一份文件涵蓋此描述
     * （review 節點確認的既有缺口：舊版 architecture 測試未對此三項分別斷言）。
     */
    @Test
    void architecture_describesSequentialNonPollingFailoverOrder() throws IOException {
        String content = read("architecture.md");
        assertThat(content).as("architecture.md 應說明依候選清單順序嘗試").contains("依候選清單順序");
        assertThat(content).as("architecture.md 應明示為優先序 failover").contains("優先序 failover");
        assertThat(content).as("architecture.md 應明示非輪詢分流").contains("非輪詢");
    }

    /**
     * SC-042：architecture.md 須明確說明彙整例外型別 MailFailoverException 的性質
     * （繼承 Spring MailException、為 unchecked、以 addSuppressed 掛載各組原始例外）。
     */
    @Test
    void architecture_describesMailFailoverExceptionType() throws IOException {
        String content = read("architecture.md");
        assertThat(content).contains("MailFailoverException");
        assertThat(content).as("應說明繼承 Spring MailException").contains("繼承 Spring");
        assertThat(content).as("應說明為 unchecked").contains("unchecked");
        assertThat(content).as("應說明以 addSuppressed 掛載各組原始例外").contains("addSuppressed");
    }

    /**
     * SC-042：architecture.md 須明確說明 MailService Bean 可由業務系統以 ConditionalOnMissingBean
     * 語意整顆覆寫（含實際覆寫程式碼範例），而非只提及「可覆寫」一詞帶過。
     */
    @Test
    void architecture_describesBeanOverrideMechanismWithExample() throws IOException {
        String content = read("architecture.md");
        assertThat(content).as("應有「覆蓋 Starter 的 Bean」章節").contains("覆蓋 Starter 的 Bean");
        assertThat(content).contains("@ConditionalOnMissingBean");
        assertThat(content).as("應提供覆寫 MailService Bean 的實際程式碼範例").contains("public MailService mailService()");
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
     * SC-043：quickstart.md 的最小設定步驟須逐鍵記載扁平 mail.* 必要欄位與完整五步驟，
     * 而非只驗證出現 mail.servers 字樣與排除過時字句
     * （review 節點確認的既有缺口：舊版測試未驗證最小設定鍵與步驟本身）。
     */
    @Test
    void quickstart_minimalSetupStepsAndFlatMailKeysAreDocumented() throws IOException {
        String content = read("quickstart.md");
        List<String> requiredSteps = List.of(
                "## Step 1：安裝模組", "## Step 2：加入依賴", "## Step 3：設定 application.yml",
                "## Step 4：程式碼範例", "## Step 5：執行驗證");
        for (String step : requiredSteps) {
            assertThat(content).as("quickstart.md 應包含步驟標題 %s", step).contains(step);
        }
        List<String> requiredMinimalKeys =
                List.of("host: smtp.example.com", "port: 587", "username: noreply@example.com",
                        "pa55word: ${MAIL_PASSWORD}", "smtp-auth-enable: true");
        for (String key : requiredMinimalKeys) {
            assertThat(content).as("quickstart.md 最小設定範例應包含 %s", key).contains(key);
        }
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
