# AGENTS.md

> 跨工具的 AI agent 導覽入口（harness「Maps Not Manuals」原則）。
> 本檔只是**地圖**：指向權威來源，不複製內容。詳細規範一律以連結目標為準。

## 這是什麼專案

`zipe-spring-boot-starters`：自製 Spring Boot Starter 集合，多模組 Maven reactor，發佈至 Maven Central。
Java 17+、Spring Boot 3.5.x。完整總覽見 [.claude/rules/project-overview.md](.claude/rules/project-overview.md)。

## 模組地圖

| 模組 | 功能 | 規範 |
|---|---|---|
| `base-spring-boot-starter` | 通用工具、共用基礎層 | [base-starter.md](.claude/rules/base-starter.md) |
| `db-spring-boot-starter` | 資料庫動態連接 | [db-starter.md](.claude/rules/db-starter.md) |
| `job-spring-boot-starter` | Quartz 排程 | [job-starter.md](.claude/rules/job-starter.md) |
| `logon-spring-boot-starter` | 登入認證（Security/LDAP/JWT） | [logon-starter.md](.claude/rules/logon-starter.md) |
| `iam-spring-boot-starter` | 帳號／群組／權限（整合 logon） | [iam-starter.md](.claude/rules/iam-starter.md) |
| `web-service-spring-boot-starter` | CXF WebService | [web-service-starter.md](.claude/rules/web-service-starter.md) |
| `web-spring-boot-starter` | 前端顯示（JSP/Thymeleaf） | [web-starter.md](.claude/rules/web-starter.md) |
| `keycloak-spring-boot-starter` | Keycloak 嵌入式（reactor 外） | [keycloak-starter.md](.claude/rules/keycloak-starter.md) |
| `starters_example` / `example-kotlin` | 整合範例（reactor 外） | [starters-example.md](.claude/rules/starters-example.md) |

## 工作流程（必讀 skill）

agent 動手前依情境載入對應 skill（位於 `.claude/skills/`）：

| 情境 | skill |
|---|---|
| 新增 starter／自動配置／設定屬性 | `authoring-a-starter` — 自動配置黃金規範 |
| 盤點 7 個 starter 的慣例漂移 | `scan-starter-drift` — 熵管理掃描 |
| 改完程式、commit／宣稱完成前 | `local-quality-gate` — 本地品質 Gate |
| 程式異動後同步 doc-site／README／llms.txt | `sync-starters-docs` — 文件同步 |
| 撰寫 commit 訊息 | `git-commit-message` |

## 機械化強制（mechanical enforcement）

不靠自律，由建構流程攔截違規：

- **格式**：Spotless（`mvn spotless:apply` 修正；check 綁 compile，CI 必跑）
- **覆蓋率**：JaCoCo 報告（刻意未設門檻，僅彙整於 CI Summary）
- **架構**：ArchUnit 結構測試，守住自動配置慣例。7 個 reactor starter 各有一份
  `src/test/java/com/zipe/architecture/StarterArchitectureTest.java`（內容相同），`mvn verify` 即執行
- **CI**：[.github/workflows/ci.yml](.github/workflows/ci.yml)（`mvn -B verify` + 文件同步提醒）

## 不可違反的規範

- **回應與文件**：台灣用語繁體中文；程式碼識別字、類別／方法／設定鍵維持原文
- **相依方向**：`iam → logon` 單向，不可反向
- **分支流程**：feature → develop → master，不可直接進 master
- **完成定義**：改 starter 程式碼後，文件同步是 commit 前置條件（見 [doc-sync.md](.claude/rules/doc-sync.md)）

> Claude Code 使用者：本檔與 `CLAUDE.md` 互補；`CLAUDE.md` 為 Claude 專屬入口，本檔為跨工具通用入口，兩者皆指回 `.claude/rules/`。
