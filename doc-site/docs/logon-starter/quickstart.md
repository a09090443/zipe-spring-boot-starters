---
id: quickstart
title: 快速開始
sidebar_position: 2
---

# 快速開始

本頁示範如何啟用資料庫表單登入，並取得當前登入使用者。

## 前置需求

- JDK 17 以上、Spring Boot 3.5.x。
- 已將 `logon-spring-boot-starter` 安裝至本地 Maven Repository。
- 已準備使用者資料表（DB 模式）或 LDAP 連線（LDAP 模式）。

:::note
本模組會接管 Spring Security 的主要設定，請避免在業務專案中重複定義 `SecurityFilterChain`，以免設定衝突。
:::

## Step 1：安裝模組

```bash
cd logon-spring-boot-starter
./mvnw clean install -DskipTests
```

## Step 2：加入依賴

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>logon-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Step 3：設定 application.yml

以資料庫驗證為例：

```yaml
zipe:
  security:
    verification-type: DB
    login-page: /login
    login-process-url: /doLogin
    default-success-url: /home
    logout-url: /logout
    permit-all:
      - /login
      - /css/**
      - /js/**
```

## Step 4：程式碼範例

DB 模式下，您需要提供使用者資料來源。內建的 `BasicUserServiceImpl` 會依帳號查詢使用者；若需自訂查詢，可實作自己的 `UserDetailsService`。取得當前登入使用者：

```java
import com.zipe.util.UserInfoUtil;
import com.zipe.vo.SysUserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    @GetMapping("/me")
    public SysUserVO currentUser() {
        return UserInfoUtil.getCurrentUser();
    }
}
```

## Step 5：執行驗證

啟動應用程式，於瀏覽器開啟登入頁：

```bash
./mvnw spring-boot:run
```

開啟 `http://localhost:8080/login`，輸入正確帳密後應被導向 `default-success-url`（範例為 `/home`）。登入後呼叫 `/me` 應回傳當前使用者資訊。

:::tip 放行靜態資源
`permit-all` 清單務必包含登入頁本身與其相依的 CSS / JS 等靜態資源，否則使用者在尚未登入時無法正確載入登入頁面樣式。
:::

:::warning CSRF 設定
Spring Security 預設啟用 CSRF 保護。若以表單登入，請確保登入表單包含 CSRF token；若為純前後端分離的 API，請依需求調整 CSRF 設定。
:::
