---
id: customization
title: 客製化指南
sidebar_position: 2
description: 跨 starter 的客製與擴充接點總覽，盤點各模組預留的介面、抽象骨架、可覆寫 Bean 與「以設定值指定 Bean 名稱」機制，協助依需求擴充而不修改 Starter 原始碼。
---

# 客製化指南

本指南盤點 `zipe-spring-boot-starters` 各 starter **預留的客製／擴充接點**，讓你在「不修改 Starter 原始碼」的前提下，依業務需求調整行為。所有結論均對應實際原始碼（`*AutoConfiguration` / `*Configuration` 類別、SPI 介面與抽象骨架類別）。

## 客製化的三個層次

擴充本專案的 starter 時，請由淺入深、依序評估下列三個層次，能用較淺的層次解決就不要動到較深的層次：

1. **只調設定（Configuration）**：透過 `application.yml` / `application.properties` 切換既有行為。例如 `security.verification-type` 切換 BASIC / LDAP / CUSTOM、`web.thymeleaf.enable` 開關視圖引擎、`spring.quartz.enable` 開關排程。這類調整不需寫任何 Java 程式碼。
2. **實作預留接點（Extension Point）**：實作 Starter 定義的 **SPI 介面**（如 `CustomLogonLogRecord`）、繼承 **抽象骨架類別**（如 `CommonLoginProcess`、`BaseJDBC`、`QuartzJobFactory`、`BaseController`、`SecurityBaseService`），或提供一個 Bean 並以 **設定值指定其名稱**（如 `security.custom-bean-name`、`security.custom-record-log-bean`、`web.service.map[*].bean-name`）。框架會在執行期主動找到並呼叫你的實作。
3. **覆寫 Bean（Override）**：對標註 `@ConditionalOnMissingBean` 的 Bean，於業務專案宣告**同型別** Bean 即可整顆替換掉預設實作；最徹底的情況可整鏈覆寫（如 logon 的 `SecurityFilterChain`）。若連 `@AutoConfiguration` 都想關掉，使用 `spring.autoconfigure.exclude`（見最後一節）。

> 原則：**設定 → 接點 → 覆寫 Bean → 排除 AutoConfiguration**，越往後侵入性越高，請優先選用前者。

## 擴充接點總表

下表彙整各 starter 的主要擴充接點，內容均取自實際原始碼。

| Starter | 主要擴充接點（介面／抽象類別／可覆寫 Bean／設定指定 Bean 名稱） | 對應設定鍵 | 何時用 |
|---|---|---|---|
| base | 覆寫 Bean：`MessageSource`、`VelocityUtil`、`MailService`、`threadPoolTaskExecutor`（`BaseAutoConfiguration` 內未標 `@ConditionalOnMissingBean`，須以同名 Bean 或 `spring.main.allow-bean-definition-overriding` 覆寫）；`MessageSource` 受 `@ConditionalOnResource(classpath:message.properties)` 控制 | `mail.*`、`velocity.*` | 想換掉郵件實作、共用執行緒池參數或訊息資源時 |
| db | 抽象骨架：繼承 `BaseJDBC` 撰寫 DAO；`@DS` / `@DynamicDS` 標註切換資料來源；可覆寫 `DataSource`（`dataSource()` Bean）整顆換成自家動態資料來源 | `dynamic.*`（`data-source.properties`）、`@DS("...")` | 需要多資料來源切換、SQL 外化、自訂連線池組裝時 |
| job | 抽象骨架：繼承 `QuartzJobFactory`（覆寫 `executeJob`）撰寫排程業務；以 `quartz-jobs.properties` 宣告排程；`spring.quartz.enable` / `job-store-type` 切換啟用與 JobStore | `spring.quartz.enable`、`spring.quartz.job-store-type`、`quartz.*` | 撰寫排程任務、選擇記憶體或 JDBC JobStore 時 |
| logon | 抽象骨架：繼承 `CommonLoginProcess`（CUSTOM 模式 `AuthenticationProvider`）、`SecurityBaseService`；SPI：實作 `CustomLogonLogRecord`；設定指定 Bean 名稱：`custom-bean-name`、`custom-record-log-bean`；**所有 `@Bean`（含 `filterChain`）皆標 `@ConditionalOnMissingBean`，可同型別覆寫** | `security.verification-type`、`security.custom-bean-name`、`security.record-log-enable`、`security.custom-record-log-bean`、`security.login-uri` | 自訂登入驗證、稽核日誌、整鏈接管 Security 設定時 |
| web | 抽象骨架：繼承 `BaseController`；`@ResponseResultBody` 統一回應格式；可覆寫 `LocaleResolver`、`viewResolver` 等視圖 Bean（多數受 `@ConditionalOnProperty` 控制） | `web.jsp.enable`、`web.thymeleaf.enable`、`web.resource.*` | 切換 JSP／Thymeleaf、自訂靜態資源路徑、共用 Controller 基底時 |
| web-service | 設定指定 Bean 名稱：`web.service.map[*].bean-name` 指向業務 `@WebService` 實作 Bean；可覆寫 `cxfServletRegistration` 調整 Servlet 路由 | `web.service.uri-mapping`、`web.service.map.*`（`bean-name`、`uri-mapping`） | 發布 SOAP 服務端點、調整 CXF Servlet 對外路徑時 |

