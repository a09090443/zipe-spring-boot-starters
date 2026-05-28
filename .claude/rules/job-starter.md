---
description: job-spring-boot-starter 的目錄結構與功能說明
paths:
  - job-spring-boot-starter/**
---

# job-spring-boot-starter

排程功能的 Starter，基於 Quartz 框架，支援資料庫或記憶體兩種儲存模式。

## 目錄結構

```
job-spring-boot-starter/
└── src/main/java/com/zipe/
    └── quartz/
        ├── autoconfiguration/
        │   ├── DataSourceAutoConfiguration.java     # Quartz 資料來源自動配置
        │   └── InitialJobAutoConfiguration.java     # 初始化排程自動配置
        ├── base/
        │   └── BaseJob.java                         # 排程基礎類別
        ├── config/
        │   ├── QuartzDataSourceProperties.java      # Quartz 資料來源屬性
        │   └── QuartzJobPropertyConfig.java         # Quartz 排程屬性設定
        ├── controller/
        │   └── QuartzController.java                # 排程管理 REST API
        ├── enums/
        │   ├── ScheduleEnum.java                    # 排程類型 Enum
        │   └── ScheduleJobStatusEnum.java           # 排程狀態 Enum
        ├── job/
        │   ├── HelloWorldJob.java                   # 範例排程 (Hello World)
        │   ├── QuartzJobFactory.java                # Quartz Job 工廠
        │   └── TestJob.java                         # 測試排程
        ├── model/
        │   └── Job.java                             # 排程資料模型
        ├── util/
        │   └── QuartzJobUtil.java                   # 排程管理工具
        └── vo/
            └── ScheduleJobVO.java                   # 排程 View Object
```

## 主要功能

Quartz 排程管理 (新增/修改/刪除/暫停/恢復)、支援 JDBC 或記憶體 JobStore、排程 REST API、可繼承 `BaseJob` 自訂業務邏輯
