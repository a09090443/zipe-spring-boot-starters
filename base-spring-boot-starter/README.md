# base-spring-boot-starter

所有 Starter 的基礎依賴模組，提供加解密、文件處理、HTTP 請求、郵件發送等通用工具類別。

## 主要功能

- 郵件發送（Spring Boot Mail + Velocity 模板）
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
    <version>3.5.7.0</version>
</dependency>
```

## 基本設定

```properties
# 郵件設定
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your@email.com
spring.mail.password=yourpassword

# Velocity 模板路徑
velocity.template.path=classpath:/templates/
```
