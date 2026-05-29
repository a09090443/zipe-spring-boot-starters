---
id: quickstart
title: 快速開始
sidebar_position: 2
---

# 快速開始

本頁示範如何發布一個 SOAP WebService 服務端，並以客戶端工具呼叫它。

## 前置需求

- JDK 17 以上、Spring Boot 3.5.x。
- 已將 `web-service-spring-boot-starter` 安裝至本地 Maven Repository。
- 對 SOAP / WSDL 有基本認識。

:::note
Spring Boot 3.x 已改用 `jakarta.jws.*` 命名空間，請確認服務介面使用的是 `jakarta` 而非舊版 `javax` 套件。
:::

## Step 1：安裝模組

```bash
cd web-service-spring-boot-starter
./mvnw clean install -DskipTests
```

## Step 2：加入依賴

```xml
<dependency>
    <groupId>com.zipe</groupId>
    <artifactId>web-service-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Step 3：設定 application.yml

設定 CXF 的服務路徑前綴：

```yaml
zipe:
  web-service:
    path: /services
    address: /user
    enable-cdata: true
```

## Step 4：程式碼範例

定義服務介面與實作：

```java
import jakarta.jws.WebService;
import jakarta.jws.WebMethod;

@WebService
public interface UserService {
    @WebMethod
    String getUserName(String userId);
}
```

```java
import com.zipe.config.Service;
import jakarta.jws.WebService;

@Service
@WebService(endpointInterface = "com.example.UserService")
public class UserServiceImpl implements UserService {

    @Override
    public String getUserName(String userId) {
        return "User-" + userId;
    }
}
```

以客戶端工具呼叫遠端服務：

```java
import com.zipe.util.WebServiceClientUtil;

public class UserServiceClient {

    public String call() {
        UserService service = WebServiceClientUtil.create(
            UserService.class,
            "http://localhost:8080/services/user?wsdl");
        return service.getUserName("1001");
    }
}
```

## Step 5：執行驗證

啟動應用程式並檢視 WSDL：

```bash
./mvnw spring-boot:run
curl http://localhost:8080/services/user?wsdl
```

若能取得 WSDL 文件，表示服務端已成功發布；接著執行客戶端程式應回傳 `User-1001`。

:::tip WSDL 路徑組成
WSDL 端點路徑由 `zipe.web-service.path` 與 `address` 串接而成，再加上 `?wsdl` 查詢字串。以範例設定為 `/services` + `/user` = `/services/user?wsdl`。
:::

:::warning 命名空間相容
SOAP 客戶端與服務端的命名空間（namespace）需一致。若由外部 WSDL 產生客戶端 stub，請確認其與服務端介面的 `targetNamespace` 相符。
:::
