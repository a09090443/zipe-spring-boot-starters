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
  csrf-enabled: false               # 預設為 true；此範例為簡化測試而關閉，正式環境建議維持開啟
  record-log-enable: true           # 啟用登出入事件記錄
  custom-record-log-bean: myLogonLogRecord  # 指向自訂日誌 Bean 名稱
```

:::note Spring Security 預設行為與覆寫方式
`logon-spring-boot-starter` 透過 `verification-type` 提供四種驗證模式：

- `basic`：使用內建 `DaoAuthenticationProvider` + `BasicUserServiceImpl`，預設帳密為 `admin/admin`。
- `ldap`：連接 LDAP 目錄服務，需另行設定 `security.ldap.*`。
- `custom`：從 Spring Context 取出 `security.custom-bean-name` 指定的 Bean（須實作 `AuthenticationProvider`）。
- `jwt`：無狀態 token 登入，內建 `POST /api/login` 簽發 token，需設定 `security.jwt.*`（詳見 [logon-starter configuration.md](../logon-starter/configuration.md)）。

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
若內建 `admin/admin` 無法滿足需求，可將 `verification-type` 改為 `custom`，並提供一個實作 `AuthenticationProvider` 的 Bean，即可接上自家的使用者資料庫或 SSO 系統。詳見下方「業務端自訂登入（CUSTOM 模式）」一節。
:::

## 業務端自訂登入（CUSTOM 模式）

`starters_example` 已內建一組可運行的 CUSTOM 自訂登入示範：業務端繼承 `logon-spring-boot-starter` 的 `CommonLoginProcess`，覆寫 `verifyNormalUser()`，以自家的 `user_login` 資料表進行帳號密碼驗證。繼承 `CommonLoginProcess` 的好處是**自動沿用父類別的 ADMIN 動態密碼機制**（`admin` 帳號以「當日日期 `yyyyMMdd`」為密碼），業務端只需專注一般帳號的驗證邏輯。

:::note 與 logon-starter 通用範例的關係
本節為 `starters_example` 的**實際整合程式碼**。`CommonLoginProcess` 的通用擴充說明、認證 Token 最佳實踐與設定屬性細節，請參閱 [logon-starter examples.md「範例四：CUSTOM 模式」](../logon-starter/examples.md) 與 [configuration.md「CUSTOM 模式」](../logon-starter/configuration.md)，此處不重複。
:::

### 步驟一：登入帳號資料表與測試資料

CUSTOM 示範使用獨立的 `user_login` 資料表（與 `user_main` 各自獨立，避免影響多資料來源切換測試）。`init/schema.sql` 與 `init/data.sql` 已補上對應結構與兩筆 BCrypt 測試帳號：

```sql
-- init/schema.sql
CREATE TABLE `user_login` (
                              `LoginId`  varchar(100) NOT NULL,
                              `Password` varchar(100) NOT NULL,
                              PRIMARY KEY (`LoginId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

```sql
-- init/data.sql（密碼為 BCrypt 雜湊，strength=10）
-- user01 / 1234
INSERT INTO user_login (LoginId, Password) VALUES('user01', '$2a$10$Y6WAl60GuH2wIULKsaRotuHGCAoYfGXmvclCEO2PrvRNQIqcb0VB2');
-- user02 / abcd
INSERT INTO user_login (LoginId, Password) VALUES('user02', '$2a$10$1Ihk8NP/mi1bxAErFUA0fu6RnY0EnuDEqYSa57VkDvxzgTrDNMgoK');
```

對應的 JPA 實體與 Repository（`UserLogin` / `UserLoginRepository`）以 `loginId` 為主鍵，並提供 `findByLoginId(String loginId)` 查詢方法。

### 步驟二：實作自訂 AuthenticationProvider

`starters_example` 中的 `DbAuthProvider` 完整實作如下（`com.example.config.DbAuthProvider`）。`@Component("dbAuthProvider")` 的 Bean 名稱即為 `application.yml` 中 `security.custom-bean-name` 所指定者；`PasswordEncoder` 由 `logon-spring-boot-starter` 預設提供（`BCryptPasswordEncoder`），可直接注入比對：

```java
@Slf4j
@Component("dbAuthProvider")
public class DbAuthProvider extends CommonLoginProcess {

    private final UserLoginRepository userLoginRepository;

    public DbAuthProvider(PasswordEncoder passwordEncoder, UserLoginRepository userLoginRepository) {
        super(passwordEncoder);
        this.userLoginRepository = userLoginRepository;
    }

    @Override
    protected UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password) {
        UserLogin userLogin = userLoginRepository.findByLoginId(loginId);
        if (userLogin == null) {
            log.warn("使用者:{} 帳號不存在", loginId);
            throw new BadCredentialsException("使用者:" + loginId + " 帳號或密碼錯誤");
        }
        if (!passwordEncoder.matches(password, userLogin.getPassword())) {
            log.warn("使用者:{} 密碼錯誤", loginId);
            throw new BadCredentialsException("使用者:" + loginId + " 帳號或密碼錯誤");
        }
        log.info("使用者:{} 登入成功", loginId);
        // 使用非 null 的權限集合使 token 成為已認證狀態，並清除明文密碼
        return new UsernamePasswordAuthenticationToken(loginId, null, Collections.emptyList());
    }
}
```

:::note data.sql 為 BCrypt 雜湊，故以 matches() 比對
`data.sql` 寫入的是 BCrypt 雜湊字串（非明文），因此 `verifyNormalUser()` 使用 `passwordEncoder.matches(rawPassword, hashedPassword)` 比對。若你的既有資料庫存的是明文密碼，請改以 `rawPassword.equals(storedPassword)` 等方式，或先將既有資料轉為 BCrypt 雜湊。
:::

### 步驟三：只改一行即可切換

`starters_example` 為了保留現有 `admin/admin` 示範，預設仍維持 `verification-type: basic`。要切換到自訂登入，只需把 `application.yml` 的 `verification-type` 由 `basic` 改成 `custom` 一行即可——`custom-bean-name` 已預先指向真實存在的 `dbAuthProvider` Bean：

```yaml
security:
  enable: true
  verification-type: custom          # 由 basic 改為 custom 即切換到自訂登入
  custom-bean-name: dbAuthProvider   # 對應 com.example.config.DbAuthProvider 的 @Component 名稱
```

切換後可用的測試帳號：

| 帳號 | 密碼 | 驗證方式 |
|---|---|---|
| `admin` | 當日日期 `yyyyMMdd`（例如 `20260614`） | 父類別 `CommonLoginProcess.verifySpecialUser()` 動態密碼 |
| `user01` | `1234` | `DbAuthProvider.verifyNormalUser()` 查 `user_login` 表 |
| `user02` | `abcd` | 同上 |

:::tip basic 模式下 dbAuthProvider 不會被使用
`dbAuthProvider` Bean 始終會被 Spring 建立（`@Component`），但只有在 `verification-type: custom` 時，`SecurityConfiguration` 才會透過 `custom-bean-name` 取出並掛載它。因此預設的 `basic` 模式下，此 Bean 存在卻不會被使用，不影響原本的 `admin/admin` 示範與專案啟動。
:::
