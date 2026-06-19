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
| `mail.host` | String | — | SMTP 伺服器主機名稱或 IP | 是 |
| `mail.port` | String | — | SMTP 連接埠（例如 `587`、`465`、`25`） | 是 |
| `mail.username` | String | — | SMTP 帳號 | 是 |
| `mail.pa55word` | String | — | SMTP 密碼（欄位名稱為 `pa55word`，非 `password`） | 是 |
| `mail.sender` | String | — | 預設寄件者電子郵件地址 | 是 |
| `mail.smtp-auth-enable` | Boolean | `true` | 是否啟用 SMTP 認證 | 否 |
| `mail.smtp-start-tls-enable` | Boolean | `false` | 是否啟用 STARTTLS 加密 | 否 |
| `mail.transport-protocol` | String | `"smtp"` | 傳輸協定 | 否 |
| `mail.encrypt-enable` | Boolean | `false` | 設為 `true` 時，`pa55word` 以 Base64 解碼後使用 | 否 |
| `mail.debug-enable` | Boolean | `false` | 啟用 JavaMail 除錯日誌 | 否 |

:::note pa55word 欄位名稱
密碼欄位名稱為 `pa55word`（55 為數字），對應 `MailPropertyConfig.pa55word`，請確認設定鍵拼寫正確。
:::

:::tip encrypt-enable 使用情境
`encrypt-enable: true` 適用於設定檔需要儲存密碼但又想避免明文的情境：先將 SMTP 密碼以 Base64 編碼後填入 `pa55word`，系統初始化時會自動以 Base64 解碼後再使用。
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
# 郵件設定（MailPropertyConfig，前綴 mail）
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

---

## 連接埠與 TLS 設定參考

| SMTP 服務商 | 連接埠 | TLS 設定 |
|---|---|---|
| Gmail | `587` | `smtp-start-tls-enable: true` |
| Gmail（SSL） | `465` | 需額外設定 SSL factory |
| Outlook / Office 365 | `587` | `smtp-start-tls-enable: true` |
| 一般企業 SMTP（無加密） | `25` | 均不啟用 |

:::warning 連接埠與加密
不同 SMTP 服務商使用的連接埠不同：`25`（未加密）、`587`（STARTTLS）、`465`（SSL）。請依服務商說明設定，並務必開啟 `smtp-start-tls-enable: true` 以保護傳輸安全。
:::
