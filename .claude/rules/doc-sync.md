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

涵蓋路徑：`*-spring-boot-starter/`、`starters_example/`（即本檔 frontmatter `paths` 所列）。

### 每次必做的檢查步驟（Checklist）

收尾前，逐步執行並在回覆中明確交代結果：

1. **列出**本次所有變更的檔案與所屬模組。
2. **逐一對照**下方「各模組觸發條件與更新規則」，判斷每項變更是否需要更新文件。
3. **實際更新**所有判定為「需更新」的 `.md` 檔（不可只說「建議更新」而不做）。
4. **明確回報**：哪些文件已同步、哪些判定為「不需同步」及其依據（對照「不需要更新文件的情況」）。

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

每個 starter 文件目錄下包含以下五份文件：

| 檔案 | 說明 |
|---|---|
| `index.md` | 模組功能概述、主要類別表格 |
| `quickstart.md` | 引入依賴與基本設定步驟 |
| `configuration.md` | 設定屬性表（屬性鍵 / 預設值 / 說明） |
| `examples.md` | 可執行使用範例（方法簽章須與原始碼一致） |
| `architecture.md` | 套件結構、核心類別、自動配置原理、擴充與維護指南 |

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
