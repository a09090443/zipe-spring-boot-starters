---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `web-service-spring-boot-starter` 的服務端發布、客戶端呼叫、CDATA 處理、欄位轉換器，以及底層 HTTP SOAP 呼叫等用法。

---

## 基礎使用範例

### 發布服務端點

以下示範定義一個訂單服務介面並完成發布。**服務介面需使用 `jakarta.jws.*` 命名空間**（Spring Boot 3.x）。

```java
// OrderService.java
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

@WebService(targetNamespace = "http://service.example.com")
public interface OrderService {

    @WebMethod(action = "createOrder")
    @WebResult(name = "orderId")
    String createOrder(@WebParam(name = "amount") double amount);
}
```

```java
// OrderServiceImpl.java
import jakarta.jws.WebService;
import org.springframework.stereotype.Component;

@WebService(
    serviceName = "OrderService",
    targetNamespace = "http://service.example.com",
    endpointInterface = "com.example.service.OrderService"
)
@Component("orderServiceImpl")          // beanName 需與 yml 設定的 beanName 一致
public class OrderServiceImpl implements OrderService {

    @Override
    public String createOrder(double amount) {
        return "ORDER-" + System.currentTimeMillis() + "-" + amount;
    }
}
```

搭配 `application.yml` 設定：

```yaml
web:
  service:
    uri-mapping: /webservice/*
    map:
      order:
        beanName: orderServiceImpl
        uri-mapping: /order
```

重啟後 WSDL 可從 `http://localhost:8080/webservice/order?wsdl` 取得。

---

### 動態客戶端呼叫遠端服務（不需 stub）

`WebServiceClientUtil` 以動態 WSDL 解析建立 CXF Client，**不需要在編譯期匯入對方的 stub 類別**，適用於呼叫第三方或政府 SOAP 服務。

```java
import com.zipe.util.WebServiceClientUtil;

public class OrderClient {

    public String createRemoteOrder() throws Exception {
        // 建立工具實例：WSDL URL、操作名稱、參數陣列
        WebServiceClientUtil client = new WebServiceClientUtil(
            "http://remote-host:8080/webservice/order?wsdl",
            "createOrder",
            new Object[]{199.0}
        );
        // 無認證呼叫；回傳 Object[]，第 0 個元素為方法回傳值
        Object[] result = client.invoke();
        return (String) result[0];
    }
}
```

---

## 進階使用範例

### 為客戶端附加 SOAP Header 認證

使用 `ClientLoginInterceptor` 在 SOAP Header 中插入帳號密碼，適用於需要自訂 SecurityHeader 格式的服務端。

:::warning 已知 Bug：認證條件邏輯反向
`WebServiceClientUtil.invoke(String username, String password)` 中的條件判斷為 `isBlank`（應為 `isNotBlank`），導致傳入真實帳密時攔截器**不會**被加入。

在 bug 修復前，請改用以下手動方式直接操作 CXF Client：
:::

```java
import com.zipe.util.ClientLoginInterceptor;
import org.apache.cxf.jaxws.JaxWsDynamicClientFactory;
import org.apache.cxf.endpoint.Client;

public class SecuredClient {

    public String callWithAuth() throws Exception {
        JaxWsDynamicClientFactory factory = JaxWsDynamicClientFactory.newInstance();
        Client client = factory.createClient(
            "http://remote-host:8080/webservice/order?wsdl");

        // 手動加入認證攔截器（不依賴 WebServiceClientUtil 的 username/password 參數）
        client.getOutInterceptors().add(
            new ClientLoginInterceptor("ws-user", "ws-password"));

        Object[] result = client.invoke("createOrder", 50.0);
        return (String) result[0];
    }
}
```

:::note ClientLoginInterceptor 的 SOAP Header 格式
`ClientLoginInterceptor` 插入的 SOAP Header 格式如下，注意 `authrity` 為程式碼中的原始拼字（非標準 `authority`）：

```xml
<SecurityHeader>
    <authrity>
        <userName>ws-user</userName>
        <userPassword>ws-password</userPassword>
    </authrity>
</SecurityHeader>
```

若對接的外部系統以 XPath 解析 Header，需確認對方是否接受此欄位名稱。
:::

---

### 在欄位上啟用 CDATA 包裹（CdataAdapter）

當服務回應的某個欄位可能包含 HTML 或 XML 特殊字元（如 `<`、`>`、`&`）時，在 JAXB 模型的 getter 上標注 `@XmlJavaTypeAdapter(CdataAdapter.class)`，即可讓序列化時自動包裹 `<![CDATA[...]]>`。

```java
import com.zipe.adapt.CdataAdapter;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement
public class Product {

    private String name;
    private String htmlDescription;   // 可能含 <b>、<a href="..."> 等標籤

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    @XmlJavaTypeAdapter(CdataAdapter.class)    // 含特殊字元的欄位加此標注
    public String getHtmlDescription() {
        return htmlDescription;
    }

    // setters...
}
```

序列化後的 XML 會如下所示，`htmlDescription` 欄位自動包裹 CDATA：

```xml
<Product>
    <name>Spring Boot 入門</name>
    <htmlDescription><![CDATA[<b>詳細說明</b> & <a href="/detail">更多</a>]]></htmlDescription>
</Product>
```

`ResponseCdataInterceptor`（已自動掛載於所有端點）會確保 CDATA 區段在最終 HTTP 回應中不被 JAXB 再次轉義。

---

### 使用 XmlUtil 處理 XML

`XmlUtil` 是基於 Jackson `XmlMapper` 的靜態工具，提供物件與 XML 字串之間的便利轉換。注意正確的方法名稱為 `beanToXml` 和 `xmlToBean`（非 `toXml` / `fromXml`）。

