---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁提供 `base-spring-boot-starter` 各項工具的實際程式碼範例，涵蓋基礎使用、進階情境與常見問題。所有範例的方法簽章均與原始碼一致。

:::info 設計慣例
- **無狀態工具**（字串、日期、`Md5Util`）以 `static` 方法呼叫。
- **加解密工具**（`AesUtil`、`Base64Util`、`DESedeUtil`）為**實例方法**，需先 `new` 出物件再呼叫 `getEncrypt` / `getDecode`，因為它們持有金鑰狀態。
- **有狀態服務**（`MailService`）一律透過 Spring 注入使用。
:::

## 基礎使用範例

### 加解密與雜湊

`AesUtil` 採 **AES-128/CBC/PKCS5Padding**，建構時傳入金鑰，再以 `getEncrypt` / `getDecode` 進行加解密。每次加密會產生**隨機 IV**，密文格式為 `Base64(IV ‖ ciphertext)`（前 16 bytes 為 IV）：

:::warning 密文格式變更（不相容舊版）
自此版本起，`AesUtil` 改為每次使用隨機 IV，密文開頭多出 16-byte IV。**舊版（以金鑰當固定 IV、無 IV 前綴）所加密的字串與檔案無法用新版解密**，需先以舊版程式解密後再用新版重新加密。相同明文每次加密也會產生不同密文（這是正確的安全特性）。
:::

```java
import com.zipe.util.crypto.AesUtil;
import com.zipe.util.crypto.Base64Util;

public class CryptoExample {

    public void demo() throws Exception {
        // AES-128 加解密（實例方法，建構子傳入金鑰）
        AesUtil aes = new AesUtil("0123456789abcdef"); // 金鑰長度 16 位元組
        String cipher = aes.getEncrypt("敏感資料");      // 每次結果不同（隨機 IV）
        String plain = aes.getDecode(cipher);

        // Base64 編解碼（實例方法，以字串為單位）
        Base64Util base64 = new Base64Util();
        String encoded = base64.getEncrypt("hello");
        String decoded = base64.getDecode(encoded);
    }
}
```

:::danger Md5Util 已棄用
`Md5Util` 已標註 `@Deprecated`。MD5 不具抗碰撞性，**禁止用於密碼雜湊或資料完整性／簽章**。密碼請改用 BCrypt／Argon2，完整性驗證請用 SHA-256 以上。僅可用於非安全用途（如產生快取鍵）。
:::

:::tip 策略模式：CryptoUtil
`CryptoUtil` 以建構子注入任一 `Crypto` 實作（`AesUtil`、`Base64Util`、`DESedeUtil` 皆實作 `Crypto` 介面），呼叫端不需關心底層演算法：

```java
import com.zipe.util.crypto.CryptoUtil;
import com.zipe.util.crypto.Base64Util;

CryptoUtil cryptoUtil = new CryptoUtil(new Base64Util());
String enc = cryptoUtil.getEncrypt("text");
String dec = cryptoUtil.getDecode(enc);
```
:::

### 加解密檔案

`AesUtil` 也支援檔案層級加解密：

```java
import com.zipe.util.crypto.AesUtil;
import java.io.File;

AesUtil aes = new AesUtil("0123456789abcdef");
aes.encryptFile(new File("plain.txt"), new File("cipher.dat"));
aes.decryptFile(new File("cipher.dat"), new File("decrypted.txt"));
```

### 字串工具

```java
import com.zipe.util.string.CommonStringUtil;
import com.zipe.util.string.RandomUtil;

public class StringExample {

    public void demo() {
        boolean isNum = CommonStringUtil.isNumber("12345");        // 是否為數字
        String padded = CommonStringUtil.addZero(7, 4);            // "0007"
        String leftPad = CommonStringUtil.stringLeftPad("ab", 5);  // 左補空白至長度 5
        String camel = CommonStringUtil.lowerCaseForFirstLetter("UserName"); // "userName"

        // 以 SecureRandom 產生亂數字串
        String mix = RandomUtil.generateStr(8);       // 含大小寫英數
        String digits = RandomUtil.generateZeroStr(6); // 純數字
        String lower = RandomUtil.generateLowerStr(6); // 純小寫英文
    }
}
```

### 日期時間工具

`DateTimeUtils` 預先定義 18 組 `DateTimeFormatter` 常數（`dateTimeFormate1`～`dateTimeFormate18`），搭配各 `getMinusOrPlusXxx` 方法進行日期運算：

