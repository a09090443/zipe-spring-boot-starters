---
name: scan-starter-drift
description: Use to audit all zipe-spring-boot-starters modules for convention drift against the authoring-a-starter golden rules — run periodically, before a release, or after adding a starter, to detect packages/naming/overridability/registration that diverge and propose fixes (harness entropy-management / garbage-collection principle).
---

# Starter 漂移掃描（scan-starter-drift）

## 為什麼有這份 skill（harness 原則 6：熵管理 / 垃圾回收）

跨 7 個 starter 的慣例會隨時間漂移。本 skill 以機械化方式對照
[`authoring-a-starter`](../authoring-a-starter/SKILL.md) 黃金規範掃描全專案，列出偏離項並提修正建議。
**定位是「掃描＋回報＋提案」，不自動改碼**；確認後再依規範修正。

## 何時跑

- 發版前；新增 starter 後；定期健檢；或懷疑慣例不一致時。

## 掃描程序（逐項執行，彙整成報告）

於專案根目錄執行（PowerShell 用 Bash 工具跑下列 sh 片段）：

### A. 自動配置註冊方式

```bash
# 應使用新式 .imports；出現 spring.factories（keycloak 除外）即為漂移
find . -path "*META-INF/spring.factories" | grep -v target | grep -v keycloak
```

### B. 自動配置類命名（應以 AutoConfiguration 結尾）

```bash
# 列出所有登記的自動配置類全名，目視非 *AutoConfiguration 結尾者
for f in $(find . -path "*src/main/resources/META-INF/spring*" -name "*.imports"); do
  echo "## $(echo "$f" | cut -d/ -f2)"; cat "$f"; echo;
done
```
> 已知漂移基線：logon 的 `SecurityConfiguration` 未循 `*AutoConfiguration`。

### C. 自動配置套件（應為 com.zipe.autoconfiguration）

```bash
grep -rl "@AutoConfiguration" */src/main/java | sed -E 's#(.*/java/)([^/]+/.*)/[^/]+$#\2#' | sort -u
```
> 已知漂移基線：job 用 `com.zipe.quartz.autoconfiguration`，其餘為 `com.zipe.autoconfiguration`。

### D. 可覆寫性（@ConditionalOnMissingBean 覆蓋）

```bash
for d in *-spring-boot-starter; do
  n=$(grep -rl "ConditionalOnMissingBean" "$d/src/main/java" 2>/dev/null | wc -l)
  echo "$d: $n 檔"
done
```
> 已知漂移基線：base/db/job/web/web-service 為 0，缺可覆寫契約。

### E. 設定屬性前綴風格（= 兩側空白一致）

```bash
grep -rhoE '@ConfigurationProperties\(prefix\s*=\s*"[^"]+"' */src/main/java | sort | uniq -c
```
> 已知漂移基線：部分 `prefix="..."` 無空白（web.resource、web.service.map、keycloak.custom）。

### F. ArchUnit 結構測試覆蓋（每個 starter 應都有）

```bash
for d in *-spring-boot-starter; do
  test -f "$d/src/test/java/com/zipe/architecture/StarterArchitectureTest.java" \
    && echo "$d: 有" || echo "$d: 缺"
done
```
> 基線：7 個 reactor starter 皆已具備（內容相同）；新增 starter 時依 authoring-a-starter 規則 6 一併複製。

## 產出報告格式

掃描後輸出一張表：

| 漂移項 | 模組 | 現況 | 黃金規範 | 建議 |
|---|---|---|---|---|

並標示哪些是「已知歷史漂移基線」（改動已發佈類名／套件有破壞性風險，需評估）
vs「新出現漂移」（應在本次修掉）。

## 紅旗

| 念頭 | 事實 |
|---|---|
| 「掃到就直接改類名」 | 改已發佈的自動配置類名／套件是破壞性變更，先評估相容性，別在掃描中順手改。 |
| 「基線漂移存在很久了，當作正常」 | 它仍是漂移；新模組不可仿，且應排入計畫性清理，而非擴散。 |
| 「沒有自動化就略過」 | 本 skill 就是定期人工觸發的 GC；不跑＝任由熵增。 |