---

## base-spring-boot-starter

`BaseAutoConfiguration` 在 classpath 同時存在 `VelocityPropertyConfig` 與 `MailPropertyConfig` 時生效（`@ConditionalOnClass`），註冊 `MessageSource`、`ApplicationContextHelper`、`VelocityUtil`、`MailService` 與 `threadPoolTaskExecutor` 等 Bean。

接點重點：

- **`MessageSource`** 受 `@ConditionalOnResource(resources = "classpath:message.properties")` 控制：只要 classpath 沒有 `message.properties` 就不註冊，因此你可以選擇不放該檔，或自行提供 `MessageSource` Bean。
- 其餘 Bean（`MailService`、`VelocityUtil`、`threadPoolTaskExecutor`）目前**未標 `@ConditionalOnMissingBean`**。若要覆寫，請以**相同 Bean 名稱**宣告並啟用 `spring.main.allow-bean-definition-overriding=true`，或直接排除整個 `BaseAutoConfiguration`（見最後一節）。

覆寫共用執行緒池的範例（沿用框架預期的 Bean 名稱 `threadPoolTaskExecutor`）：

```java
@Configuration
public class MyExecutorConfig {

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(500);
        executor.initialize();
        return executor;
    }
}
```

---

## db-spring-boot-starter

`DataSourceConfigAutoConfiguration` 在 classpath 存在 `DataSourcePropertyConfig` 時生效，讀取 `data-source.properties` 組裝 `DynamicDataSource`，並提供 `dataSource()`、`entityManagerFactory`、`transactionManager`、`jdbcTemplate`、`namedParameterJdbcDaoSupport` 等 Bean；`DataSourceAspectAutoConfiguration` 註冊 `DynamicDataSourceAspect` 切面。

接點重點：

- **繼承 `BaseJDBC` 撰寫 DAO**：`BaseJDBC` 為抽象類別，透過 `@Autowired protected JdbcTemplate jdbcTemplate` 與 `protected NamedParameterJdbcDaoSupport support` 取得 Starter 註冊的 Bean，提供 `update / queryForBean / queryForMap / queryForList` 並整合 `Conditions`（動態條件）與 `Paging`（分頁），SQL 可外化至檔案以 `ResourceEnum` 定位。

  ```java
  @Repository
  public class UserDao extends BaseJDBC {
      // 直接使用父類別提供的 update / queryForList(...) 等方法，搭配 Conditions / Paging
  }
  ```

- **以 `@DS` / `@DynamicDS` 切換資料來源**：`@DS` 預設值為 `"common"`（`String value() default "common";`），切面在方法執行前後切換 ThreadLocal 中的資料來源並於結束後清除。

  ```java
  @DS("reportDb")
  public List<Report> listReports() { ... }
  ```

