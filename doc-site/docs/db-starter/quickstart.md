---
id: quickstart
title: 快速開始
sidebar_position: 2
---

# 快速開始

本頁示範如何設定兩組資料來源，並以 `@DS` Annotation 在執行期間動態切換。

## 前置需求

- JDK 17 以上、Spring Boot 3.5.x。
- 已準備至少一組（建議兩組）可連線的資料庫。
- 已將 `db-spring-boot-starter` 安裝至本地 Maven Repository。

:::note
動態切換功能依賴 Spring AOP，請確認專案未停用 AOP 自動配置。
:::

## Step 1：安裝模組

```bash
cd db-spring-boot-starter
./mvnw clean install -DskipTests
```

## Step 2：加入依賴

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>db-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Step 3：設定 application.yml

設定主要與次要兩組資料來源：

```yaml
zipe:
  datasource:
    primary: master
    sources:
      master:
        url: jdbc:mysql://localhost:3306/main_db
        username: root
        password: ${DB_MASTER_PASSWORD}
        driver-class-name: com.mysql.cj.jdbc.Driver
      report:
        url: jdbc:mysql://localhost:3306/report_db
        username: reader
        password: ${DB_REPORT_PASSWORD}
        driver-class-name: com.mysql.cj.jdbc.Driver
```

:::warning 主資料來源
`primary` 指定的資料來源為預設連線，未標註 `@DS` 的方法都會使用此來源。請務必確認 `primary` 的值對應到 `sources` 中實際存在的 key。
:::

## Step 4：程式碼範例

在 Service 方法上以 `@DS` 指定資料來源：

```java
import com.zipe.base.annotation.DS;
import com.zipe.jdbc.BaseJDBC;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final BaseJDBC baseJDBC;

    public ReportService(BaseJDBC baseJDBC) {
        this.baseJDBC = baseJDBC;
    }

    // 使用預設（master）資料來源
    public int countUsers() {
        return baseJDBC.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    }

    // 切換至 report 資料來源
    @DS("report")
    public int countReports() {
        return baseJDBC.queryForObject("SELECT COUNT(*) FROM reports", Integer.class);
    }
}
```

## Step 5：執行驗證

啟動應用程式並分別呼叫兩個方法，觀察 P6Spy 日誌確認 SQL 落在正確的資料庫：

```bash
./mvnw spring-boot:run
```

若 `countReports()` 的 SQL 出現在連線至 `report_db` 的日誌中，即表示切換成功。

:::tip 切換生效範圍
`@DS` 透過 AOP 攔截，僅對「外部呼叫」的 Spring Bean 方法生效。若在同一類別內以 `this.method()` 直接呼叫帶 `@DS` 的方法，切面不會被觸發。
:::
