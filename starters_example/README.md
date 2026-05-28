# starters_example

整合所有 Starter 的完整範例專案，使用 H2 記憶體資料庫，可直接啟動執行，適合快速驗證各模組功能。

## 涵蓋功能

- `base-spring-boot-starter`：加解密、Excel、JasperReport、HTTP 工具
- `db-spring-boot-starter`：多資料來源動態切換（H2 記憶體資料庫）
- `job-spring-boot-starter`：Quartz 排程（Annotation、DB、XML 設定三種方式）
- `logon-spring-boot-starter`：Spring Security 登入認證
- `web-spring-boot-starter`：Thymeleaf 視圖與統一回應格式
- `web-service-spring-boot-starter`：CXF SOAP WebService

## 快速啟動

```bash
cd starters_example
./mvnw spring-boot:run
```

啟動後可透過 `src/postman/Example.postman_collection.json` 匯入 Postman 進行 API 測試。

## 說明

- 資料庫使用 H2 記憶體資料庫，啟動時自動建表並載入初始資料（`init/h2/`）
- 排程設定位於 `quartz-jobs.properties`
- 資料來源設定位於 `data-source.properties`
