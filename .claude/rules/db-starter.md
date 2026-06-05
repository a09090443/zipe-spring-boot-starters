---
description: db-spring-boot-starter 的目錄結構與功能說明
paths:
  - db-spring-boot-starter/**
---

# db-spring-boot-starter

資料庫功能的 Starter，支援單一或多個資料來源的動態切換。

## 目錄結構

```
db-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── autoconfiguration/
    │   ├── DataSourceAspectAutoConfiguration.java   # AOP 切面自動配置
    │   └── DataSourceConfigAutoConfiguration.java   # 資料來源自動配置
    ├── base/
    │   ├── annotation/
    │   │   ├── AnnotationHelper.java                # Annotation 工具
    │   │   ├── DS.java                              # 指定資料來源 Annotation
    │   │   └── DynamicDS.java                       # 動態資料來源 Annotation
    │   ├── aspect/
    │   │   └── DynamicDataSourceAspect.java         # 動態切換 AOP 切面
    │   ├── config/
    │   │   ├── DataSourcePropertyConfig.java        # 資料來源屬性設定
    │   │   └── P6SpyLogger.java                     # P6Spy SQL 日誌
    │   ├── database/
    │   │   ├── BaseDataSourceConfig.java            # 基礎資料來源設定
    │   │   ├── DataSourceHolder.java                # ThreadLocal 資料來源持有者
    │   │   └── DynamicDataSource.java               # 動態資料來源實作
    │   └── model/
    │       └── DynamicDataSourceConfig.java         # 動態資料來源設定模型
    ├── common/model/
    │   └── SqlQuery.java                            # SQL 查詢模型
    ├── enums/
    │   └── ResourceEnum.java                        # 資源類型 Enum
    └── jdbc/
        ├── BaseJDBC.java                            # 基礎 JDBC 操作類別
        └── criteria/
            ├── Conditions.java                      # 查詢條件
            ├── Paging.java                          # 分頁條件
            ├── Pair.java                            # 鍵值對
            └── SQL.java                             # SQL 建構器
```

## 主要功能

多資料來源動態切換 (`@DS` Annotation)、基礎 JDBC 封裝、SQL 查詢條件建構、P6Spy SQL 監控日誌

## 對應文件

本模組的完整使用說明與開發指南請參閱 doc-site 技術文件：

- [模組簡介](../../doc-site/docs/db-starter/index.md)
- [快速開始](../../doc-site/docs/db-starter/quickstart.md)
- [配置參考](../../doc-site/docs/db-starter/configuration.md)
- [使用範例](../../doc-site/docs/db-starter/examples.md)
- [架構與開發指南](../../doc-site/docs/db-starter/architecture.md)
