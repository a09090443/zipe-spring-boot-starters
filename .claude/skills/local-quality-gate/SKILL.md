---
name: local-quality-gate
description: Use when finishing a code change in any zipe-spring-boot-starters module and about to commit, push, or tell the user the work is done — especially after editing Java under any *-spring-boot-starter module.
---

# 本地品質 Gate（commit 前自我修正迴路）

## 總則

CI(`.github/workflows/ci.yml`)會在 push／PR 時跑 `mvn verify`,其中 **Spotless `check` 綁在 compile 階段**——只要格式未正規化,CI 必紅。本 skill 是對應的**前饋指引**:在本地先把問題擋下,提高首次通過率,而不是推上去才被 CI 發現。

**動過任一 `*-spring-boot-starter` 程式碼後,在 commit／push／宣稱「完成」之前,依序執行下列步驟。**

## 執行順序

1. **格式正規化**:`mvn -B spotless:apply`
   - 自動修 import 排序、未用 import、尾端空白、檔尾換行。**先做這步**,否則 `mvn verify` 的 spotless check 會失敗。
2. **完整 gate**:`mvn -B verify`
   - 編譯 + Spotless check + 全部測試 + JaCoCo。必須看到 **BUILD SUCCESS**(exit 0)才算過。
   - 只改單一模組可用 `mvn -B -pl <module> -am verify` 加速,但收尾前仍應跑一次全 reactor。
3. **覆蓋率不可倒退**:開 `<module>/target/site/jacoco/index.html`,確認**你動到的類別**覆蓋率沒有因新增未測程式碼而下降。
4. **文件同步**:若變更涉及對外 API／設定屬性／行為改變,**REQUIRED SUB-SKILL:** 使用 sync-starters-docs;判斷準則見 `.claude/rules/doc-sync.md`(純內部重構／測試／無行為變更的 bugfix 不需要)。
5. **才能提交**:撰寫訊息時 **REQUIRED SUB-SKILL:** 使用 git-commit-message。目前在 `master` 則先開 feature 分支(流程:feature → develop → master)。

## 紅旗(出現以下念頭代表正在違規)

| 念頭 | 事實 |
|---|---|
| 「格式之後再修,先 commit」 | spotless check 綁 compile,CI 會立刻紅。先 `spotless:apply`。 |
| 「測試讓 CI 跑就好」 | 本地 `verify` 才能在 commit 前擋下,這正是「首次通過率」的意義。 |
| 「只是小改,不用 verify」 | 小改也可能弄壞編譯或既有測試。verify 一次,不靠運氣。 |
| 「覆蓋率不關我的事」 | 新增程式碼若無測試會稀釋覆蓋率,讓「測試通過」的綠燈失真。 |

## 常見錯誤

- **跳過步驟 1 直接 verify**:既有未正規化的檔會讓 spotless check 紅,誤以為是自己改壞。先 `spotless:apply`。
- **只 compile 不 verify**:compile 不跑測試,擋不住行為迴歸。
- **在 `master` 直接 commit**:違反分支流程,先開 feature 分支。
