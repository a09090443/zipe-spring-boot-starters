---
description: base-spring-boot-starter 的目錄結構與功能說明
paths:
  - base-spring-boot-starter/**
---

# base-spring-boot-starter

基本功能的 Starter，提供各種通用工具類別。

## 目錄結構

```
base-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── autoconfiguration/
    │   └── BaseAutoConfiguration.java          # 自動配置入口
    ├── config/
    │   ├── MailPropertyConfig.java              # 郵件設定
    │   ├── ThreadPoolTaskExecutorConfig.java    # 執行緒池設定
    │   └── VelocityPropertyConfig.java          # Velocity 樣板設定
    ├── model/
    │   └── Mail.java                            # 郵件資料模型
    ├── service/
    │   ├── MailService.java                     # 郵件服務介面
    │   └── impl/MailServiceImpl.java            # 郵件服務實作
    └── util/
        ├── ApplicationContextHelper.java        # Spring Context 取得工具
        ├── LdapUtil.java                        # LDAP 工具
        ├── MapUtils.java                        # Map 工具
        ├── VelocityUtil.java                    # Velocity 樣板工具
        ├── YamlPropertySourceFactory.java       # YAML 屬性來源工廠
        ├── bean/
        │   ├── BeanUtil.java                    # Bean 轉換工具
        │   ├── DateSerializer.java              # 日期序列化
        │   ├── EnumAdapterFactory.java          # Enum 轉換工廠
        │   └── LowerCaseKeyDeserializer.java    # 小寫鍵反序列化
        ├── classloader/
        │   ├── CustomClassLoader.java           # 自訂類別載入器
        │   ├── FileClassLoader.java             # 檔案類別載入器
        │   └── JarClassLoader.java              # JAR 類別載入器
        ├── crypto/
        │   ├── AesUtil.java                     # AES 加解密
        │   ├── Base64Util.java                  # Base64 編解碼
        │   ├── Crypto.java                      # 加解密介面
        │   ├── CryptoUtil.java                  # 加解密工具
        │   ├── DESedeUtil.java                  # 3DES 加解密
        │   ├── HexUtil.java                     # 16進位工具
        │   └── Md5Util.java                     # MD5 雜湊
        ├── doc/
        │   ├── ExcelUtil.java                   # Excel 操作工具
        │   ├── JasperReportUtil.java            # JasperReport 報表工具
        │   └── (ExcelCell/ExcelLog/ExcelSheet 等 Annotation)
        ├── file/
        │   └── FileUtil.java                    # 檔案操作工具
        ├── http/
        │   └── OkHttpUtil.java                  # HTTP 請求工具 (OkHttp)
        ├── print/
        │   └── PrintUtils.java                  # 列印工具
        ├── string/
        │   ├── CommonStringUtil.java            # 字串工具
        │   ├── RandomUtil.java                  # 亂數產生工具
        │   └── StringConstant.java              # 字串常數
        ├── time/
        │   └── DateTimeUtils.java               # 日期時間工具
        └── validation/
            ├── RegexUtils.java                  # 正規表達式工具
            └── Validation.java                  # 資料驗證工具
```

## 主要功能

郵件發送、加解密工具 (AES/3DES/MD5/Base64)、Excel/JasperReport 文件處理、HTTP 請求、字串/日期工具、Bean 轉換、類別動態載入
