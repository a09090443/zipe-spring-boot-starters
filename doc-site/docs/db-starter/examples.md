---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `db-spring-boot-starter` 的資料來源切換、`BaseJDBC` 封裝與動態 SQL 條件建構的實際用法。所有範例的方法簽章與設定鍵均與原始碼一致。

:::info 核心設計
- **SQL 外部化**：SQL 不寫在 Java 程式中，而是放在 classpath（或檔案系統）的 `/sql/*.sql` 檔，透過 `ResourceEnum.SQL.getResource("檔名")` 引用，讀取後會被快取於 `SQL_CACHE`。
- **`BaseJDBC` 為抽象類別**：DAO 需 **繼承** `BaseJDBC`，即可使用其 `queryForList` / `queryForMap` / `update` 等方法。
- **具名參數**：底層使用 `NamedParameterJdbcTemplate`，SQL 以 `:paramName` 形式綁定 `Map` 參數。
:::

## 基礎使用範例

### 步驟一：撰寫外部化 SQL 檔

在 `src/main/resources/sql/user/` 下建立 `findActiveUsers.sql`（對應 `ResourceEnum` 的 `/sql` 目錄與 `.sql` 副檔名）：

```sql
SELECT id, name, email
FROM users
WHERE status = :status
```

### 步驟二：DAO 繼承 BaseJDBC

```java
import com.zipe.enums.ResourceEnum;
import com.zipe.jdbc.BaseJDBC;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserDao extends BaseJDBC {

    public List<Map<String, Object>> findByStatus(String status) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        // 對應 /sql/user/findActiveUsers.sql
        return queryForList(ResourceEnum.SQL.getResource("user/findActiveUsers"), params);
    }
}
```

### 對應到實體類別

`queryForList` 提供 `Class<T>` 多載，透過 `BeanPropertyRowMapper` 將結果列對應為實體物件：

```java
public List<User> findUsers(String status) {
    Map<String, Object> params = new HashMap<>();
    params.put("status", status);
    return queryForList(ResourceEnum.SQL.getResource("user/findActiveUsers"),
                        params, User.class);
}
```

### 更新操作

```java
// /sql/user/updateEmail.sql : UPDATE users SET email = :email WHERE id = :id
public int updateEmail(Long id, String email) {
    Map<String, Object> params = new HashMap<>();
    params.put("id", id);
    params.put("email", email);
    return update(ResourceEnum.SQL.getResource("user/updateEmail"), params);
}
```

## 進階使用範例

### 以 Conditions 建構動態條件

`Conditions` 是 fluent 條件建構器，會將條件片段 **附加** 到外部 SQL 之後（由 `getSqlText` 內部呼叫 `conditions.done(sqlText)` 完成）。可用方法包含 `equal` / `unEqual` / `like` / `in` / `notIn` / `gt` / `lt` / `gtEqual` / `ltEqual` / `isNull` / `notNull` / `and` / `or` / `orderBy`：

```java
import com.zipe.jdbc.criteria.Conditions;
import com.zipe.jdbc.criteria.SQL;

public List<Map<String, Object>> search(String keyword) {
    Conditions conditions = new Conditions()
            .like("name", keyword)
            .and()
            .equal("status", "ACTIVE")
            .orderBy("created_at", SQL.DESC); // SQL 為運算子列舉（DESC/ASC/EQUAL...）

    // 基底 SQL（/sql/user/baseQuery.sql）後會接上上述條件片段
    return queryForList(ResourceEnum.SQL.getResource("user/baseQuery"), conditions);
}
```

:::caution Conditions 與 SQL 快取
`SQL_CACHE` 以 `resource + conditions.hashCode() + paging.hashCode()` 為鍵。`Conditions` 未覆寫 `hashCode()`，因此**帶 `Conditions` 的查詢幾乎不會命中快取**；只有純靜態 SQL（不帶 `Conditions`）能有效快取。詳見 [架構與開發指南](./architecture.md)。
:::

### 伺服器端分頁

`Paging` 為抽象類別，分頁模板讀自 classpath 的 `/sql/PAGING.sql`。需提供具體的 `Paging` 子類別並設定 `page` / `pagesize` / `orderBy`：

