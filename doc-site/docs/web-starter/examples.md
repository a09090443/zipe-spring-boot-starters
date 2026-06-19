---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `web-spring-boot-starter` 的統一回應、視圖渲染、全域例外處理、語系切換與日期格式化等功能。

## 基礎使用範例

### 範例一：統一回應格式的 REST API

在 Controller 類別或方法上標注 `@ResponseResultBody`，回傳值會被 `ResponseResultBodyAdvice` 自動包裝為 `Result<T>` 結構，無需手動建立回應物件。

```java
import com.zipe.annotation.ResponseResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@ResponseResultBody  // 類別層級：整個 Controller 的所有方法都啟用包裝
public class ProductController {

    @GetMapping("/api/products")
    public List<String> list() {
        return List.of("Apple", "Banana", "Cherry");
    }
}
```

回應將自動包裝為（`code` 欄位為整數，`message` 預設為 `"OK"`）：

```json
{
  "code": 200,
  "message": "OK",
  "data": ["Apple", "Banana", "Cherry"]
}
```

### 範例二：回傳 String 型別

當方法回傳 `String` 時，Advice 內部會先透過 `ObjectMapper` 序列化，再以字串形式輸出，最終客戶端收到的仍是完整的 JSON 字串。

```java
@RestController
@ResponseResultBody
public class GreetingController {

    @GetMapping("/api/greeting")
    public String greet() {
        return "Hello, World!";
    }
}
```

客戶端收到的回應 body（Content-Type: text/plain，但值為 JSON 格式字串）：

```json
{"code":200,"message":"OK","data":"Hello, World!"}
```

### 範例三：回傳已包裝的 `Result`（避免雙重包裝）

若方法直接回傳 `Result<T>`，Advice 會偵測到並**直接透傳**，不會再次包裝成 `Result<Result<T>>`。

```java
@RestController
@ResponseResultBody
public class StatusController {

    @GetMapping("/api/status")
    public Result<String> status() {
        // 直接回傳 Result，Advice 不會再次包裝
        return Result.success("service is running");
    }
}
```

### 範例四：自訂回應訊息（`@ResponseResultBody` 的 `message` 屬性）

`@ResponseResultBody` 的 `message` 屬性預設為 `"OK"`。將其設定為其他值時，回應的 `message` 欄位會採用自訂值。

```java
@RestController
public class ReportController {

    @GetMapping("/api/report")
    @ResponseResultBody(message = "Report generated successfully")
    public Map<String, Object> generateReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("total", 100);
        return report;
    }
}
```

回應：

```json
{
  "code": 200,
  "message": "Report generated successfully",
  "data": { "total": 100 }
}
```

### 範例五：渲染 Thymeleaf 頁面

視圖名稱必須符合 `web.thymeleaf.viewNames` 的 glob 模式（預設 `html/*,vue/*,templates/*,th/*`）。

```java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "首頁");
        model.addAttribute("welcomeMsg", "歡迎使用本系統");
        // 視圖名稱 "th/index" → 實體路徑 /WEB-INF/th/index.html
        return "th/index";
    }
}
```

對應的 `src/main/webapp/WEB-INF/th/index.html`：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${title}">預設標題</title>
</head>
<body>
    <h1 th:text="${welcomeMsg}">歡迎</h1>
</body>
</html>
```

### 範例六：渲染 JSP 頁面（需 `web.jsp.enable=true`）

```java
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class JspController {

    @GetMapping("/hello")
    public ModelAndView hello() {
        ModelAndView mav = new ModelAndView();
        // 視圖名稱 "jsp/hello" → 實體路徑 /WEB-INF/jsp/hello.jsp
        mav.setViewName("jsp/hello");
        mav.addObject("welcome", "你好，JSP！");
        return mav;
    }
}
```

---

## 進階使用範例

### 範例七：拋出業務例外回傳錯誤狀態

`ResultException` 攜帶 `ResultStatus` 枚舉，由 `ResponseResultBodyAdvice` 攔截後轉成標準錯誤回應。

:::warning `ResultException` 的型別限制
`ResultException` 建構子目前只接受 `ResultStatus` 枚舉（內建三種狀態：`SUCCESS`、`BAD_REQUEST`、`INTERNAL_SERVER_ERROR`），**無法傳入自訂 `IResultStatus` 實作**。若需自訂狀態碼，請直接回傳 `Result.failure(customStatus)`，或擴充 `ResultException`（見範例八）。
:::

```java
import com.zipe.enums.ResultStatus;
import com.zipe.exception.ResultException;
import com.zipe.annotation.ResponseResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseResultBody
public class DataController {

    @GetMapping("/api/data")
    public String getData() throws ResultException {
        boolean hasError = true;
        if (hasError) {
            // 拋出 ResultException，Advice 攔截後回傳 500 錯誤格式
            throw new ResultException(ResultStatus.INTERNAL_SERVER_ERROR);
        }
        return "data";
    }
}
```

錯誤回應（HTTP 500）：

```json
{
  "code": 500,
  "message": "Internal Server Error",
  "data": null
}
```

### 範例八：自訂業務錯誤碼（實作 `IResultStatus`）

業務系統可實作 `IResultStatus` 介面，定義專屬錯誤碼，再透過 `Result.failure()` 直接回傳。

```java
// Step 1：定義業務枚舉
import com.zipe.exception.IResultStatus;
import org.springframework.http.HttpStatus;

public enum AppStatus implements IResultStatus {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 1001, "User not found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, 1002, "Email already exists");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;