- **覆寫 `DataSource`**：`dataSource()` Bean 負責整個動態資料來源的組裝；如需完全自訂連線池建立邏輯，可整顆替換（注意同時會牽動依賴它的 `entityManagerFactory` 等 Bean）。

---

## job-spring-boot-starter

排程相關自動配置受 `spring.quartz.enable` 控制。`InitialJobAutoConfiguration` 標 `@ConditionalOnProperty(name = "spring.quartz.enable", havingValue = "true")`，啟動時依 `quartz-jobs.properties` 建立排程；`DataSourceAutoConfiguration` 以 `@ConditionalOnExpression("${spring.quartz.enable:true} && '${spring.quartz.job-store-type}'.equals('jdbc')")` 控制是否建立 Quartz 專用的 `quartzDataSource`（JDBC JobStore 模式才建立）。

接點重點：

- **繼承 `QuartzJobFactory` 撰寫排程業務**：`QuartzJobFactory` 繼承 Spring 的 `QuartzJobBean`，已在 `executeInternal` 內包好「前置日誌 → 業務 → 後置日誌」與例外捕捉，子類別只需實作唯一抽象方法：

  ```java
  protected abstract void executeJob(JobExecutionContext jobExecutionContext) throws Exception;
  ```

  範例：

  ```java
  public class ReportJob extends QuartzJobFactory {
      @Override
      protected void executeJob(JobExecutionContext context) throws Exception {
          // 撰寫排程業務邏輯；例外會由父類別統一以錯誤日誌記錄
      }
  }
  ```

- **設定層切換 JobStore**：`spring.quartz.job-store-type=jdbc` 時才會建立 `quartzDataSource`（讀 `spring.datasource.quartz.*` 並套用 `spring.datasource.hikari.*`），否則使用記憶體 JobStore，無需改任何程式碼。

---

## logon-spring-boot-starter

`SecurityConfiguration` 是本次客製能力最完整的一支。它以 `SecurityPropertyConfig`（前綴 `security`）驅動三種驗證模式，並將 **所有 `@Bean`（含 `filterChain`）標上 `@ConditionalOnMissingBean`**，相依改以方法參數注入，使覆寫可在 `proxyBeanMethods=false` 下正確傳遞。可客製的層次如下。

### 1. 覆寫單顆 Bean（@ConditionalOnMissingBean）

`passwordEncoder`、`basicUserServiceImpl`、`ldapUserDetailsService`、`sessionRegistry`、`loginSuccessHandler`、`loginFailureHandler`、`logoutSuccessHandler` 皆標 `@ConditionalOnMissingBean`，於業務專案宣告**同型別** Bean 即可覆寫。例如換掉密碼編碼器：

