---
description: logon-spring-boot-starter 的目錄結構與功能說明
paths:
  - logon-spring-boot-starter/**
---

# logon-spring-boot-starter

登入認證功能的 Starter，整合 Spring Security，支援一般表單登入、LDAP 驗證、以及自訂驗證流程。

## 目錄結構

```
logon-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── autoconfiguration/
    │   └── SecurityConfiguration.java               # Spring Security 主設定
    ├── base/service/
    │   └── SecurityBaseService.java                 # 安全性基礎服務
    ├── config/
    │   ├── LdapPropertyConfig.java                  # LDAP 屬性設定
    │   ├── SecurityInitializer.java                 # Security 初始化
    │   └── SecurityPropertyConfig.java              # Security 屬性設定
    ├── enums/
    │   ├── UserEnum.java                            # 使用者狀態 Enum
    │   └── VerificationTypeEnum.java                # 驗證類型 Enum (DB/LDAP/Custom)
    ├── exception/
    │   ├── LdapException.java                       # LDAP 例外
    │   └── UserNotActivatedException.java           # 使用者未啟用例外
    ├── handler/
    │   ├── LoginFailureHandler.java                 # 登入失敗處理器
    │   ├── LoginSuccessHandler.java                 # 登入成功處理器
    │   └── LogoutSuccessHandler.java                # 登出成功處理器
    ├── model/
    │   └── LdapUser.java                            # LDAP 使用者模型
    ├── service/
    │   ├── BasicUserServiceImpl.java                # 基本使用者服務實作
    │   ├── CommonLoginProcess.java                  # 共用登入處理流程
    │   ├── CustomLogonLogRecord.java                # 自訂登入紀錄介面
    │   └── LdapUserDetailsService.java              # LDAP UserDetails 服務
    ├── util/
    │   └── UserInfoUtil.java                        # 使用者資訊工具
    └── vo/
        └── SysUserVO.java                           # 系統使用者 View Object
```

## 主要功能

Spring Security 表單登入、LDAP 目錄服務驗證、自訂驗證類型切換 (`VerificationTypeEnum`)、登入成功/失敗處理器、自訂登入日誌記錄介面

## 對應文件

本模組的完整使用說明與開發指南請參閱 doc-site 技術文件：

- [模組簡介](../../doc-site/docs/logon-starter/index.md)
- [快速開始](../../doc-site/docs/logon-starter/quickstart.md)
- [配置參考](../../doc-site/docs/logon-starter/configuration.md)
- [使用範例](../../doc-site/docs/logon-starter/examples.md)
- [架構與開發指南](../../doc-site/docs/logon-starter/architecture.md)
