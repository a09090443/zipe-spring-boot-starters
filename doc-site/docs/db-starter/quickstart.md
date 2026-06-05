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

在業務專案的 `pom.xml` 加入：

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>db-spring-boot-starter</artifactId>
    <version>3.5.7.0</version>
</dependency>
```

## Step 3：設定 data-source.properties

在業務專案的 `src/main/resources/` 建立 `data-source.properties`，宣告兩組資料來源：

```properties
# 預設使用的資料來源 key（必填）
dynamic.primary=master

# JPA Entity 掃描套件路徑（必填）
dynamic.entity-scan=com.example

# 密碼是否經 Base64+AES 加密（選填，預設 false）
dynamic.is-encrypt=false

# 主資料來源（key: master）
dynamic.data-source-map[master].url=jdbc:p6spy:mysql://localhost:3306/main_db?characterEncoding=UTF-8&serverTimezone=Asia/Taipei
dynamic.data-source-map[master].username=root
dynamic.data-source-map[master].pa55word=your_password
dynamic.data-source-map[master].driverClassName=com.p6spy.engine.spy.P6SpyDriver

# 報表資料來源（key: report）
dynamic.data-source-map[report].url=jdbc:p6spy:mysql://localhost:3306/report_db?characterEncoding=UTF-8&serverTimezone=Asia/Taipei
dynamic.data-source-map[report].username=reader
dynamic.data-source-map[report].pa55word=reader_password
dynamic.data-source-map[report].driverClassName=com.p6spy.engine.spy.P6SpyDriver
```

:::warning 重要注意事項

- 屬性前綴為 `dynamic`（非 `spring.datasource` 或 `zipe.datasource`）。
- 密碼欄位名稱為 `pa55word`（**非 `password`**），這是刻意的拼寫設計，寫成 `password` 將導致屬性綁定失敗、密碼為空。
- `dynamic.primary` 的值必須對應 `dynamic.data-source-map` 中實際存在的 key，否則啟動時會因找不到預設 DataSource 而失敗。
- 使用 P6Spy 監控時，JDBC URL 需加 `p6spy:` 前綴，並將驅動設為 `com.p6spy.engine.spy.P6SpyDriver`；若不需要 P6Spy 監控，可直接使用原生驅動與 URL。

:::

## Step 4：建立 SQL 檔案

`BaseJDBC` 的查詢方法不接受直接傳入 SQL 字串，所有 SQL 必須存放於外部 `.sql` 檔案。

在 `src/main/resources/sql/` 建立 SQL 檔案，例如 `USER_LIST.sql`：

```sql
SELECT
    u.id,
    u.username,
    u.email
FROM users u
WHERE 1 = 1
${CONDITIONS}
```

`${CONDITIONS}` 是 `Conditions` 建構器最終替換的佔位符。若該 SQL 不需要動態條件，可省略此佔位符。

## Step 5：建立 DAO 繼承 BaseJDBC

`BaseJDBC` 是抽象類別，**不可直接注入**，需建立子類別繼承：

```java
import com.zipe.enums.ResourceEnum;
import com.zipe.jdbc.BaseJDBC;
import com.zipe.jdbc.criteria.Conditions;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserJdbc extends BaseJDBC {

    /**
     * 查詢所有使用者（對應 classpath:/sql/USER_LIST.sql）
     */
    public List<Map<String, Object>> findAll() {
        return queryForList(ResourceEnum.SQL.getResource("USER_LIST"));
    }

    /**
     * 依關鍵字模糊搜尋使用者
     */
    public List<Map<String, Object>> searchByKeyword(String keyword) {
        Conditions conditions = new Conditions();
        conditions.like("u.username", keyword);

        return queryForList(
            ResourceEnum.SQL.getResource("USER_LIST"),
            new HashMap<>(),
            conditions
        );
    }
}
```

## Step 6：在 Service 以 `@DS` 切換資料來源

```java
import com.zipe.base.annotation.DS;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final UserJdbc userJdbc;

    public ReportService(UserJdbc userJdbc) {
        this.userJdbc = userJdbc;
    }

    // 使用預設（master）資料來源，不需標注 @DS
    public List<Map<String, Object>> getMasterUsers() {
        return userJdbc.findAll();
    }

    // 切換至 report 資料來源
    @DS("report")
    public List<Map<String, Object>> getReportUsers() {
        return userJdbc.findAll();
    }
}
```

## Step 7：執行驗證

啟動應用程式並分別呼叫兩個方法，觀察 P6Spy 日誌確認 SQL 落在正確的資料庫：

```bash
./mvnw spring-boot:run
```

若 `getReportUsers()` 的 SQL 出現在連線至 `report_db` 的日誌中，即表示切換成功。

:::tip 切換生效範圍

`@DS` 透過 AOP 攔截，僅對「跨 Bean 的外部呼叫」的 `public` 方法生效。

- 正確：`serviceA.method()` 呼叫 `serviceB.method()`，其中 `serviceB.method()` 標注 `@DS`
- 無效：在同一類別內以 `this.method()` 直接呼叫帶 `@DS` 的方法

若類別層級和方法層級都有 `@DS`，**類別層級優先**，方法層級的 `@DS` 會被忽略。

:::