```java
import com.zipe.util.XmlUtil;

public class XmlExample {

    // Java 物件 → XML 字串
    public String objectToXml(Product product) {
        return XmlUtil.beanToXml(product);
        // 輸出範例：<Product><name>Spring Boot 入門</name>...</Product>
    }

    // XML 字串 → Java 物件
    public Product xmlToObject(String xml) {
        return XmlUtil.xmlToBean(xml, Product.class);
    }
}
```

`XmlUtil` 的序列化設定：包含 null 欄位、忽略未知欄位、日期格式為 `yyyy-MM-dd'T'HH:mm:ss.SSS`、支援 Java 8 時間型別（`JavaTimeModule`）。

---

### 底層 HTTP SOAP 呼叫（SoapUtil）

當無法取得 WSDL，或需要直接組裝 SOAP Envelope 時，使用 `SoapUtil` 以 Apache HttpClient 直接發送 SOAP 訊息。

```java
import com.zipe.util.SoapUtil;

public class RawSoapExample {

    public String callExternalService() throws Exception {
        // 手動組裝 SOAP Envelope
        String soapEnvelope = """
            <?xml version='1.0' encoding='UTF-8'?>
            <soapenv:Envelope
                xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                xmlns:ord="http://service.example.com">
                <soapenv:Header/>
                <soapenv:Body>
                    <ord:createOrder>
                        <amount>199.0</amount>
                    </ord:createOrder>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

        // 發送 HTTP POST，Content-Type: application/xml
        String rawResponse = SoapUtil.doPostWithXml(
            "http://remote-host:8080/webservice/order",
            soapEnvelope
        );

        // 從 SOAP 回應中抽取指定標籤的內容
        String orderId = SoapUtil.getResponseXml(rawResponse, "orderId");
        return orderId;
    }
}
```

---

## 常見情境

### 情境一：回傳含特殊字元的內容

當服務回傳值可能含 `<`、`&` 等特殊字元（例如 HTML 片段、嵌入 XML）時，在 JAXB 模型欄位上標注 `@XmlJavaTypeAdapter(CdataAdapter.class)` 即可。CDATA 攔截器（`CdataContentInterceptor` / `ResponseCdataInterceptor`）已對所有端點自動掛載，**不需要任何設定**。

```java
// 在 JAXB 模型 getter 上標注
@XmlJavaTypeAdapter(CdataAdapter.class)
public String getContent() { return content; }
```

### 情境二：對接政府或第三方 SOAP 服務

以對方提供的 WSDL URL 建立 `WebServiceClientUtil`，動態解析後即可呼叫，無需 stub 程式碼：

```java
WebServiceClientUtil client = new WebServiceClientUtil(
    "https://gov-service.example.gov.tw/service?wsdl",
    "queryTaxInfo",
    new Object[]{"A123456789"}
);
Object[] result = client.invoke();
```

### 情境三：同時發布多個端點

只需在 `application.yml` 的 `map` 下新增條目，每個 Bean 對應一個獨立端點：

```yaml
web:
  service:
    uri-mapping: /webservice/*
    map:
      user:
        beanName: userServiceImpl
        uri-mapping: /user             # → /webservice/user?wsdl
      order:
        beanName: orderServiceImpl
        uri-mapping: /order            # → /webservice/order?wsdl
      product:
        beanName: productServiceImpl
        uri-mapping: /product          # → /webservice/product?wsdl
```

### 情境四：除錯 SOAP 訊息

若需要檢視完整的 SOAP 請求與回應，可在業務專案中手動配置 CXF LoggingFeature：

```java
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.Bus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CxfLoggingConfig {

    @Bean
    public LoggingFeature loggingFeature(Bus bus) {
        LoggingFeature feature = new LoggingFeature();
        feature.setPrettyLogging(true);
        feature.initialize(bus);
        return feature;
    }
}
```

:::warning 安全性提醒
LoggingFeature 會將完整 SOAP 訊息輸出至日誌，可能包含帳號密碼等敏感資料。請僅在開發或除錯環境啟用，正式環境務必關閉。
:::

---

## 常見問題

- **取不到 WSDL（404）**：確認 `web.service.uri-mapping` 與 `web.service.map.<name>.uri-mapping` 設定正確，且 Bean 名稱（`beanName`）與 `@Component` 的 value 一致。
- **客戶端解析失敗**：多半是命名空間不一致，請核對服務端介面與客戶端 WSDL 的 `targetNamespace`。
- **CDATA 內容仍被跳脫**：確認 JAXB 模型 getter 上有 `@XmlJavaTypeAdapter(CdataAdapter.class)`；CDATA 攔截器已自動掛載，不需額外設定。
- **認證未生效**：注意 `WebServiceClientUtil.invoke(username, password)` 目前存在 `isBlank` 邏輯反向的 bug，請改用手動方式將 `ClientLoginInterceptor` 加入 `client.getOutInterceptors()`。
- **`javax` / `jakarta` 類別找不到**：Spring Boot 3.x 須使用 `jakarta.jws.*`，請移除舊版 `javax` 相依，並確認 CXF 版本為 4.0.x。
- **`UserServiceImpl` 被業務專案掃描到**：本 starter jar 內含示範用的 `UserServiceImpl`（帶有 `@Component`），若業務專案的 component scan 範圍涵蓋 `com.zipe`，可能被自動掃描到。請將業務專案的 scan 範圍限縮至自身套件，或使用 `@ComponentScan(excludeFilters = ...)` 排除。

:::tip 最佳實踐
服務介面應保持穩定，避免任意調整方法簽章或命名空間，以免破壞既有客戶端。對於對外服務，建議以版本化的端點路徑（如 `/webservice/order/v1`）管理相容性。
:::
