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
    <groupId>io.github.a09090443</groupId>
    <artifactId>web-service-spring-boot-starter</artifactId>
    <version>3.5.11.0</version>
</dependency>
```

## Step 3：設定 application.yml

設定 CXF 的服務路徑前綴，以及要發布的端點清單。

```yaml
web:
  service:
    uri-mapping: /webservice/*      # CXF Servlet 的 URL 前綴，預設值為 /webservice/*
    map:
      user:                         # 邏輯名稱，任意命名
        beanName: userServiceImpl   # 實作 Bean 的 Spring Bean 名稱
        uri-mapping: /user          # 端點的相對路徑
```

啟動後 WSDL 可從 `http://localhost:8080/webservice/user?wsdl` 取得。

## Step 4：定義服務介面

```java
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

@WebService(targetNamespace = "http://service.example.com")
public interface UserService {

    @WebMethod(action = "getUserName")
    @WebResult(name = "userName")
    String getUserName(@WebParam(name = "userId") String userId);
}
```

## Step 5：實作服務介面

```java
import jakarta.jws.WebService;
import org.springframework.stereotype.Component;

@WebService(
    serviceName = "UserService",
    targetNamespace = "http://service.example.com",
    endpointInterface = "com.example.service.UserService"
)
@Component("userServiceImpl")       // beanName 需與 yml 的 beanName 設定一致
public class UserServiceImpl implements UserService {

    @Override
    public String getUserName(String userId) {
        return "User-" + userId;
    }
}
```

:::warning 注意 @Component 的 value
`@Component` 的 value（此處為 `"userServiceImpl"`）必須與 `application.yml` 中 `beanName` 的值完全一致，否則 `WebServiceRegisterAutoConfiguration` 在啟動時將找不到對應 Bean 並拋出例外。
:::

## Step 6：以客戶端工具呼叫遠端服務

`WebServiceClientUtil` 以動態 WSDL 解析呼叫遠端端點，**不需要在編譯期匯入對方的 stub 類別**。

```java
import com.zipe.util.WebServiceClientUtil;

public class UserServiceClient {

    public Object[] call() throws Exception {
        // 建立工具實例：傳入 WSDL URL、操作名稱、參數陣列
        WebServiceClientUtil client = new WebServiceClientUtil(
            "http://localhost:8080/webservice/user?wsdl",
            "getUserName",
            new Object[]{"1001"}
        );
        // 回傳 Object[]，第 0 個元素為方法回傳值
        return client.invoke();
    }
}
```

:::info 回傳型別
`invoke()` 回傳 `Object[]`，第 0 個元素為實際的方法回傳值，需手動轉型（如 `(String) result[0]`）。
:::

## Step 7：執行驗證

啟動應用程式並確認 WSDL：

```bash
./mvnw spring-boot:run
curl "http://localhost:8080/webservice/user?wsdl"
```

若能取得 WSDL 文件，代表服務端已成功發布。執行客戶端程式後應回傳 `User-1001`。

:::tip WSDL 路徑組成
WSDL 端點路徑由 `web.service.uri-mapping`（去除萬用字元 `*`）加上各端點的 `uri-mapping` 組成，最後加上 `?wsdl`。

以預設設定為例：`/webservice/` + `/user` = `/webservice/user?wsdl`
:::

:::warning 命名空間相容性
SOAP 客戶端與服務端的 `targetNamespace` 需一致。若由外部 WSDL 產生客戶端 stub，請確認與服務端介面的 `targetNamespace` 相符。
:::