```java
import com.zipe.jdbc.criteria.Paging;

Paging paging = new Paging() {}; // 自訂子類別（依資料庫方言實作 getPagingSQL 可覆寫）
paging.setPage(1);
paging.setPagesize(20);

List<User> users = queryForList(
        ResourceEnum.SQL.getResource("user/baseQuery"),
        params, conditions, paging, User.class);
```

### 以 @DS 切換多資料來源

`@DS` 預設值為 `common`，其 `value` 必須對應 `dynamic.data-source-map` 中的 key。切面（`DynamicDataSourceAspect`）在方法執行前後操作 `DataSourceHolder`（`ThreadLocal`）完成切換與還原：

```java
import com.zipe.base.annotation.DS;
import org.springframework.stereotype.Service;

@Service
public class CrossDbService {

    private final UserDao userDao;

    public CrossDbService(UserDao userDao) {
        this.userDao = userDao;
    }

    @DS("master")
    public List<User> activeUsers() {
        return userDao.findUsers("ACTIVE");
    }

    @DS("report")
    public List<Map<String, Object>> monthlyReport() {
        return userDao.findByStatus("REPORT");
    }
}
```

對應的 `application.yml` 設定（前綴為 `dynamic`）：

```yaml
dynamic:
  primary: master            # 預設（主要）資料來源名稱
  is-encrypt: false          # 連線密碼是否加密
  data-source-map:
    master:
      name: master
      url: jdbc:mysql://localhost:3306/app
      username: root
      pa55word: secret        # 注意：屬性鍵為 pa55word（原始碼命名）
      driver-class-name: com.mysql.cj.jdbc.Driver
    report:
      name: report
      url: jdbc:mysql://report-host:3306/report
      username: reader
      pa55word: secret
      driver-class-name: com.mysql.cj.jdbc.Driver
```

## 常見情境

### 情境一：讀寫分離

將寫入導向主庫、查詢導向唯讀副本，於 Service 層標註 `@DS`：

```java
@Service
public class OrderService {

    @DS("master")
    public void createOrder(Order order) {
        // 寫入主庫
    }

    @DS("replica")
    public List<Order> queryOrders(Long userId) {
        // 從唯讀副本查詢
        return ...;
    }
}
```

### 情境二：以程式動態指定資料來源

除了註解，亦可用 `DynamicDS`（`DS` 的程式化實作）在執行期決定資料來源名稱，搭配 `DataSourceHolder` 手動切換；手動切換後務必於 `finally` 還原，避免 `ThreadLocal` 殘留導致執行緒重用時連到錯誤資料庫。

### 情境三：開啟 P6Spy 排查慢查詢

模組整合 `P6SpyLogger`，可透過 P6Spy 的 `spy.properties` 與驅動包裝攔截並格式化實際執行的 SQL，便於定位效能瓶頸。詳細啟用方式見 [架構與開發指南](./architecture.md) 與 [配置參考](./configuration.md)。

## 常見問題

- **`BaseJDBC` 無法注入**：`BaseJDBC` 是抽象類別，請讓 DAO **繼承**它（而非以建構子注入 `BaseJDBC`）。
- **`SQL file not found`**：確認 `.sql` 檔位於 classpath 的 `/sql` 目錄下（或對應的檔案系統路徑），且 `getResource("名稱")` 傳入的名稱不含副檔名與前置 `/sql`。
- **`@DS` 沒有生效**：確認是「跨 Bean 的外部呼叫」，而非類別內部 `this` 直接呼叫；同時確認目標方法為 `public`。
- **找不到指定資料來源**：`@DS("xxx")` 名稱必須對應 `dynamic.data-source-map` 中的 key，拼字需完全一致。
- **執行緒重用後連到錯誤資料庫**：通常是手動操作 `DataSourceHolder` 後未清除所致，請改以 `@DS` 交由切面管理。

:::tip 最佳實踐
在 Service 層（而非 DAO 層）標註 `@DS`，確保一個交易內所有資料庫操作落在同一資料來源；避免在帶 `@Transactional` 的方法內切換資料來源，以免交易邊界與連線不一致。更深入的切面與 `ThreadLocal` 機制請參閱 [架構與開發指南](./architecture.md)。
:::