```java
@Configuration
public class MyPasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

由於 `filterChain` 以方法參數注入 `PasswordEncoder` / `BasicUserServiceImpl` 等容器 Bean，覆寫後框架會自動採用你的版本。

### 2. 整鏈覆寫 SecurityFilterChain

`filterChain(...)` 標 `@ConditionalOnMissingBean(SecurityFilterChain.class)`。若預設的表單登入／自訂登入流程不符需求，可直接宣告自己的 `SecurityFilterChain` Bean 完整接管整條過濾鏈：

```java
@Configuration
public class MySecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 完全自訂 authorizeHttpRequests / formLogin / sessionManagement ...
        return http.build();
    }
}
```

此時 `SecurityConfiguration` 內的 `filterChain` 因條件不成立而不註冊，其餘工具 Bean（如 `sessionRegistry`）仍可沿用或一併覆寫。

> 備註：`application.yml` 的 `spring.main.allow-bean-definition-overriding=true` 為向後相容保留，覆寫能力現已不依賴它。

### 3. CUSTOM 模式：自訂 AuthenticationProvider（繼承 CommonLoginProcess）

將 `security.verification-type` 設為 `custom`，並以 `security.custom-bean-name` 指向一顆實作 `AuthenticationProvider` 的 Bean。`SecurityConfiguration.authenticationProvider(...)` 會在 CUSTOM 模式下透過 `ApplicationContextHelper.getBean(...)` 取出該 Bean；**若未設定 `custom-bean-name` 會拋出 `NullPointerException("Please enter value in custom-bean-name")`**。

最省事的做法是繼承抽象骨架 `CommonLoginProcess`：它已實作 `AuthenticationProvider`，ADMIN 帳號自動走「當日日期動態密碼」驗證，子類別只需覆寫一般使用者驗證：

```java
protected abstract UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password);
```

`starters_example` 提供可運行的實例 `com.example.config.DbAuthProvider`（Bean 名稱 `dbAuthProvider`），以 `user_login` 資料表做 DB 驗證：

```java
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
        if (userLogin == null || !passwordEncoder.matches(password, userLogin.getPassword())) {
            throw new BadCredentialsException("使用者:" + loginId + " 帳號或密碼錯誤");
        }
        return new UsernamePasswordAuthenticationToken(loginId, null, Collections.emptyList());
    }
}
```

對應設定（`starters_example` 預設為 `basic` + `security.basic.users`，要切到 `custom` 只需把 `verification-type` 改為 `custom` 並取消註解 `custom-bean-name`）：

```yaml
security:
  verification-type: custom   # 由 basic 改為 custom
  custom-bean-name: dbAuthProvider   # 對應 @Component("dbAuthProvider")
```

> `dbAuthProvider` 因 `@Component` 始終會被建立，但只有 `verification-type: custom` 時 `SecurityConfiguration` 才會取出並掛載它；`basic` 模式下此 Bean 存在卻不被使用，不影響預設示範。整合情境詳見 [scenario-web-auth](./integration/scenario-web-auth.md)。

### 4. CustomLogonLogRecord 稽核日誌（SPI 介面）

`CustomLogonLogRecord` 是 `service` 套件下的 SPI 介面，定義三個回呼：

```java
public interface CustomLogonLogRecord {
    void recordLoginSuccessLog(String userId);
    void recordFailureLog(String userId);
    void recordLogoutSuccessLog(String userId);
}
```

業務專案實作後宣告為 Spring Bean，並設定 `security.record-log-enable: true` 與 `security.custom-record-log-bean`（指向你的 Bean 名稱）。三個 Handler（`LoginSuccessHandler` / `LoginFailureHandler` / `LogoutSuccessHandler`）會在對應事件發生時，透過 `ApplicationContextHelper.getBean(...)` 取出實作並回呼；**啟用 `record-log-enable=true` 卻未填 `custom-record-log-bean` 時，Handler 會丟出例外**（`The Custom-Record-Log must have value while Record-Log-Enable = true`）。

```java
@Component("auditLogRecord")
public class AuditLogRecord implements CustomLogonLogRecord {
    @Override public void recordLoginSuccessLog(String userId) { /* 寫稽核表 */ }
    @Override public void recordFailureLog(String userId) { /* 寫稽核表 */ }
    @Override public void recordLogoutSuccessLog(String userId) { /* 寫稽核表 */ }
}
```

```yaml
security:
  record-log-enable: true
  custom-record-log-bean: auditLogRecord
