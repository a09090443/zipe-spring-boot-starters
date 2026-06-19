---
id: examples
title: 使用範例
sidebar_position: 4
description: 權限保護、覆寫授權解析、自訂帳號來源與關閉內建 API 的完整範例
---

# 使用範例

本頁示範 iam-starter 常見的整合與客製化情境。所有覆寫皆基於 `@ConditionalOnMissingBean`：業務專案宣告同型別 Bean 即自動取代 starter 預設，**不需 `spring.main.allow-bean-definition-overriding`**。

## 範例 1：注入 Service 自行管理帳號

關閉內建 REST API，改在業務程式中直接使用三組 Service：

```yaml
iam:
  api:
    enabled: false   # 關閉內建 Controller，僅保留 Service
```

```java
@Service
public class OnboardingService {

    private final AccountService accountService;
    private final GroupService groupService;

    public OnboardingService(AccountService accountService, GroupService groupService) {
        this.accountService = accountService;
        this.groupService = groupService;
    }

    /** 建立新進員工帳號並指派到「一般使用者」群組。 */
    public AccountVO onboard(String username, String rawPassword, Long defaultGroupId) {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setUsername(username);
        req.setPassword(rawPassword);          // 服務層會 BCrypt 編碼
        AccountVO account = accountService.createAccount(req);
        return accountService.addToGroup(account.getId(), defaultGroupId);
    }
}
```

## 範例 2：以權限保護任意業務邏輯

權限是泛用具名授權點，不限於 REST API。可在任何方法上以 `@PreAuthorize` 比對：

```java
@Service
public class ReportService {

    // 群組 code = MANAGER → authority ROLE_MANAGER
    @PreAuthorize("hasRole('MANAGER')")
    public Report monthlySummary() { ... }

    // 權限 code = REPORT_EXPORT → authority REPORT_EXPORT（無前綴）
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public byte[] exportPdf(Long reportId) { ... }

    // 也可組合條件
    @PreAuthorize("hasRole('MANAGER') and hasAuthority('REPORT_EXPORT')")
    public byte[] exportConfidential(Long reportId) { ... }
}
```

也可在程式中直接讀取當前登入者的 authorities 自行判斷：

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
boolean canExport = auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("REPORT_EXPORT"));
```

## 範例 3：覆寫授權解析規則（GrantedAuthoritiesResolver）

若希望調整「帳號 → authorities」的展開邏輯（例如加上自訂的全域 authority，或從外部系統補充權限），宣告一個 `GrantedAuthoritiesResolver` Bean 即可覆寫 iam 的 `DbGrantedAuthoritiesResolver`：

```java
@Configuration
public class CustomAuthoritiesConfig {

    @Bean
    public GrantedAuthoritiesResolver grantedAuthoritiesResolver(AccountRepository accountRepository,
                                                                 IamProperties iamProperties) {
        DbGrantedAuthoritiesResolver delegate =
                new DbGrantedAuthoritiesResolver(accountRepository, iamProperties);
        return username -> {
            // 在 DB 權限之外，為所有登入者補上一個基礎 authority
            Set<GrantedAuthority> merged = new LinkedHashSet<>(delegate.resolve(username));
            merged.add(new SimpleGrantedAuthority("ROLE_USER"));
            return merged;
        };
    }
}
```

:::tip 此擴充點對三種驗證模式皆生效
`GrantedAuthoritiesResolver` 是 logon-starter 定義的 SPI。BASIC、LDAP、CUSTOM 三種驗證模式的登入流程都會呼叫它解析 authorities，因此覆寫一處即同時影響所有模式。
:::

## 範例 4：LDAP 驗證 + iam 授權

組織以 AD／LDAP 驗證帳號密碼，但權限改由 iam 資料庫集中管理：

```yaml
security:
  verification-type: ldap
  ldap:
    ip: 10.0.0.10
    port: "389"
    domain: corp.example.com
    dn: DC=corp,DC=example,DC=com

iam:
  enabled: true
  ddl:
    init: false
```

只要在 `iam_account` 建立與 LDAP 同名的帳號（`password` 欄可留空）並指派群組，使用者以 AD 帳密登入後，`LdapUserDetailsService` 即透過 `GrantedAuthoritiesResolver` 由 iam 取得群組／權限。**不需在 iam 維護密碼**。

## 範例 5：覆寫內建 Controller

保留內建 API 但替換其中一個 Controller 的行為（例如帳號建立要額外發送通知）：

```java
@RestController
@RequestMapping("${iam.api.base-path:/api/iam}/accounts")
public class CustomAccountController {

    private final AccountService accountService;
    private final NotificationService notificationService;

    public CustomAccountController(AccountService accountService,
                                   NotificationService notificationService) {
        this.accountService = accountService;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<AccountVO> create(@RequestBody CreateAccountRequest request) {
        AccountVO account = accountService.createAccount(request);
        notificationService.welcome(account.getUsername());
        return ResponseEntity.ok(account);
    }

    // 其餘端點可委派 accountService 比照內建實作補齊……
}
```

由於 starter 的 `accountController` Bean 標註 `@ConditionalOnMissingBean`，宣告同型別 Bean 後內建版本自動退讓。

## 範例 6：完全自管帳號來源

若帳號資料不存於 iam 表（例如來自既有的人事系統），可保留 iam 的群組／權限模型，僅覆寫 `BasicUserServiceImpl` 的來源：

```java
@Bean
public BasicUserServiceImpl iamUserDetailsService(PasswordEncoder passwordEncoder,
                                                  HrAccountGateway hrGateway,
                                                  GrantedAuthoritiesResolver authoritiesResolver) {
    return new HrBackedUserDetailsService(passwordEncoder, hrGateway, authoritiesResolver);
}
```

`HrBackedUserDetailsService` 繼承 `BasicUserServiceImpl`、覆寫 `loadUserByUsername`，密碼與狀態取自人事系統，authorities 仍由 `GrantedAuthoritiesResolver` 提供——iam 的授權模型與外部帳號來源可自由組合。
