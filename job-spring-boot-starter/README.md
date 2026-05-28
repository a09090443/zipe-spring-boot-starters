# job-spring-boot-starter

基於 Quartz 的排程任務管理模組，支援 JDBC 與記憶體兩種儲存模式，並提供 REST API 進行排程管理。

## 主要功能

- Quartz 排程管理（新增、修改、刪除、暫停、恢復）
- 支援 JDBC JobStore（資料庫持久化）與記憶體 JobStore
- 透過 `quartz-jobs.properties` 設定檔自動建立排程
- 排程管理 REST API
- 可繼承 `BaseJob` 實作自訂業務邏輯

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>job-spring-boot-starter</artifactId>
    <version>3.5.7.0</version>
</dependency>
```

## 基本設定

```properties
# quartz-jobs.properties
quartz.job-map.job1.name=MyJob
quartz.job-map.job1.clazz=com.example.job.MyJob
quartz.job-map.job1.cron-expression=0 0/5 * * * ?
```

## 使用方式

```java
public class MyJob extends BaseJob {
    @Override
    protected void executeInternal(JobExecutionContext context) {
        // 排程業務邏輯
    }
}
```
