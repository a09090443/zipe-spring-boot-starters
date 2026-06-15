# iam-spring-boot-starter 帳號群組權限管理設計

- **日期**：2026-06-15
- **模組**：`iam-spring-boot-starter`（新增，第 7 個主要 starter）
- **狀態**：設計確認，待實作

## 目標

新增一個獨立的「身分與存取管理（Identity & Access Management）」starter，提供可存入資料庫的**帳號、群組、權限**領域模型與 CRUD，讓引入的業務系統具備基本的權限管理能力；並與 `logon-spring-boot-starter` 整合，使登入帳號來源從寫死的 `admin` 改為資料庫帳號表，三種認證模式（BASIC / LDAP / CUSTOM）皆能取得權限。

核心原則：**最小核心（YAGNI）＋ 必須有明確的客製擴充點**。

## 已確認的決策

| 項目 | 決策 |
|---|---|
| 新舊 starter | **開新 starter** `iam-spring-boot-starter`，不塞進 logon |
| 命名 | `iam`（Identity & Access Management），涵蓋帳號＋群組＋權限 |
| 領域模型 | 帳號 ─ 群組（即角色）─ 權限，最單純三層 |
| 權限顆粒度 | 純「具名授權點」，與 API/URL 解耦，開發者自行決定用法 |
| 持久化 | JPA（Entity + Spring Data JPA Repository） |
| 對外成品程度 | 核心 Service/Repository 必給；REST Controller 可選、可關閉、可覆寫 |
| 權限檢查方式 | Spring Security authorities ＋ 註解式（`@PreAuthorize`） |
| 核心 SPI | `GrantedAuthoritiesResolver`（定義於 logon，由 iam 實作） |
| 與 logon 整合 | iam 編譯期依賴 logon；BASIC 覆寫 `BasicUserServiceImpl`，LDAP 經 resolver 補權限 |
| 客製機制 | 一律以 `@ConditionalOnMissingBean` + 同型別 Bean 覆寫 |

## 模組定位與職責分工

| Starter | 負責 |
|---|---|
| `logon-starter` | **認證機制**（怎麼登入：表單 / LDAP / JWT / CUSTOM）、Security Filter Chain |
| `iam-starter`（新） | **身分與授權資料**（帳號、群組、權限的儲存、CRUD、查詢、權限解析） |
| `db-starter` | 純資料源基礎建設（不放業務實體） |

**為什麼開新 starter：**

1. 關注點分離：logon 管「怎麼證明你是你」，iam 管「你是誰、你能做什麼」。兩者生命週期與依賴不同（iam 需要 JPA + DB，logon 不該被迫綁 JPA）。
2. 可獨立使用：有些系統用 Keycloak / SSO 認證，仍可用 iam 管自己的帳號群組權限。
3. 依賴方向乾淨：iam 依賴 logon 的 SPI 介面，logon 不知道 iam 存在。

## 資料模型（5 張表，前綴 `iam_`）

### `iam_account`（帳號）

| 欄位 | 型別 | 說明 |
|---|---|---|
| id | PK, auto | 主鍵 |
| username | unique | 登入帳號 |
| password | varchar | BCrypt 雜湊（LDAP 來源帳號此欄不使用） |
| display_name | varchar | 顯示名稱 |
| enabled | bool | 是否啟用（停用即不可登入） |
| locked | bool | 是否鎖定 |
| created_at / updated_at | timestamp | 稽核時間 |

### `iam_group`（群組＝角色）

| 欄位 | 型別 | 說明 |
|---|---|---|
| id | PK | 主鍵 |
| code | unique | 對應 Security authority（套用 `role-prefix`，如 `ROLE_ADMIN`） |
| name | varchar | 群組名稱 |
| description | varchar | 說明 |
| enabled | bool | 是否啟用 |

### `iam_permission`（權限）

| 欄位 | 型別 | 說明 |
|---|---|---|
| id | PK | 主鍵 |
| code | unique | authority 字串（如 `USER_CREATE`），供 `hasAuthority()` 或自行判斷 |
| name | varchar | 顯示名稱 |
| description | varchar | 說明 |
| enabled | bool | 是否啟用 |

> 權限刻意**去 API 化**：不含 `http_method` / `url_pattern`，純粹是抽象的具名授權點。
> 開發者可用於 `@PreAuthorize`、程式內手動判斷、選單/按鈕顯示控制等任意情境。

### 關聯表（多對多）

