---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁提供 `base-spring-boot-starter` 各項工具的實際程式碼範例，涵蓋基礎使用、進階情境與常見問題。

## 基礎使用範例

### 加解密與雜湊

```java
import com.zipe.util.crypto.AesUtil;
import com.zipe.util.crypto.Md5Util;
import com.zipe.util.crypto.Base64Util;

public class CryptoExample {

    public void demo() {
        // AES 加解密
        String key = "0123456789abcdef0123456789abcdef";
        String cipher = AesUtil.encrypt("敏感資料", key);
        String plain = AesUtil.decrypt(cipher, key);

        // MD5 雜湊（常用於密碼比對）
        String hash = Md5Util.encode("password123");

        // Base64 編解碼
        String encoded = Base64Util.encode("hello".getBytes());
        byte[] decoded = Base64Util.decode(encoded);
    }
}
```

### 字串與日期工具

```java
import com.zipe.util.string.CommonStringUtil;
import com.zipe.util.string.RandomUtil;
import com.zipe.util.time.DateTimeUtils;

public class UtilExample {

    public void demo() {
        boolean blank = CommonStringUtil.isEmpty("   ");
        String code = RandomUtil.randomNumeric(6); // 產生 6 碼數字
        String now = DateTimeUtils.format(new java.util.Date(), "yyyy-MM-dd HH:mm:ss");
    }
}
```

## 進階使用範例

### 以 Velocity 樣板發送 HTML 郵件

先在 `src/main/resources/templates/welcome.vm` 建立樣板：

```text
<h1>歡迎 $userName！</h1>
<p>您的啟用碼為：$activationCode</p>
```

接著於程式中套版並發送：

```java
import com.zipe.model.Mail;
import com.zipe.service.MailService;
import com.zipe.util.VelocityUtil;
import java.util.HashMap;
import java.util.Map;

public class TemplateMailExample {

    private final MailService mailService;

    public TemplateMailExample(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendWelcome(String to, String userName, String code) {
        Map<String, Object> model = new HashMap<>();
        model.put("userName", userName);
        model.put("activationCode", code);
        String html = VelocityUtil.render("welcome.vm", model);

        Mail mail = new Mail();
        mail.setTo(to);
        mail.setSubject("歡迎加入");
        mail.setContent(html);
        mail.setHtml(true);
        mailService.send(mail);
    }
}
```

### 使用 OkHttpUtil 呼叫外部 API

```java
import com.zipe.util.http.OkHttpUtil;
import java.util.HashMap;
import java.util.Map;

public class HttpExample {

    public String callApi() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        return OkHttpUtil.get("https://api.example.com/data", headers);
    }
}
```

## 常見情境

### 情境一：匯出 Excel 報表

使用 `ExcelUtil` 搭配 Annotation 將物件清單匯出為 Excel：

```java
import com.zipe.util.doc.ExcelUtil;
import java.util.List;

public class ExportExample {
    public void export(List<User> users, java.io.OutputStream out) {
        ExcelUtil.export(users, User.class, out);
    }
}
```

### 情境二：非同步發送大量郵件

模組已配置執行緒池，搭配 `@Async` 即可非同步發送，避免阻塞主執行緒：

```java
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BatchMailService {

    @Async
    public void sendInBackground(Mail mail) {
        mailService.send(mail);
    }
}
```

### 情境三：取得 Spring 容器中的 Bean

在非 Spring 管理的類別中取得 Bean：

```java
import com.zipe.util.ApplicationContextHelper;

public class LegacyComponent {
    public void doWork() {
        MailService service = ApplicationContextHelper.getBean(MailService.class);
        // ...
    }
}
```

## 常見問題

- **郵件發送失敗並拋出 `AuthenticationFailedException`**：請檢查 `spring.mail.username` 與 `password` 是否正確，部分服務商需使用「應用程式密碼」而非登入密碼。
- **Velocity 找不到樣板**：確認樣板檔位於 `resource-loader-path` 指定的目錄，且副檔名與程式中傳入的名稱一致。
- **AES 解密拋出例外**：請確認加解密使用相同的 key，且 key 長度符合所選 AES 模式（128 為 16 位元組、256 為 32 位元組）。

:::tip 最佳實踐
無狀態工具（加解密、字串、日期）建議直接以靜態方法呼叫，不需注入；有狀態服務（如 `MailService`）則一律透過 Spring 注入，以確保設定與生命週期由容器統一管理。
:::