```java
import com.zipe.util.time.DateTimeUtils;
import java.time.LocalDateTime;

public class DateExample {

    public void demo() {
        // 取得目前時間字串（yyyy-MM-dd HH:mm:ss）
        String now = DateTimeUtils.getDateNow(DateTimeUtils.dateTimeFormate1);

        // 取得目前 LocalDateTime 物件
        LocalDateTime dateTime = DateTimeUtils.getDateNow();

        // 從今天往後加 7 天，並以 yyyy-MM-dd 格式輸出
        String next7Days = DateTimeUtils.getMinusOrPlusDays(7, DateTimeUtils.dateTimeFormate2);

        // 從今天往前推 1 個月
        String lastMonth = DateTimeUtils.getMinusOrPlusMonths(-1, DateTimeUtils.dateTimeFormate6);

        // 將字串解析為 LocalDateTime
        LocalDateTime parsed = DateTimeUtils.getDateTime("2026-06-05 10:30:00", DateTimeUtils.dateTimeFormate1);
    }
}
```

## 進階使用範例

### 發送郵件

`MailService` 為 Spring Bean，需先呼叫 `setInitData()` 依設定建立 `JavaMailSenderImpl`，再選擇對應的發送方法。`Mail` 物件的收件人欄位為**字串陣列**：

```java
import com.zipe.model.Mail;
import com.zipe.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotifyService {

    @Autowired
    private MailService mailService;

    public void sendPlainText() throws Exception {
        Mail mail = new Mail();
        mail.setMailTo(new String[]{"user@example.com"});
        mail.setMailSubject("系統通知");
        mail.setMailContent("您的申請已核准。");

        mailService.setInitData();      // 依 mail.* 設定初始化寄件者
        mailService.simpleMailSend(mail); // 純文字
    }

    public void sendHtmlWithAttachment(java.io.File report) throws Exception {
        Mail mail = new Mail();
        mail.setMailTo(new String[]{"user@example.com"});
        mail.setMailCc(new String[]{"manager@example.com"});
        mail.setMailSubject("月報表");
        mail.setMailContent("<h3>請參閱附件月報表</h3>");
        mail.setAttachments(java.util.List.of(report));

        mailService.setInitData();
        mailService.attachedSend(mail); // HTML + 附件
    }
}
```

| 方法 | 用途 |
|---|---|
| `simpleMailSend(Mail)` | 純文字郵件 |
| `sendEmail(Mail)` | MIME 郵件（支援 HTML） |
| `attachedSend(Mail)` | 帶附件郵件 |
| `richContentSend(Mail)` | 內嵌圖片等多媒體內容 |
| `sendBatchMailWithFile(Mail)` | 批次發送含附件郵件 |

### 以 Velocity 樣板產生內容

`VelocityUtil` 提供四種模板載入模式（classpath / file / filesystem / web）。以 classpath 模式為例，模板放於 `src/main/resources/template/mail/welcome.vm`：

```text
<h1>歡迎 $userName！</h1>
<p>您的啟用碼為：$activationCode</p>
```

```java
import com.zipe.util.VelocityUtil;
import java.util.HashMap;
import java.util.Map;

public class TemplateExample {

    public String renderWelcome(String userName, String code) {
        Map<String, Object> model = new HashMap<>();
        model.put("userName", userName);
        model.put("activationCode", code);

        VelocityUtil velocityUtil = new VelocityUtil();
        velocityUtil.initClassPath();                              // classpath 載入模式
        String html = velocityUtil.generateContent("mail/welcome.vm", model);
        velocityUtil.close();
        return html;
    }
}
```

:::note 模板路徑
classpath 模式下模板根目錄由 `velocity.dir-path` 決定（預設 `template`），故傳入 `"mail/welcome.vm"` 對應 `template/mail/welcome.vm`。
:::

### 使用 OkHttpUtil 呼叫外部 API

`OkHttpUtil` 採單例設計，透過 `getInstance()` 取得實例，回傳 OkHttp 原生 `Response`：

```java
import com.zipe.util.http.OkHttpUtil;
import okhttp3.Response;
import java.util.HashMap;
import java.util.Map;

public class HttpExample {

    public String callGet() throws Exception {
        try (Response response = OkHttpUtil.getInstance().getData("https://api.example.com/data")) {
            return response.body().string();
        }
    }

    public String callPost() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("account", "admin");
        body.put("password", "secret");
        try (Response response = OkHttpUtil.getInstance().postData("https://api.example.com/login", body)) {
            return response.body().string();
        }
    }
}
```

非同步呼叫則使用 `getDataAsyn` / `postDataAsyn`，傳入 `NetCall` 回呼處理結果。

## 常見情境

### 情境一：匯出 / 匯入 Excel

`ExcelUtil` 提供多種匯出多載。以 `Map<String, String>`（屬性名→欄位標題）搭配資料集合匯出最為常用：

