---
id: scenario-webservice
title: 情境四：SOAP WebService
sidebar_position: 5
description: 使用 base + web-service Starter 發布 CXF SOAP 服務
---

# 情境四：SOAP WebService

## 情境說明

本情境示範如何建構一個**對外發布 SOAP WebService 的系統**。雖然 RESTful API 已是主流，但在企業環境中，仍有大量既有系統（如政府機關、金融、ERP）採用 SOAP 協議。`web-service-spring-boot-starter` 以 Apache CXF 為核心，讓你只需定義介面與實作、加上少量設定，即可自動發布 SOAP 端點並產生 WSDL，無須手動撰寫任何 CXF 配置類別。

## 使用的 Starters

| Starter | 在本情境的角色 |
|---|---|
| `base-spring-boot-starter` | 提供工具類等基礎設施（已被 web-service-starter 依賴） |
| `web-service-spring-boot-starter` | 提供 CXF Servlet 掛載與 Endpoint 自動發布 |

## pom.xml 依賴配置

`web-service-starter` 自身已包含 `cxf-spring-boot-starter-jaxws`、`cxf-rt-databinding-jaxb`、`jaxws-ri`、`httpclient` 與 `base-spring-boot-starter`，因此業務模組只需引入：

```xml
<dependencies>
    <dependency>
        <groupId>io.github.a09090443</groupId>
        <artifactId>web-service-spring-boot-starter</artifactId>
        <version>${zipe.spring.starter.version}</version>
    </dependency>
</dependencies>
```

## application.yml 端點設定

```yaml
server:
  port: 8080
  servlet:
    context-path: /example

web:
  service:
    uri-mapping: /webservice/*     # CXFServlet 掛載路徑
    map:
      example:
        beanName: exampleWebServiceImpl   # Spring Bean 名稱
        uri-mapping: /example             # 此服務的子路徑
```

完整存取 URL 由以下各段組成：

```
http://localhost:8080/{context-path}/{uri-mapping}/{子路徑}?wsdl
= http://localhost:8080/example/webservice/example?wsdl
```

| 段落 | 來源 |
|---|---|
| `8080` | `server.port` |
| `/example` | `server.servlet.context-path` |
| `/webservice/*` | `web.service.uri-mapping`（CXFServlet 攔截） |
| `/example` | `web.service.map.example.uri-mapping` |

## 定義 WebService 介面

介面即 SEI（Service EndPoint Interface），以 `@WebService` 標註並明確指定 `targetNamespace`。以下為 `starters_example` 的 `ExampleWebService` 完整定義：

```java
@WebService(name = "UserService", targetNamespace = "http://service.example.com")
public interface ExampleWebService {

    @WebMethod
    User getUser(@WebParam(name = "userId",
            targetNamespace = "http://service.example.com") String userId);

    @WebMethod
    String getUserName(@WebParam(name = "userId",
            targetNamespace = "http://service.example.com") String userId);

    @WebMethod
    @WebResult(name = "Map")
    Map<String, User> getAllUserData();
}
```

| 屬性 | 值 | 說明 |
|---|---|---|
| `name` | `UserService` | WSDL 中 portType 的名稱 |
| `targetNamespace` | `http://service.example.com` | XML 命名空間，動態呼叫時必須正確對應 |

所有 `@WebParam` 都明確設定相同的 `targetNamespace`，確保動態呼叫時命名空間一致。

## 實作 WebService

實作類別需加上 `@WebService`（指定 `endpointInterface`）與 `@Component`（讓 Spring 管理，Bean name 預設為 `exampleWebServiceImpl`）：

