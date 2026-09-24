---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `base-spring-boot-starter` 的所有可設定屬性，並提供完整的 `application.yml` 範例。

:::info 屬性前綴說明
本模組的屬性前綴為 **`mail`** 與 **`velocity`**，而非 `spring.mail` 或 `zipe.mail`。請確認設定正確的前綴，否則屬性不會生效。
:::

---

## 郵件設定屬性（前綴：`mail`）

郵件設定對應 `MailPropertyConfig`，所有屬性皆以 `mail` 為前綴。

| 屬性鍵 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `mail.host` | String | — | SMTP 伺服器主機名稱或 IP（未設定 `mail.servers` 時，作為唯一一組的來源） | 否 * |
| `mail.port` | String | — | SMTP 連接埠（例如 `587`、`465`、`25`） | 否 * |
| `mail.username` | String | — | SMTP 帳號 | 否 * |
| `mail.pa55word` | String | — | SMTP 密碼（欄位名稱為 `pa55word`，非 `password`） | 否 * |
| `mail.sender` | String | — | 預設寄件者電子郵件地址 | 否 |
| `mail.smtp-auth-enable` | Boolean | `true` | 是否啟用 SMTP 認證 | 否 |
| `mail.smtp-start-tls-enable` | Boolean | `false` | 是否啟用 STARTTLS 加密 | 否 |
| `mail.transport-protocol` | String | `"smtp"` | 傳輸協定 | 否 |
| `mail.encrypt-enable` | Boolean | `false` | 設為 `true` 時，`pa55word` 以 Base64 解碼後使用 | 否 |
| `mail.debug-enable` | Boolean | `false` | 啟用 JavaMail 除錯日誌 | 否 |
| `mail.servers` | `List<MailServerProperty>` | 空清單 | 多組 SMTP 伺服器（見下節）；設定後以此清單為準，上方單組欄位不再生效 | 否 |
| `mail.connection-timeout` | Integer（毫秒） | `5000` | SMTP 連線逾時，套用至每一組伺服器（含 `transport-protocol: smtps`，見下方說明） | 否 |
| `mail.read-timeout` | Integer（毫秒） | `3000` | SMTP 讀取逾時，套用至每一組伺服器（含 `transport-protocol: smtps`） | 否 |
| `mail.write-timeout` | Integer（毫秒） | `5000` | SMTP 寫入逾時，套用至每一組伺服器（含 `transport-protocol: smtps`） | 否 |
| `mail.failover.max-attempts` | Integer | `2147483647`（即不額外限制） | 單次發送最多嘗試的伺服器組數上限；實際上限為此值與已設定組數的較小者 | 否 |
| `mail.failover.overall-timeout` | Long（毫秒） | `30000` | 單次發送允許的整體切換時間上限；`0` 或負值表示不限制；`smtp` 與 `smtps` 兩種協定皆生效 | 否 |

:::info 連線／讀取／寫入逾時對 smtp 與 smtps 皆生效
三種底層逾時與整體切換時間上限對 `transport-protocol: smtp`（含 STARTTLS）與 `transport-protocol: smtps`（隱式 TLS，例如 465 埠）兩種協定皆會生效，不需為 smtps 額外設定。內部依該組實際協定將逾時值同時寫入對應的 JavaMail 屬性前綴，使用者只需維持既有 `mail.connection-timeout` 等單一設定鍵，不需依協定分別設定。
:::

:::caution 連線／讀取／寫入逾時為本次新增的行為，升級前無此限制
`mail.connection-timeout`（5000ms）、`mail.read-timeout`（3000ms）、`mail.write-timeout`（5000ms）
三者為**本次多 SMTP 容錯切換一併引入**的預設逾時上限；升級前的單組 SMTP 呼叫**未曾對這三個階段
設定任何逾時**，理論上會無限期等待底層 socket。若既有 SMTP 伺服器的網路延遲或處理時間**已經**
超過上述任一預設值（例如連線建立、等待回應或大附件寫入需要 5 秒以上），升級後可能因逾時而改用
下一組（或在未設定 `mail.servers` 時直接視為該唯一一組失敗），與升級前「無限等待直到成功或連線
逾時」的行為不同。若您的環境需要更長的等待時間，請明確調高對應的 `mail.*-timeout` 屬性。
:::

:::warning smtps（隱式 TLS）強制驗證伺服器憑證主機名稱
`transport-protocol: smtps` 的連線由內部截止機制接手建立與交握，交握時會依標準 HTTPS 規則驗證伺服器憑證的主機名稱（Subject Alternative Name／Common Name）是否與 `host` 設定相符，避免整體逾時機制在不知情下削弱既有的 TLS 安全保證。若目標 SMTP 伺服器使用自簽憑證或憑證未涵蓋設定的主機名稱，交握將會失敗並依容錯機制切換至下一組；請確認 `host` 與憑證上的主機名稱一致（建議使用網域名稱而非 IP），或改用信任鏈完整的正式憑證。
:::

