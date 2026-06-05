---
id: quickstart
title: 快速開始
sidebar_position: 2
---

# 快速開始

本頁將帶您在五個步驟內完成 `base-spring-boot-starter` 的引入，並以郵件發送功能進行驗證。

## 前置需求

- 已安裝 JDK 17 以上版本。
- 業務專案使用 Spring Boot 3.5.x。
- 已具備可用的 Maven 環境。

:::note
請先確認本機 Maven Repository 已存在本模組，否則步驟二的依賴解析會失敗。
:::

## Step 1：安裝模組

進入 `base-spring-boot-starter` 目錄，將模組安裝至本地 Maven Repository：

```bash
cd base-spring-boot-starter
./mvnw clean install -DskipTests
```

看到 `BUILD SUCCESS` 即表示安裝完成。

## Step 2：加入依賴

於業務專案的 `pom.xml` 加入依賴：

```xml
<dependencies>
    <dependency>
        <groupId>com.zipe</groupId>
        <artifactId>base-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Step 3：設定 application.yml

若要使用郵件功能，於 `application.yml` 設定 SMTP 連線資訊。本模組的郵件設定前綴為 `mail`（非 `spring.mail`）：

```yaml
mail:
  host: smtp.example.com
  port: 587
  username: noreply@example.com
  pa55word: ${MAIL_PASSWORD}      # 對應欄位名稱為 pa55word
  smtp-auth-enable: true
  smtp-start-tls-enable: true
  sender: noreply@example.com     # 預設寄件者地址
  transport-protocol: smtp
  debug-enable: false
  encrypt-enable: false           # 設為 true 時，pa55word 以 Base64 解碼後使用

velocity:
  dir-path: template              # Velocity 樣板放置目錄，預設為 classpath 下的 template/
```

:::warning 密碼安全
請勿將 SMTP 密碼直接硬寫在版本控制的設定檔中。建議透過環境變數（`${MAIL_PASSWORD}`）或外部化設定管理機敏資訊。
:::

## Step 4：程式碼範例

注入 `MailService` 並發送一封測試郵件。**注意：使用郵件功能前必須先呼叫 `setInitData()` 初始化 SMTP 連線。**

```java
import com.zipe.model.Mail;
import com.zipe.service.MailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailDemoController {

    @Autowired
    private MailService mailService;

    @GetMapping("/send-test-mail")
    public String sendTestMail() throws MessagingException {
        // 每次使用前初始化（建議在 @PostConstruct 統一呼叫一次）
        mailService.setInitData();

        Mail mail = new Mail();
        mail.setMailTo(new String[]{"user@example.com"});
        mail.setMailSubject("測試郵件");
        mail.setMailContent("這是一封由 base-spring-boot-starter 發送的測試郵件。");
        // contentType 預設 "text/plain"，HTML 郵件改設為 "text/html"

        mailService.simpleMailSend(mail);
        return "mail sent";
    }
}
```

也可以直接使用無狀態的加解密工具（不需要任何 Spring 注入）：

```java
import com.zipe.util.crypto.AesUtil;

public class CryptoDemo {
    public static void main(String[] args) {
        // secretKey 必須恰好 16 個 ASCII 字元（AES-128）
        AesUtil aesUtil = new AesUtil("testtesttesttest");
        String encrypted = aesUtil.getEncrypt("hello world", "UTF-8");
        String decrypted = aesUtil.getDecode(encrypted, "UTF-8");
        System.out.println(decrypted); // hello world
    }
}
```

## Step 5：執行驗證

啟動應用程式並呼叫測試端點：

```bash
./mvnw spring-boot:run
curl http://localhost:8080/send-test-mail
```

若回傳 `mail sent` 且收件匣收到郵件，即表示模組已正確運作。

:::tip 驗證加解密
若暫時沒有可用的 SMTP 伺服器，可先以加解密工具進行驗證，因其為純靜態方法呼叫，不需要任何外部連線或 Spring Context。
:::

:::tip 建議的初始化模式
在需要使用郵件功能的 Service 中，透過 `@PostConstruct` 統一呼叫 `setInitData()`，可以在應用啟動時提前發現 SMTP 連線問題，避免執行期才拋出例外：

```java
@Service
public class NotificationService {

    @Autowired
    private MailService mailService;

    @PostConstruct
    public void init() throws MessagingException {
        mailService.setInitData();
    }
}
```
:::
