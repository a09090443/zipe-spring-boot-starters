# basic 模式可自訂帳密設計

日期：2026-06-19
模組：logon-spring-boot-starter（連帶 starters_example、doc-site）

## 背景與問題

`logon-starter` 在 `verification-type=basic` 模式下，使用者來源是內建的
`BasicUserServiceImpl`，其帳號密碼**寫死為 `admin/admin`**（開發/測試用 stub），
無法由設定檔自訂。需求：讓 basic 模式可由 `application.yml` 自行設定帳密。

引入 iam-starter 時，`IamUserDetailsService`（繼承 `BasicUserServiceImpl`）會接管
帳號查詢、改查 `iam_account`；本設計針對「**未引入 iam**、純 logon basic」的情境。

## 設計決策（經 brainstorming 確認）

| 決策點 | 選擇 |
|---|---|
| 帳號範圍 | **多組帳號、各帶權限** |
| 沒設定時行為 | **保留 `admin/admin` fallback**（向後相容，現有測試不受影響） |
| 密碼格式 | **明文與 `{bcrypt}` 前綴皆支援** |
| 權限命名 | authorities 直接轉 `SimpleGrantedAuthority`，**不自動加 `ROLE_`** |

## 一、設定屬性結構

於 `SecurityPropertyConfig`（前綴 `security`）新增 `basic` 區塊：

```yaml
security:
  verification-type: basic
  basic:
    users:
      - username: user01
        password: 1234                  # 明文，啟動時自動編碼
        authorities: [admin, viewer]
      - username: user02
        password: '{bcrypt}$2a$10$....' # 已雜湊，直接採用
        authorities: [viewer]
```

新增型別：

- `SecurityPropertyConfig.basic` → `BasicUserPropertyConfig`，含 `List<BasicUser> users`
- `BasicUser`：`String username`、`String password`、`List<String> authorities`（預設空清單）

行為規則：

1. 有設定 `users` → `BasicUserServiceImpl` 從清單查帳號，取代寫死的 `admin/admin`。
2. 沒設定 `users` → fallback 回 `admin/admin`（向後相容）。
3. `authorities` 內字串直接轉 `SimpleGrantedAuthority`，不自動加 `ROLE_`。

## 二、密碼編碼：同時支援明文與 {bcrypt}

核心在於「驗證時用哪個 `PasswordEncoder` 比對」。

**現狀：** `passwordEncoder` Bean 為 `BCryptPasswordEncoder`，不認 `{bcrypt}` 前綴。

**做法：** 將預設 `passwordEncoder` Bean 改為
`PasswordEncoderFactories.createDelegatingPasswordEncoder()`（Spring 官方推薦）。
它依 `{id}` 前綴選 encoder，預設以 bcrypt 編碼新密碼，且向後相容既有 bcrypt 雜湊。

`BasicUserServiceImpl.loadUserByUsername` 邏輯：

- `password` 以 `{` 開頭（已含 `{id}` 前綴）→ 原值放進 `UserDetails`，交給
  DelegatingPasswordEncoder 依前綴比對。
- 否則視為明文 → 用 `passwordEncoder.encode(明文)` 產生 `{bcrypt}...` 再放進 `UserDetails`。

**影響面：** 改 `passwordEncoder` Bean 型別會連帶影響 iam / custom / ldap（共用同一 Bean）。
DelegatingPasswordEncoder 對既有 bcrypt 雜湊完全相容，風險低；需驗證現有測試有無寫死
`instanceof BCryptPasswordEncoder` 的假設。

**已否決替代方案：** 「只在 BasicUserServiceImpl 內部去 `{bcrypt}` 前綴後比對」——hack 且
只修 basic、不一致，不採用。

## 三、測試

logon-starter：

- `BasicUserServiceImpl` 新測試：①有設定 `users` 時依 username 載入，明文與 `{bcrypt}`
  兩種密碼皆能比對成功；②查無帳號拋 `UsernameNotFoundException`；③沒設定 `users` 時
  fallback 回 `admin/admin`。
- 既有測試回歸：`JwtSecurityIntegrationTest` 等，確認改用 DelegatingPasswordEncoder 後
  basic 表單登入與 JWT 登入仍正常。
- 檢查並調整現有測試中寫死 `BCryptPasswordEncoder` 型別的假設。

## 四、範例專案（starters_example）

不改範例預設模式（維持 custom + iam，避免破壞 iam 示範），僅在 `application.yml` 以
**註解形式**補一段 `security.basic.users` 範例，說明「不帶 iam、純 basic」時如何自訂帳密。

## 五、文件同步（doc-sync gate，必做）

- `logon-starter/configuration.md` — 新增 `security.basic.users[]` 屬性表。
- `logon-starter/examples.md` — 新增「basic 模式自訂帳密」範例。
- `logon-starter/architecture.md` — 更新 `BasicUserServiceImpl` 職責與 `PasswordEncoder`
  改為 DelegatingPasswordEncoder 的說明。
- `logon-starter/index.md` — 視措辭更新主要類別說明。
- README（根 + logon 模組）查有無寫死 `admin/admin`。
- 改完 docs 後於 `doc-site/` 跑 `npm run build`，連同根目錄 `llms.txt`/`llms-full.txt`
  一起 commit。