```java
@WebService(
    serviceName = "ExampleWebService",
    targetNamespace = "http://service.example.com",
    endpointInterface = "com.example.webservice.ExampleWebService"
)
@Component
public class ExampleWebServiceImpl implements ExampleWebService {

    private final Map<String, User> userMap = new HashMap<>();

    public ExampleWebServiceImpl() {
        // 在建構子中寫入測試資料，不依賴資料庫
        userMap.put("01", new User("01", "Gary"));
        userMap.put("02", new User("02", "John"));
        userMap.put("03", new User("03", "Mary"));
    }

    @Override
    public User getUser(String userId) {
        return userMap.get(userId);
    }

    @Override
    public String getUserName(String userId) {
        return userMap.get(userId).getUserName();
    }

    @Override
    public Map<String, User> getAllUserData() {
        return userMap;
    }
}
```

- `serviceName`：對外發布的服務名稱（出現在 WSDL 的 `<service name>` 中）。
- `endpointInterface`：指定 SEI 完整類別路徑，使 CXF 知道要暴露哪個介面。

## 驗證 WSDL

應用程式啟動後，在瀏覽器直接開啟以下路徑即可查看服務定義：

```
http://localhost:8080/example/webservice/example?wsdl
```

WSDL 文件中可看到：

- `<wsdl:definitions targetNamespace="http://service.example.com">`：命名空間
- `<wsdl:portType name="UserService">`：對應介面的 `@WebService(name="UserService")`
- `<wsdl:service name="ExampleWebService">`：對應實作的 `serviceName`
- 各方法的 `<wsdl:operation>`、入參、出參、型別定義

也可查看所有已發布服務清單：`http://localhost:8080/example/webservice`。

## 撰寫 Client 測試呼叫

`starters_example` 提供三種呼叫方式，皆針對同一個 WSDL 端點。這些測試是純客戶端測試，**需要應用程式已在本地啟動**才能執行。

### 方式一：JaxWsProxyFactoryBean（強型別代理）

```java
JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
factory.setAddress(WEB_SERVICE_URL);
factory.setServiceClass(ExampleWebService.class);
ExampleWebService us = (ExampleWebService) factory.create();
User result = us.getUser("02");
```

可直接用介面方法呼叫，強型別、IDE 支援好，但需引用介面類別。

### 方式二：WebServiceClientUtil（工具類別封裝）

```java
WebServiceClientUtil clientUtil =
        new WebServiceClientUtil(WEB_SERVICE_URL, "getUser", new Object[]{"01"});
Object[] result = clientUtil.invoke();
```

由 `web-service-starter` 提供的工具類別封裝了動態呼叫邏輯，無需引用介面類別，適合跨系統使用。

### 方式三：JaxWsDynamicClientFactory（純動態呼叫）

```java
JaxWsDynamicClientFactory factory = JaxWsDynamicClientFactory.newInstance();
Client client = factory.createClient(WEB_SERVICE_URL);
QName qname = new QName("http://service.example.com", "getUserName");
Object[] result = client.invoke(qname, "01");
```

完全動態，透過 `QName` 指定 `targetNamespace` 與方法名稱，無需任何介面類別，最靈活但需確保 namespace 正確。

:::info CXF 自動產生 WSDL 的機制
你不需要手寫 WSDL。`web-service-starter` 由兩個自動配置類別分工完成發布：

- `CxfConfigAutoConfiguration`：建立 `ServletRegistrationBean`，將 `CXFServlet` 掛載至 `web.service.uri-mapping`（預設 `/webservice/*`），使所有該路徑的請求交給 CXF 處理。
- `WebServiceRegisterAutoConfiguration`：實作 `InitializingBean`，在 `afterPropertiesSet()` 中讀取 `web.service.map` 的每個條目，從 Context 取出對應 Bean，建立 `EndpointImpl` 並呼叫 `endpoint.publish(uriMapping)` 發布。

CXF 會根據介面與實作上的 JAX-WS 注解（`@WebService`、`@WebMethod`、`@WebParam`、`@WebResult`），在執行期間自動產生對應的 WSDL，因此你只要維護 Java 程式碼，WSDL 永遠與程式同步。
:::