\* 標示「否 *」者：`mail.servers` 未設定時，這幾個欄位仍為建立唯一一組 SMTP 所必須，僅是不再透過 `mail.servers` 表達。

:::note pa55word 欄位名稱
密碼欄位名稱為 `pa55word`（55 為數字），對應 `MailPropertyConfig.pa55word` 與 `MailServerProperty.pa55word`，請確認設定鍵拼寫正確。
:::

:::tip encrypt-enable 使用情境
`encrypt-enable: true` 適用於設定檔需要儲存密碼但又想避免明文的情境：先將 SMTP 密碼以 Base64 編碼後填入 `pa55word`，系統初始化時會自動以 Base64 解碼後再使用。**此為 Base64 編碼，並非加密**，不具備保密強度，僅用於避免設定檔直接明文儲存；請勿將其視為金鑰加密等保護機制。
:::

---

## 多組 SMTP 容錯切換（`mail.servers`）

自本版本起，郵件模組支援設定**多組 SMTP 伺服器**，任一組因連線、認證或傳輸失敗而無法送出時，
自動依清單順序改用下一組重新嘗試，直到成功或全部組別皆已嘗試為止（優先序 failover，非輪詢分流）。
五個發送方法（`sendEmail`、`simpleMailSend`、`attachedSend`、`richContentSend`、`sendBatchMailWithFile`）
與 `setInitData()` 皆套用此機制。

`mail.servers` 為 `MailServerProperty` 清單，每組可獨立設定：

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `mail.servers[i].name` | String | — | 伺服器識別名稱，僅用於日誌辨識，未設定時以 `host:port` 顯示 |
| `mail.servers[i].host` | String | — | 該組 SMTP 主機 |
| `mail.servers[i].port` | String | — | 該組 SMTP 連接埠 |
| `mail.servers[i].username` | String | — | 該組帳號 |
| `mail.servers[i].pa55word` | String | — | 該組密碼 |
| `mail.servers[i].smtp-auth-enable` | Boolean | `true` | 該組是否啟用 SMTP 認證 |
| `mail.servers[i].smtp-start-tls-enable` | Boolean | `false` | 該組是否啟用 STARTTLS |
| `mail.servers[i].transport-protocol` | String | `"smtp"` | 該組傳輸協定，可設為 `smtp`（含 STARTTLS）或 `smtps`（隱式 TLS，例如 465 埠）；同一清單可混用兩種協定，逾時與整體截止對兩者皆生效 |
| `mail.servers[i].encrypt-enable` | Boolean | `false` | 該組密碼是否為 Base64 編碼 |

:::info 未設定 mail.servers 時的向後相容行為
`mail.servers` 未設定（空清單）時，`MailPropertyConfig.resolveServers()` 會以上方單組扁平欄位
（`mail.host` / `mail.port` / `mail.username` / `mail.pa55word` 等）自動合成唯一一組候選，
既有僅設定單組 `mail.*` 屬性的使用者升級後**不需修改設定**即可維持原有行為。
:::

:::danger 已知取捨：至少一次投遞語意，可能重複收信
容錯切換為**「至少一次投遞」（at-least-once）語意**：若某組 SMTP 已接收郵件內容（DATA 階段之後）
才回報失敗，切換至下一組重送時，**收件者可能收到重複的郵件**。這是失敗即改用其他 SMTP 重送
所隱含、無法在應用層完全消除的機制本質限制，並非程式缺陷。若業務對重複投遞極度敏感，
請於收件端另行以訊息 ID／內容雜湊等方式做去重處理。
:::

---

## 執行緒池設定（靜態常數，目前不可由設定檔調整）

執行緒池由 `BaseAutoConfiguration.serviceJobTaskExecutor()` 建立，Bean 名稱為 `threadPoolTaskExecutor`。

| 參數 | 值 | 說明 |
|---|---|---|
| corePoolSize | `5` | 核心執行緒數 |
| maxPoolSize | `1000` | 最大執行緒數 |
| queueCapacity | `200` | 任務佇列容量 |
| keepAliveSeconds | `30000` | 閒置執行緒存活時間（秒） |
| waitForTasksToCompleteOnShutdown | `true` | 關閉時等待任務完成 |

:::note 覆蓋執行緒池
若需要自訂執行緒池參數，可在引用方宣告同名 Bean 覆蓋 Starter 的預設值：

```java
@Configuration
public class MyThreadPoolConfig {

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor customExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
```