    AppStatus(HttpStatus httpStatus, Integer code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus getHttpStatus() { return httpStatus; }
    @Override public Integer getCode() { return code; }
    @Override public String getMessage() { return message; }
}
```

```java
// Step 2：在 Controller 使用
@RestController
@ResponseResultBody
public class UserController {

    @GetMapping("/api/users/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            // 直接回傳 Result.failure，不透過 ResultException
            return Result.failure(AppStatus.USER_NOT_FOUND);
        }
        return Result.success(user);
    }
}
```

回應（HTTP 404）：

```json
{
  "code": 1001,
  "message": "User not found",
  "data": null
}
```

### 範例九：繼承 BaseController

`BaseController` 提供 i18n 訊息查找（`getMessage`）與 `Environment` 存取功能。

:::note 命名空間
`BaseController` 使用 `jakarta.servlet.http.*`，相容 Spring Boot 4 / Jakarta EE 11。
:::

```java
import com.zipe.base.controller.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DashboardController extends BaseController {

    @Override
    public ModelAndView initPage() {
        // 必須實作：回傳首頁 ModelAndView
        ModelAndView mav = new ModelAndView("th/dashboard");
        mav.addObject("title", getMessage("dashboard.title"));
        return mav;
    }
}
```

### 範例十：語系切換（`LocaleChangeInterceptor`）

發送帶有 `language` 參數的請求，`LocaleChangeInterceptor` 會自動更新語系並存入 Cookie（有效期 4800 秒）。後續請求只要攜帶該 Cookie，`MessageSource` 就會使用對應語系的訊息。

```
# 切換為英文
GET /dashboard?language=en_US

# 切換回台灣繁體中文
GET /dashboard?language=zh_TW
```

```java
// BaseController.getMessage() 會根據當前語系查找訊息
String title = getMessage("dashboard.title");
```

### 範例十一：日期格式化（`DateFormatter`）

前端傳入毫秒 Unix timestamp 字串，後端方法參數宣告為 `java.util.Date` 即可自動綁定，無需手動解析。

```java
@RestController
@ResponseResultBody
public class EventController {

    @GetMapping("/api/events")
    public List<String> getEvents(@RequestParam Date startDate, @RequestParam Date endDate) {
        // startDate 與 endDate 由 DateFormatter 從毫秒 timestamp 字串自動轉換
        // 例如：?startDate=1717200000000&endDate=1717286400000
        return eventService.findBetween(startDate, endDate);
    }
}
```

前端呼叫範例：

```
GET /api/events?startDate=1717200000000&endDate=1717286400000
```

---

## 常見情境

### 情境一：前後端分離的 JSON API

整個專案僅提供 API 時，於各 `RestController` 標注 `@ResponseResultBody`，使所有回應維持一致的 `Result<T>` 結構，前端可統一以 `code` 欄位判斷成敗。

### 情境二：傳統頁面與 API 並存

頁面以 `@Controller` 回傳視圖名稱、API 以 `@RestController` + `@ResponseResultBody` 回傳 JSON，兩者於同一應用中共存，互不干擾。

### 情境三：自訂回應狀態碼

實作 `IResultStatus` 介面，定義業務專屬的狀態碼與訊息，搭配 `Result.failure(customStatus)` 直接回傳（不依賴 `ResultException`）。

---

## 常見問題

- **回應沒有被包裝**：確認 Controller 或方法已標注 `@ResponseResultBody`，且元件掃描範圍涵蓋 `com.zipe`（`ResponseResultBodyAdvice` 不在 AutoConfiguration 中，需靠元件掃描發現）。若回傳值已是 `Result` 型別，Advice 會直接透傳（不會雙重包裝）。
- **頁面 404 或樣板找不到**：核對視圖名稱是否符合對應引擎的 `viewNames` glob 模式，以及模板檔案是否存在於 `src/main/webapp/WEB-INF/` 下對應路徑（視圖名稱 + 副檔名）。
- **JSP 無法渲染**：確認 `web.jsp.enable=true`，且 `pom.xml` 包含 `tomcat-embed-jasper` 依賴。內嵌 Tomcat 打包為 JAR 時 JSP 渲染可能受限，建議改用 WAR 封裝或改採 Thymeleaf。
- **靜態資源載入失敗**：確認 `web.resource.pathPattern`（URL 前綴）與 `web.resource.location`（實體目錄）設定正確，且靜態檔案確實存在於 `location` 指定的目錄下。
- **JSON 回應 `message` 欄位非預期**：預設 `message` 為 `"OK"`；若 `@ResponseResultBody(message = "xxx")` 設定了非預設值，回應 `message` 會採用自訂值。錯誤回應的 `message` 由 `ResultStatus` 或自訂 `IResultStatus` 的 `getMessage()` 決定。
- **例外被捕獲但 HTTP 狀態碼仍為 200**：確認例外是從 `@ResponseResultBody` 標注的 Controller 方法拋出，且 `ResponseResultBodyAdvice` 已被元件掃描注冊。未知例外（非 `ResultException`）一律回傳 HTTP 500。

:::tip 最佳實踐
統一回應結構能大幅簡化前端錯誤處理邏輯。建議制定明確的狀態碼規範（如 2xx 成功、4xx 用戶端錯誤、5xx 伺服器錯誤），集中定義在 `IResultStatus` 實作枚舉中，避免散落各處的魔術數字。業務層的例外處理優先使用 `Result.failure(customStatus)` 直接回傳，比透過 `ResultException` 拋出更具彈性。
:::
