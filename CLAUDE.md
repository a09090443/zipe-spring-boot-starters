# zipe-spring-boot-starters

自製的 Spring Boot Starter 集合專案，提供各種常用功能的自動配置模組，供各業務系統引入使用。

跨工具的 agent 導覽入口見 [AGENTS.md](AGENTS.md)（harness「Maps Not Manuals」地圖）。

詳細規則請參閱 `.claude/rules/` 目錄：

- [專案總覽、技術規格、常用指令](.claude/rules/project-overview.md)
- [base-spring-boot-starter](.claude/rules/base-starter.md)
- [db-spring-boot-starter](.claude/rules/db-starter.md)
- [job-spring-boot-starter](.claude/rules/job-starter.md)
- [logon-spring-boot-starter](.claude/rules/logon-starter.md)
- [iam-spring-boot-starter](.claude/rules/iam-starter.md)
- [web-service-spring-boot-starter](.claude/rules/web-service-starter.md)
- [web-spring-boot-starter](.claude/rules/web-starter.md)
- [keycloak-spring-boot-starter](.claude/rules/keycloak-starter.md)
- [starters_example](.claude/rules/starters-example.md)
- [文件同步規則（改程式後確認文件）](.claude/rules/doc-sync.md)

> Git Commit 訊息規範已改為專案 skill：`.claude/skills/git-commit-message/`（撰寫提交訊息時自動套用）。
>
> 改完程式、commit 前的本地品質 Gate（`spotless:apply` → `mvn verify` → 覆蓋率 → 文件同步）見專案 skill：`.claude/skills/local-quality-gate/`。
>
> 新增 starter／自動配置時遵循黃金規範 skill：`.claude/skills/authoring-a-starter/`；定期以 `.claude/skills/scan-starter-drift/` 掃描跨模組慣例漂移（harness 熵管理）。