此覆蓋機制依賴 `spring.main.allow-bean-definition-overriding: true`，此設定由 Starter 的 `application.yml` 預先啟用，無需手動設定。
:::

---

## Velocity 樣板設定（前綴：`velocity`）

Velocity 設定對應 `VelocityPropertyConfig`。

| 屬性鍵 | 型別 | 預設值（來源） | 說明 |
|---|---|---|---|
| `velocity.dir-path` | String | `"template"`（`resource.properties`） | Velocity 樣板存放目錄；`BaseAutoConfiguration` 以 classpath loader 模式初始化，此值作為模板根目錄路徑前綴 |

:::info 樣板路徑說明
`velocity.dir-path=template` 表示樣板應放置於 `src/main/resources/template/` 目錄下。呼叫 `velocityUtil.generateContent("mail/welcome.vm", model)` 時，實際解析路徑為 `classpath:template/mail/welcome.vm`。
:::

---

## 條件性 Bean：messageSource

`BaseAutoConfiguration` 有一個條件性 Bean：

```java
@Bean
@ConditionalOnResource(resources = "classpath:message.properties")
public MessageSource messageSource() { ... }
```

**含義：** 只有引用方 classpath 存在 `message.properties` 時，才會建立 `messageSource` Bean。

若引用方需要多語系支援，請在 `src/main/resources/` 下建立 `message.properties`（以及對應語系的 `message_zh_TW.properties` 等）。

---

## 完整 application.yml 範例

以下為包含本模組所有可設定屬性的完整範例，可直接複製後調整：

```yaml
# 郵件設定（MailPropertyConfig，前綴 mail）：單組 SMTP 寫法（向後相容，未設定 servers 時生效）
mail:
  host: smtp.example.com
  port: "587"
  username: noreply@example.com
  pa55word: ${MAIL_PASSWORD}          # 請以環境變數注入，避免明文存入版本控制
  sender: noreply@example.com
  smtp-auth-enable: true
  smtp-start-tls-enable: true
  transport-protocol: smtp
  encrypt-enable: false               # 設為 true 時，pa55word 以 Base64 解碼後使用
  debug-enable: false                 # 開發除錯時可設為 true，會輸出 SMTP 協定日誌

# Velocity 樣板設定（VelocityPropertyConfig，前綴 velocity）
velocity:
  dir-path: template                  # 樣板放置於 src/main/resources/template/
```

多組 SMTP 容錯切換寫法（設定 `mail.servers` 後，上方單組欄位不再生效）：

```yaml
mail:
  sender: noreply@example.com
  connection-timeout: 5000            # 逾時設定為全域，套用至每一組伺服器
  read-timeout: 3000
  write-timeout: 5000
  failover:
    max-attempts: 3                   # 單次發送最多嘗試 3 組（預設不額外限制，以已設定組數為準）
    overall-timeout: 15000            # 單次發送整體切換時間上限 15 秒（預設 30000）
  servers:
    - name: primary
      host: smtp1.example.com
      port: "587"
      username: noreply@example.com
      pa55word: ${MAIL_PASSWORD_PRIMARY}
      smtp-auth-enable: true
      smtp-start-tls-enable: true
    - name: backup
      host: smtp2.example.com
      port: "587"
      username: noreply-backup@example.com
      pa55word: ${MAIL_PASSWORD_BACKUP}
      smtp-auth-enable: true
      smtp-start-tls-enable: true
```

:::danger 重複寄送風險提醒
如上節「已知取捨」所述，容錯切換為至少一次投遞語意，`primary` 失敗改用 `backup` 重送時，
若 `primary` 其實已將郵件內容送達 SMTP 伺服器才回報失敗，收件者可能收到 2 封相同郵件。
:::

---

## 連接埠與 TLS 設定參考

| SMTP 服務商 | 連接埠 | TLS 設定 |
|---|---|---|
| Gmail | `587` | `transport-protocol: smtp`、`smtp-start-tls-enable: true` |
| Gmail（SSL） | `465` | `transport-protocol: smtps`，無需額外設定 |
| Outlook / Office 365 | `587` | `transport-protocol: smtp`、`smtp-start-tls-enable: true` |
| 一般企業 SMTP（無加密） | `25` | 均不啟用 |

:::warning 連接埠與加密
不同 SMTP 服務商使用的連接埠不同：`25`（未加密）、`587`（STARTTLS）、`465`（隱式 TLS）。請依服務商說明設定：`587` 埠搭配 `transport-protocol: smtp` 與 `smtp-start-tls-enable: true`；`465` 埠搭配 `transport-protocol: smtps`（連線逾時、整體截止與憑證主機名稱驗證皆已內建生效，不需額外設定 SSL factory）。
:::