```

### 5. 共用認證基底：SecurityBaseService

業務服務可繼承 `SecurityBaseService` 取得 `fetchLoginUser()` 等共用方法，從 `HttpSession` 取回目前登入使用者的 `SysUserVO`（內部透過 `UserInfoUtil.loginUserId()` 解析識別碼）。

---

## web-spring-boot-starter

`ViewResolverAutoConfiguration` 與 `WebAutoConfiguration` 在 classpath 存在 `WebPropertyConfig` 時生效。視圖相關 Bean 受 `@ConditionalOnProperty` 控制：`viewResolver`（JSP）需 `web.jsp.enable=true`、`templateResolver` / `viewResolverThymeLeaf`（Thymeleaf）需 `web.thymeleaf.enable=true`，兩者可共存（分別以 order 1 / 2 與 viewNames glob 分流）。`localeResolver`（`CookieLocaleResolver`，預設 `Locale.TAIWAN`）與 `enableDefaultServlet` 為無條件註冊。

接點重點：

- **設定層切換視圖引擎**：以 `web.jsp.enable` / `web.thymeleaf.enable` 決定啟用哪種解析器，無需寫程式碼。
- **繼承 `BaseController`**：抽象基底，提供 `getMessage(...)` 國際化、`Environment` 存取與 `request` / `response` / `currentLocale` 等受保護欄位，並定義初始頁面抽象方法供子類別實作。
- **`@ResponseResultBody` 統一回應格式**：標於 Controller／方法上，套用統一的 `Result` DTO 回應包裝。
- **覆寫視圖 Bean**：`localeResolver` 等 Bean 可於業務專案宣告同型別 Bean 調整（例如改預設語系或 Cookie 名稱）。

---

## web-service-spring-boot-starter

`CxfConfigAutoConfiguration` 以 `web.service.uri-mapping` 註冊 `CXFServlet`（`cxfServletRegistration` Bean）；`WebServiceRegisterAutoConfiguration` 在 `afterPropertiesSet()` 遍歷 `web.service.map`，依每筆的 `bean-name` 從容器取出實作 Bean，以 CXF `EndpointImpl` 發布到該筆的 `uri-mapping`。

接點重點：

- **設定指定 Bean 名稱發布服務**：這是本模組的主要擴充方式——你只需提供 `@WebService` 實作 Bean，並在 `web.service.map` 中以 `bean-name` 指向它、`uri-mapping` 指定對外路徑，框架即自動發布。

  ```yaml
  web:
    service:
      uri-mapping: /services/*
      map:
        helloService:
          bean-name: helloServiceImpl   # 對應業務 @WebService 實作 Bean
          uri-mapping: /hello
  ```

- **覆寫 `cxfServletRegistration`**：如需調整 CXF Servlet 的對外路由或註冊參數，可宣告同型別 `ServletRegistrationBean` Bean 覆寫。
- 每個端點固定掛載 `CdataContentInterceptor`（入站）與 `ResponseCdataInterceptor`（出站）攔截器並啟用 MTOM／JAXB 元素解包。

---

## 找不到接點怎麼辦

當既有設定鍵、SPI 介面與抽象類別都無法滿足需求時，可採用以下兩種通則：

### 1. 以同型別 Bean 覆寫（首選）

對標註 `@ConditionalOnMissingBean` 的 Bean，於業務專案宣告**同型別** Bean 即可讓 Starter 的預設實作退讓。logon-starter 的所有 `@Bean`（含 `filterChain`）皆已支援此方式。對於**未標** `@ConditionalOnMissingBean` 的 Bean（如 base 的 `mailService` / `velocityUtil`），請以**相同 Bean 名稱**宣告並啟用 `spring.main.allow-bean-definition-overriding=true`，或改用下一招直接排除整個自動配置。

### 2. 排除整個 AutoConfiguration（spring.autoconfigure.exclude）

本專案的自動配置類別皆以 `@AutoConfiguration` 標註。若要完全停用某支自動配置（連帶其所有 Bean），用 `spring.autoconfigure.exclude` 指定其全限定類名即可，之後便能自行提供完整替代設定：

```yaml
spring:
  autoconfigure:
    exclude:
      - com.zipe.autoconfiguration.BaseAutoConfiguration
      - com.zipe.autoconfiguration.SecurityConfiguration
```

排除後，該模組原本註冊的所有 Bean 都不再產生，需由你的 `@Configuration` 完整補齊，請評估侵入性後再採用。一般而言應優先嘗試「調設定 → 實作接點 → 覆寫單顆 Bean」，排除整個 AutoConfiguration 是最後手段。
