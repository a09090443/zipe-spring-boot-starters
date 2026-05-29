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

:::note
JSP 與 Thymeleaf 不建議同時啟用，請於設定中明確指定其中一種視圖引擎，以免解析器衝突。
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

以 Thymeleaf 為例：

```yaml
zipe:
  web:
    view-type: thymeleaf
    prefix: /WEB-INF/th/
    suffix: .html
    static-path-pattern: /static/**
```

## Step 4：程式碼範例

撰寫頁面 Controller（回傳視圖名稱）：

```java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/message")
    public String message(Model model) {
        model.addAttribute("msg", "Hello Thymeleaf");
        return "message"; // 對應 /WEB-INF/th/message.html
    }
}
```

撰寫使用統一回應格式的 REST API：

```java
import com.zipe.annotation.ResponseResultBody;
import com.zipe.dto.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseResultBody
public class UserApiController {

    @GetMapping("/api/user")
    public User getUser() {
        User user = new User();
        user.setName("Alice");
        return user;
    }
}
```

## Step 5：執行驗證

啟動應用程式：

```bash
./mvnw spring-boot:run
```

開啟 `http://localhost:8080/message` 應看到渲染後的頁面；呼叫 `/api/user` 應得到統一包裝的 JSON：

```json
{
  "status": 200,
  "message": "success",
  "data": { "name": "Alice" }
}
```

:::tip 自動包裝
只要在 Controller 或方法上標註 `@ResponseResultBody`，回傳值就會被 `ResponseResultBodyAdvice` 自動包裝為 `Result` 結構，無須手動建立回應物件。
:::

:::warning 視圖路徑對應
`prefix` 與 `suffix` 決定視圖名稱如何對應到實體檔案。範例中回傳 `"message"` 會解析為 `/WEB-INF/th/message.html`，請確認檔案實際存在於該路徑。
:::
