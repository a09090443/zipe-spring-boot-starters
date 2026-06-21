---
description: 程式碼異動後必須確認 doc-site 文件是否需要同步更新
paths:
  - base-spring-boot-starter/**
  - db-spring-boot-starter/**
  - job-spring-boot-starter/**
  - logon-spring-boot-starter/**
  - web-service-spring-boot-starter/**
  - web-spring-boot-starter/**
  - keycloak-spring-boot-starter/**
  - starters_example/**
  - example-kotlin/**
---

# 文件同步規則（doc-sync）

## 總則

完成任一模組程式碼變更後（提交或合併前），**必須對照 `doc-site/docs/` 目錄判斷文件是否需要同步更新**。

文件與程式碼必須保持一致，避免文件落後於實作。每次異動結束前，請依照本規則逐步確認。

---

## 強制執行 Gate（最高優先，不可略過）

文件同步是**完成定義（Definition of Done）的一部分**。只要本 session 動過下列任一路徑的程式碼，
在以下任一時機**之前**，都必須先完成文件同步檢查，否則視為任務未完成：

- ⛔ 執行 `git commit` / `git push` 之前
- ⛔ 對使用者宣稱「完成」「修好了」「都正常」「可以了」之前
- ⛔ 結束一段程式碼變更工作、準備收尾之前

涵蓋路徑：`*-spring-boot-starter/`、`starters_example/`、`example-kotlin/`（即本檔 frontmatter `paths` 所列）。

**需同步的文件範圍（缺一不可）：**
- `doc-site/docs/`（各 starter 與 integration 的技術文件）
- 根目錄 `README.md` 與**各模組** `*/README.md`（模組清單、版本號、功能概述、快速開始的依賴範例）
- 根目錄 `llms.txt` 與 `llms-full.txt`（LLM 友善彙整檔，**進版控**，由 build 重新產生後一併提交）

> 文件同步**不只是 doc-site**。README 常寫死版本號與模組清單，最容易被遺漏，務必一併檢查。

> **`llms.txt` 不可手改，但需 build 後提交**：根目錄的 `llms.txt` / `llms-full.txt` 由
> `docusaurus-plugin-llms` 在 `npm run build` 時自動從 `doc-site/docs/` 產生（輸出到 `doc-site/build/`），
> 再經 `postbuild` 腳本複製到 repo 根目錄作為**進版控的彙整副本**。因此**改完 docs 後，必須執行一次
> `npm run build` 讓根目錄彙整檔重新產生，並一併 commit**——詳見下方「LLM 文件（llms.txt）同步」一節。

### 每次必做的檢查步驟（Checklist）

收尾前，逐步執行並在回覆中明確交代結果：

1. **列出**本次所有變更的檔案與所屬模組。
2. **逐一對照**下方「各模組觸發條件與更新規則」，判斷每項變更是否需要更新文件。
3. **實際更新**所有判定為「需更新」的 `.md` 檔（不可只說「建議更新」而不做）。
4. **版本號專項檢查**：若本次**升級了任何依賴版本或專案自身版本**（含第三方套件、webjars、
   Spring Boot、starter artifact 版本等），務必對**整個 repo**（`doc-site/docs/`、根與各模組
   `README.md`、各 `pom.xml`）`grep` 該版本的**舊版本號**，確認所有寫死的版本（依賴範例、
   整合情境 `pom` 片段、模組清單表、版本對照表等）已一併同步。
   「版本升級不需更新文件」**僅適用於文件未寫死該版本號的情況**——只要文件寫死了，就必須改。
5. **明確回報**：哪些文件已同步、哪些判定為「不需同步」及其依據（對照「不需要更新文件的情況」）。

### 紅旗（出現以下念頭代表正在違規）

| 念頭 | 事實 |
|---|---|
| 「先 commit，文件之後再補」 | 文件同步是 commit 的前置條件，不是後續工作。 |
| 「這只是 bug fix，應該不用動文件」 | 仍須對照觸發表確認；AutoConfiguration 邏輯異動即使行為不變也要更新 architecture.md。 |
| 「使用者沒提到文件」 | 文件同步是預設義務，不需使用者提醒。使用者提醒時即代表已經漏掉。 |
| 「等使用者問再說」 | 收尾前主動執行，不等詢問。 |

> 若不確定某項變更是否需要更新文件，呼叫 `sync-starters-docs` skill 協助判斷——不要略過判斷。

---

---

## 文件結構對應表

| 模組程式碼目錄 | 對應 doc-site 文件目錄 |
|---|---|
| `base-spring-boot-starter/` | `doc-site/docs/base-starter/` |
| `db-spring-boot-starter/` | `doc-site/docs/db-starter/` |
| `job-spring-boot-starter/` | `doc-site/docs/job-starter/` |
| `logon-spring-boot-starter/` | `doc-site/docs/logon-starter/` |
| `web-service-spring-boot-starter/` | `doc-site/docs/web-service-starter/` |
| `web-spring-boot-starter/` | `doc-site/docs/web-starter/` |
| `keycloak-spring-boot-starter/` | （文件目錄待建立） |
| `starters_example/` | `doc-site/docs/integration/` |
| `example-kotlin/` | `doc-site/docs/example-kotlin/` |

每個 starter 文件目錄下包含以下五份文件：

| 檔案 | 說明 |
|---|---|
| `index.md` | 模組功能概述、主要類別表格 |
| `quickstart.md` | 引入依賴與基本設定步驟 |
| `configuration.md` | 設定屬性表（屬性鍵 / 預設值 / 說明） |
| `examples.md` | 可執行使用範例（方法簽章須與原始碼一致） |
| `architecture.md` | 套件結構、核心類別、自動配置原理、擴充與維護指南 |

---

## LLM 文件（llms.txt）同步

doc-site 已透過 `docusaurus-plugin-llms` 提供符合 [llmstxt.org](https://llmstxt.org) 標準的
LLM 友善文件，供 AI 助理 / IDE / RAG 高效消費。

### 運作方式

| 檔案 | 內容 | 進版控？ | 產生方式 |
|---|---|---|---|
| 根目錄 `llms.txt` | 文件索引（各頁標題 + 摘要 + 連結） | ✅ 是 | build 產生後由 `postbuild` 複製到根目錄 |
| 根目錄 `llms-full.txt` | 全部文件串接的單一全文 | ✅ 是 | 同上 |
| `doc-site/build/llms*.txt` | 部署用產物 | ❌ 否（gitignore） | `npm run build` 時自動產生 |

產生流程：

1. `docusaurus-plugin-llms`（設定於 `doc-site/docusaurus.config.js` 的 `plugins` 區段）在
   `npm run build` 時，從 `doc-site/docs/` 產生 `doc-site/build/llms.txt` 與 `llms-full.txt`。
2. `package.json` 的 `postbuild` 腳本（`scripts/copy-llms.mjs`）自動把這兩檔複製到 **repo 根目錄**，
   作為**進版控、平時開發可直接讀**的彙整副本。

要點：

- **內容直接來自 `doc-site/docs/`，不可手動編輯**根目錄的 `llms*.txt`；改的是 `docs/`，再 build。
- 每筆索引的**摘要取自各 `.md` 的 frontmatter `description` 或開頭段落**，所以寫 docs 時
  維持清楚的開頭描述，即可獲得高品質的 llms.txt 摘要。

### 收尾前必做（改過 docs 時）

> ⛔ 只要本 session 改動了 `doc-site/docs/`，commit 前**必須**執行一次 `npm run build`
> （於 `doc-site/` 目錄），讓根目錄 `llms.txt` / `llms-full.txt` 重新產生，並將更新後的這兩檔
> **一併 commit**。否則版控內的彙整檔會落後於 docs。

### 其他需人工確認的時機

1. **新增 / 移除整個 starter 模組或文件目錄**：新模組頁面會自動被收錄，無須改 plugin 設定，
   但應確認 build 回報的頁數正確（外掛會回報 `N total available documents processed`）。
2. **專案定位、模組清單或一句話簡介有變**：更新 `docusaurus.config.js` 內
   `docusaurus-plugin-llms` 的 `title` / `description`（此為 llms.txt 開頭的專案描述，非自動產生）。
3. **doc-site 部署網址（`url` / `organizationName`）變更**：llms.txt 內所有連結以此為前綴，
   變更後須重建，連結才會正確。

### 紅旗

| 念頭 | 事實 |
|---|---|
| 「llms.txt 內容過時了，手動改一下」 | 不要手改彙整檔；改 `docs/` 後 `npm run build` 即重新產生。 |
| 「改了 docs 就好，llms.txt 之後再說」 | 版控彙整檔需與 docs 同次提交；build 後一併 commit。 |
| 「加了新模組文件，llms.txt 不用管」 | 內容會自動收錄，但應 build 一次確認頁數，並檢視 plugin `description` 是否仍涵蓋新模組。 |

---

## 判斷流程：哪些變更需要更新文件？

### 需要更新文件

- 新增或修改對外（public）類別、方法或工具
- API 方法簽章變更（參數、回傳型別、方法名稱）
- 新增或修改 `application.yml` / `application.properties` 設定屬性
- 新增功能模組或使用情境
- 自動配置 Bean 新增、移除或行為改變（`AutoConfiguration` 類別變更）
- 新增架構面的擴充點（介面、抽象類別、SPI 機制）

### 不需要更新文件

- 純內部重構（private 方法調整，不影響對外 API、設定或行為）
- 新增或修改測試程式碼（`src/test/` 下的變更）
- 單純修正 Bug 且對外行為不變（修正後表現符合現有文件描述）
- 僅升級版本號且無 Breaking Change

---

## 各文件更新指引

### `index.md` — 模組概述

需更新時機：
- 新增主要功能或移除既有功能
- 主要類別清單有異動（新增、刪除、改名）

更新內容：
- 功能摘要段落
- 主要類別表格（類別名稱 / 功能說明）

---

### `quickstart.md` — 快速開始

需更新時機：
- Maven 依賴 `groupId`、`artifactId`、`version` 有異動
- 基本設定步驟或最小可執行範例有變化

更新內容：
- 依賴引入區塊（XML 格式）
- 必要設定欄位與初始使用步驟

---

### `configuration.md` — 設定屬性參考

需更新時機：
- 新增、移除或修改任何 `@ConfigurationProperties` 屬性
- 屬性預設值變更
- 屬性對應的行為說明有異動

更新內容：
- 屬性對照表（屬性鍵 / 型別 / 預設值 / 說明）

---

### `examples.md` — 使用範例

需更新時機：
- 對外 API 方法簽章有異動
- 新增可供使用者直接套用的功能情境

更新內容：
- 程式碼範例（方法簽章、呼叫方式須與原始碼 100% 一致）
- 情境說明與預期輸出

---

### `architecture.md` — 架構與開發指南

需更新時機：
- 套件結構（package）有異動
- 核心類別新增、移除或職責改變
- 自動配置邏輯（`AutoConfiguration`）有更動
- 新增擴充點或維護建議

更新內容：
- 套件結構樹狀圖
- 核心類別職責說明
- 自動配置原理說明
- 擴充與客製化指南

---

## 輔助工具

本專案已提供 **`sync-starters-docs` skill**，可輔助判斷哪些文件需要同步，並協助產生或更新文件內容。

觸發條件對照表：

| 程式碼異動類型 | 建議觸發 skill |
|---|---|
| 新增工具類別或對外 API | 是 |
| API 簽章變更 | 是 |
| 新增設定屬性 | 是 |
| 新增登入方式或連線類型 | 是 |
| 排程設定或執行方式變更 | 是 |
| 新增 REST / SOAP Endpoint | 是 |
| 自動配置 Bean 變更 | 是 |
| 純內部重構、測試異動 | 否 |

---

## 提交規範提醒

文件變更提交時，請遵循本專案的 [Git Commit 訊息規範](git-commit.md)：

- **類型**：使用 `文件`
- **範圍**：填入對應 starter 名稱（例如 `base-starter`、`db-starter`）
- **主旨**：簡述文件更新內容

範例：

```
文件(base-starter): 新增 AES-256 加解密工具的說明與範例
```

```
文件(db-starter): 更新多資料來源設定屬性對照表
```

---

## 撰寫規範

- 文件語言：**繁體中文（台灣用語）**
- 程式碼、類別名稱、方法名稱、設定屬性鍵：**保持原始英文**，不翻譯
- 程式碼範例中的方法簽章須與原始碼完全一致，不得自行修改或簡化
