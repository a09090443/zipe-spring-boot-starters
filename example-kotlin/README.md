# example-kotlin

以 Kotlin 語言撰寫的整合範例，功能與 `starters_example` 相同，展示各 Starter 在 Kotlin 專案中的使用方式。

## 涵蓋功能

- 多資料來源動態切換（H2 記憶體資料庫）
- Quartz 排程任務
- CXF SOAP WebService
- JasperReport 報表
- 加解密工具

## 快速啟動

```bash
cd example-kotlin
./gradlew bootRun
```

## 說明

- 使用 Gradle 建構（`build.gradle.kts`）
- 資料庫使用 H2 記憶體資料庫，啟動時自動建表
- 資料來源設定位於 `data-source.properties`
- 排程設定位於 `quartz-jobs.properties`
