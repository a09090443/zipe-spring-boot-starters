# logon-starter JWT 登入模式設計

- **日期**：2026-06-14
- **模組**：`logon-spring-boot-starter`
- **狀態**：設計確認，待實作

## 目標

在 `logon-spring-boot-starter` 新增 JWT 登入模式，作為 `VerificationTypeEnum` 的第四種驗證方式（與 BASIC / LDAP / CUSTOM 並列），供前後端分離或無狀態 API 場景使用。

## 已確認的決策

| 項目 | 決策 |
|---|---|
| 位置 | `logon-spring-boot-starter`，JWT 相關集中於 `com.zipe.jwt` 子套件 |
| 驗證模式定位 | 新增 `VerificationTypeEnum.JWT`，與 BASIC / LDAP / CUSTOM 互斥 |
| 函式庫 | jjwt（`io.jsonwebtoken`） |
| 發 token 入口 | starter 內建 REST 登入端點，以 `@ConditionalOnMissingBean` 可覆寫 |
| Token 生命週期 | 單一 access token，過期即重新登入（無 refresh token） |
| 簽章演算法 | HS256 / RS256 由設定 `security.jwt.algorithm` 切換 |
| 權限來源 | token 只放 username，每次請求透過 `UserDetailsService` 查權限（可即時撤權） |

## 整體架構

當 `security.verification-type=JWT` 時，filter chain 切換成無狀態 token 流程，與現有 session 流程互斥（`STATELESS`、停用 `formLogin` 與 session 管理）。

```
登入階段（發 token）:
  POST /api/login {username, password}
    → JwtLoginController（內建，可覆寫）
    → AuthenticationManager 委派既有 AuthenticationProvider 驗帳密
       （BASIC=DaoAuthenticationProvider / LDAP=LdapUserDetailsService，沿用現有邏輯）
    → 驗證成功 → JwtTokenProvider.generateToken(username)
    → 回傳 { "token": "xxx", "tokenType": "Bearer" }

請求階段（驗 token）:
  任意請求帶 Authorization: Bearer xxx
    → JwtAuthenticationFilter（OncePerRequestFilter，加在 UsernamePasswordAuthenticationFilter 前）
    → JwtTokenProvider.validate() 取出 username
    → UserDetailsService.loadUserByUsername() 查權限（可即時撤權）
    → 寫入 SecurityContext → 現有 @PreAuthorize / UserInfoUtil 照常可用
```

**關鍵點**：JWT 模式重用現有 `AuthenticationProvider` 驗帳密邏輯，因此 JWT 可疊在 BASIC 或 LDAP 之上（帳密來源不變，只是改用 token 承載登入狀態）。

## 新增類別與職責

JWT 相關全部集中於 `com.zipe.jwt` 子套件：

```
com.zipe.jwt
├── JwtProperties              設定屬性
├── JwtTokenProvider           簽發 / 驗證核心
├── JwtAuthenticationFilter    Bearer token 驗證 filter
├── JwtLoginController         內建登入端點（可覆寫）
└── vo
    ├── JwtLoginRequest
    └── JwtLoginResponse
```

| 類別 | 職責 |
|---|---|
| `JwtProperties` | `@ConfigurationProperties(prefix="security.jwt")`，承載 algorithm、secret、公私鑰路徑、過期秒數、登入端點路徑等設定 |
| `JwtTokenProvider` | 核心元件：依 `algorithm` 選 HS256/RS256，負責 `generateToken(username)` 與 `validateAndGetUsername(token)`；金鑰於初始化時依設定載入一次 |
| `JwtAuthenticationFilter` | `OncePerRequestFilter`，解析 `Authorization: Bearer`，驗 token → 查 `UserDetailsService` → 寫入 `SecurityContext` |
| `JwtLoginController` | 內建 `POST /api/login`，委派 `AuthenticationManager` 驗帳密後發 token；以 `@ConditionalOnMissingBean` 可覆寫 |
| `JwtLoginRequest` / `JwtLoginResponse` | 登入請求（username/password）與回應（token/tokenType）的 DTO |

### 現有類別的調整

