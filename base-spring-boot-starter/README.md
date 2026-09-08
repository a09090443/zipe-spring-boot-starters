# base-spring-boot-starter

所有 Starter 的基礎依賴模組，提供加解密、文件處理、HTTP 請求、郵件發送等通用工具類別。

## 主要功能

- 郵件發送（Spring Boot Mail + Velocity 模板），支援設定多組 SMTP 伺服器並於單組失敗時自動容錯切換
- 加解密工具（AES、3DES、MD5、Base64、Hex）
- 文件處理（Excel via Apache POI、JasperReport 報表）
- HTTP 客戶端（OkHttp）
- 字串、日期、Bean 轉換、正規表達式等通用工具
- 類別動態載入（ClassLoader）

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>base-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

## 基本設定

```properties
# 郵件設定（單組 SMTP，屬性前綴為 mail，非 spring.mail）
mail.host=smtp.example.com
mail.port=587
mail.username=your@email.com
mail.pa55word=yourpassword

# 郵件設定（多組 SMTP，依序容錯切換；未設定時以上方單組欄位自動合成一組，向後相容）
mail.servers[0].name=primary
mail.servers[0].host=smtp1.example.com
mail.servers[0].port=587
mail.servers[0].username=your@email.com
mail.servers[0].pa55word=yourpassword
mail.servers[1].name=backup
mail.servers[1].host=smtp2.example.com
mail.servers[1].port=587
mail.servers[1].username=your-backup@email.com
mail.servers[1].pa55word=yourbackuppassword

# Velocity 模板路徑
velocity.template.path=classpath:/templates/
```

> 完整設定屬性（含逾時、容錯切換上限）與重複寄送風險說明，請參閱
> [doc-site/docs/base-starter/configuration.md](../doc-site/docs/base-starter/configuration.md)。
