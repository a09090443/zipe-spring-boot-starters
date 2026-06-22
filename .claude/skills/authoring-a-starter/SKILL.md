---
name: authoring-a-starter
description: Use when creating a new *-spring-boot-starter module, adding a new @AutoConfiguration class, or adding @ConfigurationProperties to any zipe-spring-boot-starters module — encodes the golden conventions a starter must follow so agents replicate the established pattern instead of drifting.
---

# Starter 黃金規範（authoring-a-starter）

## 為什麼有這份 skill（harness 原則 2 + 6）

agent 會複製既有模式——好的壞的都複製。本 skill 把「一個合格 starter 長怎樣」明文化為
**可重複套用的黃金規範**，讓新增模組／自動配置時有單一權威，避免慣例漂移（entropy）。
規範由現有 7 個 starter 歸納而來；偏離既有慣例前，先讀這裡。

> 配套：寫完用 `scan-starter-drift` 驗證未引入新漂移；收尾用 `local-quality-gate`；
> 對外變更用 `sync-starters-docs` 同步文件。

## 黃金規範（GOLDEN RULES）

### 1. 模組與 pom

- 目錄與 artifactId：`<功能>-spring-boot-starter`。
- `<parent>` 指向根 `pom.xml`；**子 pom 不寫第三方版本**，版本一律由根 `dependencyManagement` 管理。
- 新增模組要加進根 `pom.xml` 的 `<modules>`（reactor）與 `dependencyManagement`（內部 starter 區塊）。
- `spring-boot-configuration-processor` 設 `<optional>true</optional>`，產生設定屬性 metadata。

### 2. 套件結構

- 套件根一律 `com.zipe.*`。
- 自動配置類放 **`com.zipe.autoconfiguration`**。
  （現存 job 用 `com.zipe.quartz.autoconfiguration` 屬歷史漂移，新模組勿仿。）

### 3. 自動配置類

- 標註 `@AutoConfiguration`（**不是** `@Configuration`）。
- 類名以 **`AutoConfiguration`** 結尾（例：`FooAutoConfiguration`）。
  （現存 logon 的 `SecurityConfiguration` 屬歷史漂移，新類別勿仿。）
- 以 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  逐行登記**完整類名**。**不要用舊式 `spring.factories`**（keycloak 為 reactor 外的歷史例外）。
- 條件化啟用：適時加 `@ConditionalOnClass` / `@ConditionalOnProperty` / `@ConditionalOnResource`。

### 4. 可覆寫（這是 starter 的核心契約）

- 對外公開、使用方可能想自訂的 Bean，**一律加 `@ConditionalOnMissingBean`**，讓應用端能覆寫。
  （現存 base/db/job/web/web-service 較少用，新模組請補齊；iam/logon 為正面範例。）

### 5. 設定屬性

- 以 `@ConfigurationProperties(prefix = "<模組前綴>")` 集中設定，前綴用模組語意名（如 `iam`、`web`、`quartz`）。
- 風格統一：`prefix = "..."`（等號兩側留空白）。
- 屬性類別為 `public`（否則綁定失敗，ArchUnit 會擋）。

### 6. 測試（機械化強制）

- 引入 `spring-boot-starter-test`（test scope）。
- **複製 ArchUnit 結構測試**：把 `base-spring-boot-starter/.../architecture/StarterArchitectureTest.java`
  複製到新模組同路徑（`src/test/java/com/zipe/architecture/`），並在子 pom 加
  `com.tngtech.archunit:archunit-junit5`（test scope，版本由根 pom 管理）。
- 為對外類別補單元測試，避免稀釋 JaCoCo 覆蓋率。

### 7. 文件（完成定義的一部分）

- 建 `doc-site/docs/<模組>-starter/` 五件套：`index / quickstart / configuration / examples / architecture`。
- 更新 `.claude/rules/<模組>-starter.md`、`AGENTS.md` 模組地圖、根與模組 `README.md`。
- 跑 `npm run build`（於 doc-site/）重產根目錄 `llms.txt` / `llms-full.txt` 並一併 commit。
  細節依 `sync-starters-docs` 與 `.claude/rules/doc-sync.md`。

## 新增 starter 檢查清單（逐項做，回報結果）

1. [ ] 建模組目錄與子 pom（parent 指根、無第三方版本、configuration-processor optional）
2. [ ] 根 pom 加入 `<modules>` 與 `dependencyManagement` 內部 starter 項
3. [ ] 自動配置類置於 `com.zipe.autoconfiguration`、`@AutoConfiguration`、類名 `*AutoConfiguration`
4. [ ] 建 `AutoConfiguration.imports` 並登記完整類名
5. [ ] 可覆寫 Bean 加 `@ConditionalOnMissingBean`
6. [ ] `@ConfigurationProperties(prefix = "...")` 為 public、前綴用模組名
7. [ ] 複製 `StarterArchitectureTest` + 加 archunit-junit5 test 依賴
8. [ ] 對外類別補單元測試
9. [ ] 建五件套文件 + 更新 rules/AGENTS/README + 重產 llms.txt
10. [ ] 跑 `scan-starter-drift` 確認無新漂移；再走 `local-quality-gate` 收尾

## 紅旗

| 念頭 | 事實 |
|---|---|
| 「用 `@Configuration` 就好」 | starter 自動配置要 `@AutoConfiguration` 才會被 imports 機制載入。 |
| 「Bean 直接註冊，使用方不會想換」 | starter 的價值在可覆寫；少了 `@ConditionalOnMissingBean` 使用方無法客製。 |
| 「ArchUnit 測試太麻煩，先略過」 | 那就失去機械化強制，下一個 agent 又會漂移。複製一份即可。 |
| 「照 job/logon 既有寫法抄」 | 那兩處有歷史漂移（套件、命名）；以本規範為準，不是抄最近的檔。 |