```java
import com.zipe.util.doc.ExcelUtil;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelExportExample {

    public void export(List<User> users, OutputStream out) {
        // key = 物件屬性名，value = Excel 欄位標題（LinkedHashMap 保留欄位順序）
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("id", "編號");
        headers.put("name", "姓名");
        headers.put("email", "電子郵件");

        ExcelUtil.exportExcel(headers, users, out);
    }
}
```

匯入時以目標類別搭配 `ExcelCell` 標註的欄位對應，並透過 `ExcelLogs` 收集解析錯誤：

```java
import com.zipe.util.doc.ExcelUtil;
import com.zipe.util.doc.ExcelLogs;
import java.io.File;
import java.util.Collection;

public class ExcelImportExample {

    public Collection<User> importUsers(File excel) {
        ExcelLogs logs = new ExcelLogs();
        Collection<User> users = ExcelUtil.importExcel(excel, User.class, "yyyy-MM-dd", logs);
        if (logs.getHasError()) {
            // 處理 logs.getLogList() 中的逐行錯誤
        }
        return users;
    }
}
```

### 情境二：取得 Spring 容器中的 Bean

在非 Spring 管理的類別中，透過 `ApplicationContextHelper` 取得 Bean：

```java
import com.zipe.util.ApplicationContextHelper;
import com.zipe.service.MailService;

public class LegacyComponent {
    public void doWork() {
        MailService service = ApplicationContextHelper.popBean(MailService.class);
        // 也可依名稱取得：ApplicationContextHelper.popBean("mailServiceImpl", MailService.class);
    }
}
```

### 情境三：執行緒池與並行任務（對應 `example` 專案）

`example/` 專案示範 `base-spring-boot-starter` 的執行緒運用。`ThreadPoolTaskExecutorConfig` 提供共用的池大小常數（`CORE_POOL_SIZE = 5`、`MAX_POOL_SIZE = 1000`），可作為建立 `ExecutorService` 的依據：

```java
import com.zipe.config.ThreadPoolTaskExecutorConfig;
import com.example.util.threadpool.ThreadCallableTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPoolExample {

    public void run() throws Exception {
        ExecutorService executor =
                Executors.newFixedThreadPool(ThreadPoolTaskExecutorConfig.CORE_POOL_SIZE);

        // ThreadCallableTask 實作 Callable<String>，模擬耗時工作後回傳結果
        Future<String> future = executor.submit(new ThreadCallableTask("order-1"));
        String result = future.get();

        executor.shutdown();
    }
}
```

### 情境四：讀取照片 EXIF GPS 資訊

`example/` 中的 `ReadPhotoLocation` 示範以 `metadata-extractor` 解析 JPEG 的 GPS 經緯度：

```java
import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import java.io.File;

public class PhotoLocationExample {

    public void printLocation(File jpeg) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(jpeg);
        for (Directory directory : metadata.getDirectories()) {
            if (directory instanceof GpsDirectory gpsDir) {
                GeoLocation geo = gpsDir.getGeoLocation();
                if (geo != null) {
                    System.out.println("緯度: " + geo.getLatitude());
                    System.out.println("經度: " + geo.getLongitude());
                }
            }
        }
    }
}
```

## 常見問題

- **AES 解密拋出例外**：請確認加解密使用相同的金鑰，且金鑰長度為 16 位元組（AES-128）。本模組的 `AesUtil` 為 AES-128/CBC/PKCS5Padding，非 AES-256。
- **`Md5Util` 找不到 `encode` 方法**：`Md5Util` 沒有泛用的 `encode`，請依需求選用 `parseStrToMd5L32` / `parseStrToMd5U32` / `parseStrToMd5L16` / `parseStrToMd5U16`。
- **郵件發送失敗並拋出 `AuthenticationFailedException`**：請檢查 `mail.*` 設定的帳號密碼是否正確；部分服務商需使用「應用程式密碼」。並確認已先呼叫 `mailService.setInitData()`。
- **Velocity 找不到樣板**：確認模板檔位於 `velocity.dir-path`（預設 `template`）指定的目錄下，且呼叫 `generateContent` 前已執行對應的 `initClassPath()` / `initFilePath()` 等初始化方法。

:::tip 最佳實踐
無狀態工具（字串、日期、`Md5Util`）直接以靜態方法呼叫；加解密工具因持有金鑰須 `new` 出實例；有狀態服務（如 `MailService`）一律透過 Spring 注入，以確保設定與生命週期由容器統一管理。更深入的內部結構與擴充方式請參閱 [架構與開發指南](./architecture.md)。
:::
