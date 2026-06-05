---
description: starters_example 整合測試範例專案的目錄結構與功能說明
paths:
  - starters_example/**
---

# starters_example（整合測試範例）

整合所有 Starter 的測試範例專案，示範各 Starter 的使用方式與相互配合。

## 目錄結構

```
starters_example/
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │   ├── Application.java                     # 應用程式入口
    │   │   ├── config/
    │   │   │   └── LogonLogRecord.java              # 自訂登入日誌 (logon-starter)
    │   │   ├── controller/
    │   │   │   ├── RestfulController.java           # REST API 控制器
    │   │   │   └── WebController.java               # Web 頁面控制器
    │   │   ├── jdbc/
    │   │   │   └── ExampleJdbc.java                 # JDBC 查詢範例 (db-starter)
    │   │   ├── job/
    │   │   │   ├── ExampleAnnotationJob.java        # Annotation (@Scheduled) 方式排程
    │   │   │   ├── ExampleDbJob.java                # DB 模式排程（REST API 動態註冊）
    │   │   │   └── ExampleXmlJob.java               # Properties (quartz-jobs.properties) 設定排程
    │   │   ├── model/
    │   │   │   ├── UserDetail.java                  # 使用者詳細資料模型
    │   │   │   └── UserMain.java                    # 使用者主要資料模型
    │   │   ├── repository/
    │   │   │   ├── UserDetailRepository.java        # 使用者詳細資料 Repository
    │   │   │   └── UserMainRepository.java          # 使用者主要資料 Repository
    │   │   ├── service/
    │   │   │   ├── DBExampleService.java            # 資料庫範例服務介面
    │   │   │   ├── DBExampleServiceImpl.java        # 資料庫範例服務實作
    │   │   │   ├── ExampleService.java              # 通用範例服務介面
    │   │   │   └── ExampleServiceImpl.java          # 通用範例服務實作
    │   │   └── webservice/
    │   │       ├── ExampleWebService.java           # WebService 介面
    │   │       └── impl/ExampleWebServiceImpl.java  # WebService 實作
    │   └── resources/
    │       ├── application.yml                      # 應用程式主設定
    │       ├── data-source.properties               # 資料來源設定
    │       ├── quartz-datasource.properties         # Quartz 資料來源設定
    │       ├── quartz-jobs.properties               # Quartz 排程設定
    │       ├── resources.properties                 # Logback 外部化參數
    │       ├── init/
    │       │   ├── schema.sql                       # 建表 SQL（user_main / user_detail，application.yml 實際引用）
    │       │   ├── data.sql                         # 初始資料（application.yml 實際引用）
    │       │   └── h2/
    │       │       ├── schema.sql                   # H2 建表 SQL（TBL_EMPLOYEES，備用）
    │       │       └── data.sql                     # H2 初始資料（備用）
    │       ├── jasperreport/                        # JasperReport 報表模板 (*.jrxml)
    │       └── logback-spring.xml                   # Logback 日誌設定
    ├── postman/
    │   └── Example.postman_collection.json          # Postman 測試集合
    └── test/java/com/example/
        ├── controller/                              # Controller 整合測試
        ├── service/                                 # Service 單元測試
        └── util/                                    # 工具類別測試 (crypto/excel/jasperreport)
```

## 主要功能

整合所有 Starter 的完整範例、H2 記憶體資料庫快速啟動、各功能模組測試案例、Postman 測試集合

## 對應文件

本模組的完整使用說明與開發指南請參閱 doc-site 技術文件：

- [整合範例總覽](../../doc-site/docs/integration/index.md)
- [Web 與登入認證情境](../../doc-site/docs/integration/scenario-web-auth.md)
- [資料庫情境](../../doc-site/docs/integration/scenario-db.md)
- [排程情境](../../doc-site/docs/integration/scenario-job.md)
- [WebService 情境](../../doc-site/docs/integration/scenario-webservice.md)
- [完整整合情境](../../doc-site/docs/integration/scenario-full.md)