- `iam_account_group`：`account_id` ↔ `group_id`
- `iam_group_permission`：`group_id` ↔ `permission_id`

### Authorities 組成規則

登入後：帳號 → 所屬群組 → 群組的權限。最終 `authorities` =
- 各群組 `code`（套用 `iam.group.role-prefix`，預設 `ROLE_`，供 `hasRole()`）＋
- 各權限 `code`（供 `hasAuthority('USER_CREATE')`）。

### 刻意先不做（YAGNI）

帳號直掛權限、群組階層（父子）、權限分類、多租戶、實體欄位動態擴充、權限規則引擎/DSL。皆可日後加，不影響核心。

## 程式分層與元件

套件根 `com.zipe`，沿用既有 starter 分層慣例。

```
com.zipe
├─ entity/                 JPA 實體（Account / Group / Permission，關聯用 @ManyToMany）
├─ repository/             AccountRepository / GroupRepository / PermissionRepository（JpaRepository）
├─ service/               核心業務（interface + Impl，皆 @ConditionalOnMissingBean）
│   ├─ AccountService      建立/查詢/更新/停用帳號、加入退出群組、改密碼
│   ├─ GroupService        群組 CRUD、群組掛/卸權限
│   └─ PermissionService   權限 CRUD、查詢
├─ vo/                     DTO（CreateAccountRequest、AccountVO …，Service 回傳 DTO 而非 Entity）
├─ security/
│   ├─ IamUserDetailsService          extends logon 的 BasicUserServiceImpl（BASIC 接點）
│   └─ DbGrantedAuthoritiesResolver   實作 logon 的 GrantedAuthoritiesResolver（DB 查權限）
├─ controller/            可選、可關閉（iam.api.enabled）
│   ├─ AccountController / GroupController / PermissionController
├─ config/
│   └─ IamProperties       @ConfigurationProperties("iam")
└─ autoconfiguration/
    └─ IamAutoConfiguration  裝配上述 Bean，全部 @ConditionalOnMissingBean
                            （含 @EntityScan / @EnableJpaRepositories 指向 starter 套件）
```

關鍵設計：

1. 所有對外 Bean 都 `@ConditionalOnMissingBean` —— 客製的總開關。
2. Service 回傳 DTO/VO，避免 JPA 實體與 lazy proxy 漏到外層。
3. 引入即生效：`@EntityScan` / `@EnableJpaRepositories` 指向 starter 套件，免手動掃描。

## 與 logon-starter 的整合機制

以「authorities 解析 SPI」統一三種驗證模式。SPI 定義在 **logon**，由 iam 實作。

### logon-starter 需新增（小幅、向後相容）

```java
public interface GrantedAuthoritiesResolver {
    Collection<? extends GrantedAuthority> resolve(String username);
}
```

- 預設 Bean：回傳空集合（`@ConditionalOnMissingBean`），**完全保留 logon 現行行為**（不裝 iam 時 LDAP 仍為空權限）。
- 修改 `LdapUserDetailsService`：`buildAuthenticatedToken` 改呼叫 resolver 取得 authorities，取代目前寫死的 `Collections.emptyList()`。

### iam-starter 提供

- `DbGrantedAuthoritiesResolver implements GrantedAuthoritiesResolver`：查 DB（account → groups → permissions），以 `@ConditionalOnMissingBean` 蓋掉預設空實作。
- `IamUserDetailsService extends BasicUserServiceImpl`：BASIC 模式接點，`loadUserByUsername` 由 iam 帳號表提供 password / enabled / locked，並以同一個 resolver 取得 authorities。註冊為 `BasicUserServiceImpl` 型別 Bean（`@ConditionalOnMissingBean`），自動取代 logon 寫死的 admin。

### 三種 verification-type 的覆蓋

| 模式 | 認證 (authN) | 授權 (authZ) |
|---|---|---|
| **BASIC** | iam DB 密碼比對 | iam（resolver） |
| **LDAP** | LDAP 目錄 | **iam DB**（resolver 補群組權限） |
| **CUSTOM** | 開發者 provider | 可選用 resolver |

- LDAP：帳號**先在 iam 建檔**（username + 群組指派，password 欄位不使用），LDAP 驗完密碼後由 resolver 依 username 補上權限；LDAP 有但 iam 未建檔者 → 空權限。
- 效果：classpath 有 iam 時，BASIC 表單登入與 JWT 模式自動改用 DB 帳號表；LDAP 取得 DB 權限。

### 整合代價

