---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `web-spring-boot-starter` 的統一回應、視圖渲染與全域例外處理。

## 基礎使用範例

### 統一回應格式的 REST API

```java
import com.zipe.annotation.ResponseResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@ResponseResultBody
public class ProductController {

    @GetMapping("/api/products")
    public List<String> list() {
        return List.of("Apple", "Banana", "Cherry");
    }
}
```

回應將自動包裝為：

```json
{
  "status": 200,
  "message": "success",
  "data": ["Apple", "Banana", "Cherry"]
}
```

### 渲染 Thymeleaf 頁面

```java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "首頁");
        return "index";
    }
}
```

## 進階使用範例

### 拋出自訂例外回傳錯誤狀態

搭配 `ResultException` 與 `ResultStatus`，可在統一格式下回傳錯誤：

```java
import com.zipe.enums.ResultStatus;
import com.zipe.exception.ResultException;
import com.zipe.annotation.ResponseResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseResultBody
public class OrderController {

    @GetMapping("/api/orders/{id}")
    public Order get(@PathVariable Long id) {
        Order order = orderService.find(id);
        if (order == null) {
            throw new ResultException(ResultStatus.NOT_FOUND, "訂單不存在");
        }
        return order;
    }
}
```

錯誤回應：

```json
{
  "status": 404,
  "message": "訂單不存在",
  "data": null
}
```

### 繼承 BaseController 共用方法

```java
import com.zipe.base.controller.BaseController;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController extends BaseController {
    // 可直接使用 BaseController 提供的共用工具方法
}
```

## 常見情境

### 情境一：前後端分離的 JSON API

整個專案僅提供 API 時，於各 RestController 標註 `@ResponseResultBody`，使所有回應維持一致的 `Result` 結構，前端可統一以 `status` 判斷成敗。

### 情境二：傳統頁面與 API 並存

頁面以 `@Controller` 回傳視圖名稱、API 以 `@RestController` + `@ResponseResultBody` 回傳 JSON，兩者於同一應用中共存。

### 情境三：自訂回應狀態碼

擴充 `ResultStatus` 列舉或實作 `IResultStatus` 介面，定義業務專屬的狀態碼與訊息。

## 常見問題

- **回應沒有被包裝**：確認 Controller 或方法已標註 `@ResponseResultBody`，且回傳的不是已包裝過的 `Result`。
- **頁面 404 或樣板找不到**：核對 `prefix` / `suffix` 與實體檔案路徑是否一致。
- **JSP 無法渲染**：內嵌 Tomcat 打包為 JAR 時不支援 JSP，請改用 WAR 或 Thymeleaf。
- **靜態資源載入失敗**：檢查 `static-path-pattern` 與 `static-locations` 設定是否涵蓋資源實際位置。

:::tip 最佳實踐
統一回應結構能大幅簡化前端錯誤處理邏輯。建議制定明確的狀態碼規範（如 2xx 成功、4xx 用戶端錯誤、5xx 伺服器錯誤），並集中於 `ResultStatus` 維護，避免散落各處的魔術數字。
:::
