---
name: release-to-maven-central
description: Use when asked to publish, release, or deploy zipe-spring-boot-starters to Maven Central (e.g.「發布至 maven central」「出新版」「release 4.x.x.x」)。Covers the GitHub-Action-driven release flow, version/tag rules, branch flow, and pre-flight irreversibility checks.
---

# 發布至 Maven Central

## 總則

**發布由 GitHub Action 驅動，不在本地跑 `mvn deploy`。** 觸發方式是在 GitHub 建立 **Release（含 tag）**，workflow `.github/workflows/publish-maven-central.yml`（`on: release: published`）會接手：以 tag 覆寫版本 → 簽章 → `mvn -Prelease clean deploy` → `autoPublish` 正式發布 → 回寫版本至預設分支。

**`autoPublish=true` 代表上傳成功即不可逆**：版本一旦上 Central 永遠無法刪除或覆蓋。觸發前務必與使用者確認版本號，並確認該版本尚未發布。

> 別自己在本地 `mvn -Prelease clean deploy`：本機通常無 `~/.m2/settings.xml` 的 Central token 與 GPG 密碼，憑證只存在於 GitHub Secrets。

## 版本與 tag 規則

- tag 格式：`v<版本>` 或 `<版本>`（開頭 `v` 會自動去除）。
- 版本沿用 `<SpringBoot 版本>.<流水號>`，例如 `v4.0.0.1`、`v3.5.14.1`。
- **tag 名即發布版本**：workflow 於 build 前以 tag 覆寫各模組版本（`versions:set`），故 `pom.xml` 不必先手動改版；發布後會把該版本回寫預設分支（pom 已是該版本則略過）。

## 執行順序

1. **確認版本未發布**：查 Central metadata，避免撞號（撞號會發布失敗）。
   ```bash
   curl -s https://repo1.maven.org/maven2/io/github/a09090443/zipe-spring-boot-starters/maven-metadata.xml
   ```
2. **確認 Secrets 已設**（Settings → Secrets → Actions，缺一不可）：
   `MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`、`GPG_PRIVATE_KEY`、`GPG_PASSPHRASE`。
3. **整理待發布的程式於分支**（流程 feature → develop → master，**發布走 master**）：
   - 把未提交變更 commit 至 `develop`（訊息 **REQUIRED SUB-SKILL:** 使用 git-commit-message），push。
   - 合併 `develop` → `master`：先 `git checkout master && git pull`，再 `git merge develop`（若已分叉用一般／`--no-ff` 合併，不可硬 `--ff-only`），`git push origin master`。
4. **向使用者確認後**建立 Release 觸發發布（不可逆）：
   ```bash
   gh release create v4.0.0.1 --target master --title "v4.0.0.1" --notes "<發布摘要>"
   ```
5. **監看 workflow** 直到成功：
   ```bash
   gh run list --workflow=publish-maven-central.yml --limit 3
   gh run watch <run-id> --exit-status --compact
   ```
6. **驗證**：
   - Central 同步至 `repo1.maven.org` 有延遲（HEAD 該版本 .pom，404 屬正常，通常 10–30 分鐘）。
   - 版本回寫：master pom 若本就等於該版本，workflow 會「略過回寫」，**不會**有回寫 commit，屬正常。

## 紅旗（出現以下念頭代表方向錯了）

| 念頭 | 事實 |
|---|---|
| 「在本地跑 `mvn -Prelease deploy` 發布」 | 憑證在 GitHub Secrets，本機通常無 settings.xml；發布走 GitHub Release 觸發 Action。 |
| 「直接在 develop 打 tag 發布」 | 發布走 master；先把碼合併進 master 再於 master 建 Release。 |
| 「沿用上次版本號重發」 | Central 不可覆蓋，撞號會失敗；務必遞增流水號。 |
| 「建 Release 前不用問」 | `autoPublish=true` 不可逆，建立 Release 前須向使用者確認版本與內容。 |
| 「pom 要先手動改成新版」 | 不必，tag 會覆寫版本；發布後自動回寫預設分支。 |

## 常見錯誤

- **`git merge --ff-only` 失敗就卡住**：master 與 develop 常已分叉（master 含先前合併 commit），改用一般合併（`git merge develop` 或 `--no-ff`）。
- **以為沒回寫 commit 是壞了**：master pom 已等於發布版本時 workflow 故意略過回寫，正常。
- **發布後立刻找不到於 repo1**：CDN 同步有延遲，先看 https://central.sonatype.com，repo1 稍後才同步。

## 相關

- workflow：`.github/workflows/publish-maven-central.yml`（檔頭註解為權威說明）。
- 發布前的本地品質把關：**REQUIRED SUB-SKILL:** 使用 local-quality-gate。
- 文件部署為另一條獨立 workflow：`.github/workflows/deploy-doc-site.yml`。
