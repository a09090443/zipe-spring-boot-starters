# db-spring-boot-starter

動態多資料來源管理模組，支援執行時切換多個資料庫連線，並提供基礎 JDBC 封裝。

## 主要功能

- 多資料來源動態切換（`@DS` 註解）
- 支援 MySQL、MS SQL Server、MariaDB、AS/400
- P6Spy SQL 監控日誌
- 基礎 JDBC 封裝與 SQL 條件建構器
- 資料庫密碼加密儲存

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>db-spring-boot-starter</artifactId>
    <version>4.0.0.0</version>
</dependency>
```

## 基本設定

```properties
# 指定主要資料來源
dynamic.datasource.primary=master

# 主資料來源設定
dynamic.datasource.datasource-map.master.url=jdbc:mysql://localhost:3306/mydb
dynamic.datasource.datasource-map.master.username=root
dynamic.datasource.datasource-map.master.pa55word=password

# 次要資料來源設定
dynamic.datasource.datasource-map.secondary.url=jdbc:mysql://localhost:3306/mydb2
dynamic.datasource.datasource-map.secondary.username=root
dynamic.datasource.datasource-map.secondary.pa55word=password
```

## 使用方式

```java
// 在類別或方法上指定使用的資料來源
@DS("secondary")
public class UserRepository {
    // 使用 secondary 資料來源
}
```
