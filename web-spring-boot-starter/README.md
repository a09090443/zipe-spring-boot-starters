# web-spring-boot-starter

前端視圖整合模組，支援 JSP 與 Thymeleaf 兩種視圖引擎，並提供統一 REST 回應格式封裝。

## 主要功能

- JSP / Thymeleaf 視圖引擎切換
- 統一 REST 回應格式（`@ResponseResultBody` 註解自動封裝）
- 靜態資源路徑配置
- 國際化語言切換攔截器
- 基礎 Controller 類別

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>web-spring-boot-starter</artifactId>
    <version>4.0.0.0</version>
</dependency>
```

## 基本設定

```properties
# 視圖類型：jsp 或 thymeleaf
web.view-type=thymeleaf

# 靜態資源路徑
web.resource-locations=classpath:/static/
```

## 使用方式

```java
@RestController
@ResponseResultBody  // 自動將回傳值封裝為統一格式
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
        // 回應自動封裝為 { "code": 200, "message": "success", "data": {...} }
    }
}
```
