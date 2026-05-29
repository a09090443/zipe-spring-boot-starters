---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `db-spring-boot-starter` 的資料來源切換、JDBC 封裝與 SQL 條件建構的實際用法。

## 基礎使用範例

### 使用 BaseJDBC 進行查詢

```java
import com.zipe.jdbc.BaseJDBC;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public class UserDao {

    private final BaseJDBC baseJDBC;

    public UserDao(BaseJDBC baseJDBC) {
        this.baseJDBC = baseJDBC;
    }

    public List<Map<String, Object>> findAll() {
        return baseJDBC.queryForList("SELECT id, name, email FROM users");
    }

    public int insert(String name, String email) {
        return baseJDBC.update(
            "INSERT INTO users(name, email) VALUES (?, ?)", name, email);
    }
}
```

## 進階使用範例

### 以 @DS 切換多資料來源

```java
import com.zipe.base.annotation.DS;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class CrossDbService {

    private final BaseJDBC baseJDBC;

    public CrossDbService(BaseJDBC baseJDBC) {
        this.baseJDBC = baseJDBC;
    }

    @DS("master")
    public List<Map<String, Object>> activeUsers() {
        return baseJDBC.queryForList("SELECT * FROM users WHERE status = 'ACTIVE'");
    }

    @DS("report")
    public List<Map<String, Object>> monthlyReport() {
        return baseJDBC.queryForList("SELECT * FROM monthly_summary");
    }
}
```

### 使用 SQL 建構器組裝動態查詢

```java
import com.zipe.jdbc.criteria.SQL;
import com.zipe.jdbc.criteria.Conditions;
import com.zipe.jdbc.criteria.Paging;

public class QueryBuilderExample {

    public String buildQuery(String keyword, int page, int size) {
        Conditions conditions = new Conditions();
        if (keyword != null && !keyword.isEmpty()) {
            conditions.like("name", keyword);
        }
        conditions.eq("status", "ACTIVE");

        Paging paging = new Paging(page, size);

        return SQL.select("id", "name", "email")
                  .from("users")
                  .where(conditions)
                  .page(paging)
                  .build();
    }
}
```

## 常見情境

### 情境一：讀寫分離

將寫入操作導向主庫、查詢導向唯讀副本：

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

### 情境二：跨資料庫彙整報表

在同一個服務方法中先後查詢不同資料庫，並合併結果：

```java
@Service
public class DashboardService {

    private final CrossDbService crossDbService;

    public DashboardService(CrossDbService crossDbService) {
        this.crossDbService = crossDbService;
    }

    public Dashboard build() {
        var users = crossDbService.activeUsers();   // master
        var report = crossDbService.monthlyReport(); // report
        return new Dashboard(users, report);
    }
}
```

### 情境三：開啟 P6Spy 排查慢查詢

於 `application.yml` 設定 `zipe.datasource.p6spy.enabled: true` 並調整慢查詢門檻後，超過門檻的 SQL 會被標示於日誌中，便於定位效能瓶頸。

## 常見問題

- **`@DS` 沒有生效**：確認是「跨 Bean 的外部呼叫」，而非類別內部 `this` 直接呼叫；同時確認目標方法為 `public`。
- **找不到指定資料來源**：`@DS("xxx")` 的名稱必須對應 `zipe.datasource.sources` 中的 key，拼字需完全一致。
- **執行緒重用後連到錯誤資料庫**：通常是手動操作 `DataSourceHolder` 後未清除所致，請改以 `@DS` 交由切面管理。

:::tip 最佳實踐
盡量在 Service 層（而非 DAO 層）標註 `@DS`，以確保一個交易內所有資料庫操作落在同一資料來源；同時避免在帶 `@Transactional` 的方法內切換資料來源，以免交易邊界與連線不一致。
:::