- `com.zipe.enums.VerificationTypeEnum`：新增 `JWT` enum 值
- `com.zipe.autoconfiguration.SecurityConfiguration`：新增一條 `jwtLoginConfigure()` 分支（對應現有 `basicLoginConfigure` / `customLoginConfigure`），並把上述 Bean 以 `@ConditionalOnMissingBean` 條件化註冊，僅在 JWT 模式生效
- 需多註冊一個 `AuthenticationManager` Bean（由 `HttpSecurity` 取得或以現有 provider 組裝），供 JWT 模式驗帳密

## 設定屬性表

`@ConfigurationProperties(prefix = "security.jwt")`：

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `security.jwt.algorithm` | String | `HS256` | 簽章演算法，`HS256` 或 `RS256` |
| `security.jwt.secret` | String | （無） | HS256 用的對稱密鑰；**建議用環境變數注入**，HS256 模式必填 |
| `security.jwt.private-key-location` | String | （無） | RS256 用的私鑰路徑（簽 token）；支援 `classpath:` / `file:` |
| `security.jwt.public-key-location` | String | （無） | RS256 用的公鑰路徑（驗 token） |
| `security.jwt.expiration-seconds` | long | `3600` | access token 有效秒數（預設 1 小時） |
| `security.jwt.login-uri` | String | `/api/login` | 內建登入端點路徑 |
| `security.jwt.header` | String | `Authorization` | 讀取 token 的 header 名稱 |
| `security.jwt.token-prefix` | String | `Bearer ` | token 前綴 |
| `security.jwt.issuer` | String | （無，選填） | 寫入 `iss` claim，供驗證來源 |

- **啟動驗證（fail-fast）**：`JwtTokenProvider` 初始化時依 `algorithm` 檢查必要金鑰是否齊備——HS256 缺 `secret`、或 RS256 缺金鑰路徑時拋例外並給明確訊息（對齊現有 CUSTOM 模式缺 `custom-bean-name` 即拋例外的風格）。
- **安全預設**：`secret` 不給預設值，避免內建弱密鑰被誤用於正式環境。

## 錯誤處理

| 情境 | 行為 |
|---|---|
| 登入帳密錯誤 | `JwtLoginController` 回 `401`，沿用現有 `LoginFailureHandler` 的錯誤格式（或回一致的 JSON 錯誤體） |
| token 缺失 / 格式錯 / 過期 / 簽章不符 | `JwtAuthenticationFilter` 不寫 `SecurityContext`，放行給後續 → 由 Spring Security 回 `401`；過期與簽章錯誤分別 log（不洩漏細節給 client） |
| 啟動時金鑰設定不全 | fail-fast 拋例外，啟動失敗 |
| token 有效但使用者已停用/刪除 | `UserDetailsService` 查不到或 `UserNotActivatedException` → `401`（沿用現有例外） |

filter 不直接寫 response body，維持 Spring Security 既有的 `AuthenticationEntryPoint` 機制，讓 401 行為與其他模式一致。

## 測試策略

- `JwtTokenProvider`：HS256 / RS256 各自的簽發→驗證 round-trip、過期 token、竄改 token、錯誤金鑰
- `JwtAuthenticationFilter`：合法 token 寫入 SecurityContext、非法 token 不寫入
- 整合測試：`@SpringBootTest` 設 `verification-type=JWT`，跑「登入拿 token → 帶 token 存取受保護資源 → 401（無 token）」
- 在 `starters_example` 增一個 JWT 模式範例（對齊現有 CUSTOM 範例的做法）

## 文件同步（收尾前強制執行）

本次屬於「新增登入方式 + 新增設定屬性 + AutoConfiguration Bean 變更」，依 doc-sync 規則必須更新：

- `doc-site/docs/logon-starter/` 的 index / quickstart / configuration / examples / architecture 五份
- 根目錄與 `logon-spring-boot-starter/README.md`（若有列模組功能/設定）
- `starters_example` 範例 + `doc-site/docs/integration/`
- 改完 docs 後於 `doc-site/` 跑 `npm run build` 重產根目錄 `llms.txt` / `llms-full.txt` 一併 commit
- 以 `sync-starters-docs` skill 輔助判斷與產生
