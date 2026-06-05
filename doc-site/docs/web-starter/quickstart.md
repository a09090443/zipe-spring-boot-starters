---
id: quickstart
title: 快速開始
sidebar_position: 2
---

# 快速開始

本頁示範如何建立一個 Thymeleaf 頁面與一個使用統一回應格式的 REST API。

## 前置需求

- JDK 17 以上、Spring Boot 3.5.x。
- 已將 `web-spring-boot-starter` 安裝至本地 Maven Repository。

:::note JSP 與 Thymeleaf 共存注意事項
JSP 與 Thymeleaf 可同時啟用，但必須確保兩者的 `viewNames` glob 模式不重疊（JSP 預設 `jsp/*`、Thymeleaf 預設 `html/*,vue/*,templates/*,th/*`），否則 order 較小的 JSP 解析器會優先命中 Thymeleaf 視圖名稱，導致渲染失敗。
:::

## Step 1：安裝模組

```bash
cd web-spring-boot-starter
./mvnw clean install -DskipTests
```

## Step 2：加入依賴

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>web-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Step 3：設定 application.yml

以 Thymeleaf 為例（使用正確的屬性鍵 `web.thymeleaf.enable`）：

```yaml
web:
  resource:
    pathPattern: /static/**
    location: /WEB-INF/static/
  thymeleaf:
    enable: true
    viewNames: "html/*,vue/*,templates/*,th/*"
    stuff: .html
    templateMode: HTML
  jsp:
    enable: false
```

:::info 屬性前綴為 `web`，不是 `zipe.web`
正確的屬性前綴是 `web.*`，例如 `web.thymeleaf.enable`、`web.resource.pathPattern`。請勿使用 `zipe.web.*` 或 `web.view-type` 等不存在的屬性名稱。
:::

## Step 4：建立視圖模板

Thymeleaf 模板必須放在 `src/main/webapp/WEB-INF/` 目錄下。以 `th/` 子目錄為例：

```
src/main/webapp/WEB-INF/
└── th/
    └── message.html
```

`src/main/webapp/WEB-INF/th/message.html` 內容範例：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><title>Message</title></head>
<body>
    <p th:text="${msg}">預設訊息</p>
</body>
</html>
```

## Step 5：程式碼範例

撰寫視圖 Controller（回傳視圖名稱）：

```java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/message")
    public String message(Model model) {
        model.addAttribute("msg", "Hello Thymeleaf");
        // 視圖名稱 "th/message" 對應到 /WEB-INF/th/message.html
        // （prefix=/WEB-INF/ + viewName + suffix=.html）
        return "th/message";
    }
}
```

撰寫使用統一回應格式的 REST API：

```java
import com.zipe.annotation.ResponseResultBody;
import com.zipe.dto.Result;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseResultBody
public class UserApiController {

    @GetMapping("/api/user")
    public Map<String, Object> getUser() {
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Alice");
        user.put("age", 30);
        return user;
    }
}
```

## Step 6：執行驗證

啟動應用程式：

```bash
./mvnw spring-boot:run
```

開啟 `http://localhost:8080/message` 應看到渲染後的 Thymeleaf 頁面；呼叫 `/api/user` 應得到統一包裝的 JSON：

```json
{
  "code": 200,
  "message": "OK",
  "data": { "name": "Alice", "age": 30 }
}
```

:::tip 自動包裝
只要在 Controller 或方法上標注 `@ResponseResultBody`，回傳值就會被 `ResponseResultBodyAdvice` 自動包裝為 `Result<T>` 結構，無須手動建立回應物件。
:::

:::warning 視圖路徑對應規則
視圖名稱的實體路徑由三部分組成：**固定 prefix**（`/WEB-INF/`）+ **視圖名稱**（你在 Controller 回傳的字串）+ **suffix**（`web.thymeleaf.stuff`，預設 `.html`）。

範例：回傳 `"th/message"` → 實體路徑為 `/WEB-INF/th/message.html`。

請確認模板檔案實際存在於 `src/main/webapp/WEB-INF/` 下對應的路徑。
:::

:::note 視圖名稱需符合 viewNames 模式
Thymeleaf ViewResolver 只處理符合 `web.thymeleaf.viewNames` 設定（預設 `html/*,vue/*,templates/*,th/*`）的視圖名稱。若你的視圖名稱不在這些模式中，需更新 `viewNames` 設定，或改用符合模式的目錄結構。
:::