需要動 logon：新增 `GrantedAuthoritiesResolver` 介面 + 預設空實作 Bean + 改 `LdapUserDetailsService` 一段。預設行為不變、向後相容。

## 擴充點（客製總覽）

一律以「在自己專案宣告同型別 Bean 覆寫 starter 預設」（`@ConditionalOnMissingBean`）。

| 想客製的東西 | 怎麼做 |
|---|---|
| 帳號→權限的對應規則 | 提供自己的 `GrantedAuthoritiesResolver` Bean |
| 帳號 CRUD 邏輯 | 覆寫 `AccountService` Bean |
| 群組/權限 CRUD 邏輯 | 覆寫 `GroupService` / `PermissionService` Bean |
| 密碼編碼方式 | 覆寫 `PasswordEncoder` Bean（沿用 logon 既有機制） |
| 內建 REST API 行為 | `iam.api.enabled=false` 自己寫，或覆寫單一 Controller Bean |
| 查詢方式 | 注入 Repository 自行加 query method |
| 權限的「用法」 | 權限是具名 authority，可用 `@PreAuthorize` / 手動判斷 / 選單控制 |

兩個層次：(1) 改規則（覆寫 resolver 或某 Service）；(2) 整碗自己接（只用 Entity + Repository 當資料層）。

**刻意不做成擴充點**：實體欄位動態擴充、權限階層引擎、規則 DSL。

## 設定屬性（`iam.*`）

| 屬性 | 預設 | 說明 |
|---|---|---|
| `iam.enabled` | `true` | 總開關，關閉則不裝配任何 iam Bean |
| `iam.api.enabled` | `true` | 是否啟用內建 REST Controller |
| `iam.api.base-path` | `/api/iam` | 內建 API 路由前綴 |
| `iam.group.role-prefix` | `ROLE_` | 群組 code 轉 authority 時的前綴 |
| `iam.ddl.init` | `false` | 是否由 starter 提供 `schema.sql` 自動建表 |

## 建表策略

- starter 內附 `schema-iam.sql`（標準 SQL，相容 H2 / MySQL / PostgreSQL）。
- 預設**不自動執行**（`iam.ddl.init=false`），交由業務專案以 Flyway/Liquibase 或手動建表掌控；範例專案開啟以利展示。
- **不靠 Hibernate `ddl-auto` 自動建表**（正式環境風險），但 Entity 仍正確標註欄位對應。

## 測試

- Repository：`@DataJpaTest` + H2 驗證 CRUD 與關聯查詢。
- `DbGrantedAuthoritiesResolver`：驗證帳號→群組→權限正確展開成 authorities。
- 整合：`@SpringBootTest` 驗證 BASIC 登入走 iam 帳號表、覆寫 Bean 生效。
- LDAP 授權：於 logon-starter 測試（mock resolver 驗證 authorities 注入 token）。

## 文件（依 doc-sync 規則）

- 新增 `doc-site/docs/iam-starter/`：`index` / `quickstart` / `configuration` / `examples` / `architecture` 五份。
- 新增 `.claude/rules/iam-starter.md`，並於根 `CLAUDE.md` 加連結。
- 更新根 `README.md` 模組清單（6 → 7 個 starter）。
- 更新 logon 文件：補 `GrantedAuthoritiesResolver` 擴充點與 LDAP 授權說明。
- `doc-site` build 重新產生根 `llms.txt` / `llms-full.txt` 並一併提交。
- `starters_example` 加一組 iam 帳號/群組/權限示範 + 對應 `integration/` 情境文件。

## 建構整合

- `iam-spring-boot-starter` 加入根 `pom.xml` 的 `<modules>` reactor 與 `dependencyManagement`。
- 子 pom `<parent>` 指向根 pom；依賴 `logon-spring-boot-starter` + `spring-boot-starter-data-jpa`。

## 實作順序建議

1. logon-starter：新增 `GrantedAuthoritiesResolver` SPI + 預設空實作 + 改 `LdapUserDetailsService`（向後相容，先確保既有測試通過）。
2. iam-starter：建模組骨架（pom、Entity、Repository、`schema-iam.sql`）。
3. iam-starter：Service 層 + DTO。
4. iam-starter：`security/`（`IamUserDetailsService`、`DbGrantedAuthoritiesResolver`）+ `IamAutoConfiguration`。
5. iam-starter：可選 Controller。
6. 測試（Repository → resolver → 整合）。
7. starters_example 示範 + 全套文件同步 + llms.txt 重建。
