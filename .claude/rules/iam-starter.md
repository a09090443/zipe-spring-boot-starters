# iam-spring-boot-starter

身分與授權管理 Starter，提供可儲存於資料庫的帳號（Account）— 群組（Group）— 權限（Permission）模型，並透過 logon-starter 的 `GrantedAuthoritiesResolver` 擴充點接上登入流程。相依方向為 iam → logon（單向）。

## 主要功能領域

JPA 持久化的帳號／群組／權限三層模型（兩兩多對多，表名 `iam_` 前綴）；`DbGrantedAuthoritiesResolver` 實作 logon 的 `GrantedAuthoritiesResolver` SPI，三種驗證模式共用 DB 授權；`IamUserDetailsService` 繼承 `BasicUserServiceImpl`，BASIC／JWT 模式以資料庫帳號取代內建 stub；帳號／群組／權限三組 Service（必備）與內建 REST Controller（可選，`iam.api.enabled` 控制）；全面 `@ConditionalOnMissingBean` 可覆寫。

## doc-site 文件導覽

工作於本模組時，依需求閱讀對應文件：

| 需求 | doc-site 文件 |
|---|---|
| 了解模組整體功能與主要類別清單 | [index.md](../../doc-site/docs/iam-starter/index.md) |
| 引入依賴、建表、接上登入並以權限保護端點 | [quickstart.md](../../doc-site/docs/iam-starter/quickstart.md) |
| 查詢 `iam.*`（enabled、api.enabled、api.base-path、group.role-prefix、ddl.init）屬性與資料表結構 | [configuration.md](../../doc-site/docs/iam-starter/configuration.md) |
| 查詢注入 Service、覆寫 Resolver、自訂帳號來源、LDAP + iam 授權的用法 | [examples.md](../../doc-site/docs/iam-starter/examples.md) |
| 了解套件結構、資料模型、與 logon 的整合機制、自動配置原理與擴充指南 | [architecture.md](../../doc-site/docs/iam-starter/architecture.md) |
