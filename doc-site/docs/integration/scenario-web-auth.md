---
id: scenario-web-auth
title: 情境一：Web 應用含認證
sidebar_position: 2
description: 使用 base + web + logon Starter 建構具登入認證的 Web 應用
---

# 情境一：Web 應用含認證

## 情境說明

本情境示範如何建構一個**具登入保護的 Web 應用**，最典型的應用場景是企業內部的**管理後台**：使用者必須先登入，才能存取受保護的頁面；登入、登出與認證失敗等事件可記錄至日誌系統以便稽核。

整個情境的核心在於三個 Starter 的協作：`web` 負責前端視圖渲染、`logon` 負責 Spring Security 認證與授權、`base` 提供底層工具與基礎設施。

## 使用的 Starters

| Starter | 在本情境的角色 |
|---|---|
| `base-spring-boot-starter` | 提供加解密、工具類等基礎設施（被其他 Starter 依賴） |
| `web-spring-boot-starter` | 提供 JSP / Thymeleaf 視圖解析，並提供 `BaseController` 基底類別 |
| `logon-spring-boot-starter` | 提供 Spring Security 過濾鏈、登入頁、登入日誌策略 |

## pom.xml 依賴配置

```xml
<dependencies>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>base-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>web-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>logon-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>

    <!-- JSP 編譯支援（若使用 JSP 視圖才需要） -->
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-jasper</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## application.yml 最小必要設定

```yaml
server:
  port: 8080
  servlet:
    context-path: /app

web:
  jsp:
    enable: false
  thymeleaf:
    enable: true
    viewNames: html/*
    stuff: .html
    templateMode: HTML

security:
  enable: true
  verification-type: basic          # 使用內建帳密 admin/admin
  allow-uris: /static/**            # 白名單路徑（不需登入）
  login-success-uri: /home          # 登入成功跳轉
  login-failure-uri: /login         # 登入失敗跳轉
  csrf-enabled: false
  record-log-enable: true           # 啟用登出入事件記錄
  custom-record-log-bean: myLogonLogRecord  # 指向自訂日誌 Bean 名稱
```

:::note Spring Security 預設行為與覆寫方式
`logon-spring-boot-starter` 透過 `verification-type` 提供三種驗證模式：

- `basic`：使用內建 `DaoAuthenticationProvider` + `BasicUserServiceImpl`，預設帳密為 `admin/admin`。
- `ldap`：連接 LDAP 目錄服務，需另行設定 `security.ldap.*`。
- `custom`：從 Spring Context 取出 `security.custom-bean-name` 指定的 Bean（須實作 `AuthenticationProvider`）。

此外，`security.login-uri` 未設定時走預設登入頁（`/login`）；設定後則改用自訂登入頁，Session 策略改為 `STATELESS`。若將 `security.enable` 設為 `false`，所有路徑 `/**` 都會 `permitAll`，等同於關閉安全控制。
:::

## 實作步驟

### 步驟一：撰寫頁面 Controller

頁面 Controller 繼承 `web-spring-boot-starter` 提供的 `BaseController`，回傳的視圖名稱需與 `web.thymeleaf.viewNames` 的 Ant Pattern 前綴一致。以下為 `starters_example` 中的 `WebController` 完整片段：

```java
@Controller
@RequestMapping("/")
public class WebController extends BaseController {

    @GetMapping({"/thymeleaf"})
    public String thymeleaf() {
        // 對應 /WEB-INF/html/hello.html
        return "html/hello";
    }

    @GetMapping({"/jsp"})
    public String jsp() {
        // 對應 /WEB-INF/jsp/hello.jsp（前綴 jsp/ 走 JSP Resolver）
        return "jsp/hello";
    }

    @GetMapping({"/demo"})
    public String demo() {
        return "html/demo";
    }

    @Override
    @GetMapping({"/"})
    public ModelAndView initPage() {
        ModelAndView view = new ModelAndView("index");
        view.addObject("name", "John");
        return view;
    }
}
```

:::note BaseController 採 setter 注入，子類別免寫建構子
`BaseController` 為抽象類別，`Environment` 與 `MessageSource` 由其內部以 `@Autowired` setter 注入，因此子類別**不需要撰寫建構子**，只要覆寫抽象方法 `initPage()` 作為首頁進入點即可（不是自行新增 `index()`）。
:::

視圖名稱前綴決定由哪個 ViewResolver 解析：以 `jsp/` 開頭的交給 `InternalResourceViewResolver`（JSP，Order = 1），其餘由 `ThymeleafViewResolver`（Order = 2）處理。

### 步驟二：Security 設定

Security 的核心設定皆透過 `application.yml` 的 `security.*` 完成，無須撰寫額外的 `SecurityFilterChain` 設定類別 —— `logon-spring-boot-starter` 的 `SecurityConfiguration`（標有 `@EnableWebSecurity` 與 `@EnableMethodSecurity(prePostEnabled = true)`）會自動建立過濾鏈。

由於 `@EnableMethodSecurity` 已自動開啟，你可以直接在任意 Controller 或 Service 方法上使用 Method Security 注解：

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin")
public String adminPage() {
    return "html/admin";
}
```

### 步驟三：自訂登入日誌

當 `security.record-log-enable: true` 時，必須提供一個實作 `CustomLogonLogRecord` 介面的 Bean，且 Bean 名稱需與 `security.custom-record-log-bean` 一致。以下為 `starters_example` 中的 `LogonLogRecord` 完整實作：

```java
@Slf4j
@Component
public class LogonLogRecord implements CustomLogonLogRecord {

    @Override
    public void recordLoginSuccessLog(String userId) {
        log.info("測試登入紀錄:{}", userId);
    }

    @Override
    public void recordFailureLog(String userId) {
        log.info("測試登入錯誤紀錄:{}", userId);
    }

    @Override
    public void recordLogoutSuccessLog(String userId) {
        log.info("測試登出紀錄:{}", userId);
    }
}
```

## 自訂認證行為的方式

本情境採用**策略模式（Strategy Pattern）**：`logon-spring-boot-starter` 定義 `CustomLogonLogRecord` 介面，業務系統提供實作，Starter 在 Spring Security 事件發生時，透過 Bean 名稱查找並呼叫對應方法。

| 介面方法 | 觸發時機 | 對應 Handler |
|---|---|---|
| `recordLoginSuccessLog(userId)` | 認證通過後 | `LoginSuccessHandler` |
| `recordFailureLog(userId)` | 認證失敗後 | `LoginFailureHandler` |
| `recordLogoutSuccessLog(userId)` | 登出後 | `LogoutSuccessHandler` |

啟用條件須同時滿足：

1. `security.record-log-enable` 為 `true`
2. `security.custom-record-log-bean` 不為空
3. 對應名稱的 Bean 已以 `@Component` 註冊於 Spring Context

只要實作這個介面，即可將登出入事件接到既有的稽核系統、資料庫或外部監控平台，而無須修改 Starter 本身。

:::tip 進一步切換驗證來源
若內建 `admin/admin` 無法滿足需求，可將 `verification-type` 改為 `custom`，並提供一個實作 `AuthenticationProvider` 的 Bean，即可接上自家的使用者資料庫或 SSO 系統。
:::
