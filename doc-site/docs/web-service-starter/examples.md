---
id: examples
title: 使用範例
sidebar_position: 4
---

# 使用範例

本頁示範 `web-service-spring-boot-starter` 的服務端發布、客戶端呼叫、CDATA 與認證攔截器等用法。

## 基礎使用範例

### 發布服務端點

```java
import jakarta.jws.WebService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;

@WebService
public interface OrderService {
    @WebMethod
    String createOrder(@WebParam(name = "amount") double amount);
}
```

```java
import com.zipe.config.Service;
import jakarta.jws.WebService;

@Service
@WebService(endpointInterface = "com.example.OrderService")
public class OrderServiceImpl implements OrderService {

    @Override
    public String createOrder(double amount) {
        return "ORDER-" + System.currentTimeMillis() + "-" + amount;
    }
}
```

### 呼叫遠端服務

```java
import com.zipe.util.WebServiceClientUtil;

public class OrderClient {

    public String createRemoteOrder() {
        OrderService service = WebServiceClientUtil.create(
            OrderService.class,
            "http://remote-host:8080/services/order?wsdl");
        return service.createOrder(199.0);
    }
}
```

## 進階使用範例

### 為客戶端附加認證資訊

使用 `ClientLoginInterceptor` 為請求加入帳密，對接需要認證的服務：

```java
import com.zipe.util.ClientLoginInterceptor;
import com.zipe.util.WebServiceClientUtil;

public class SecuredClient {

    public String call() {
        OrderService service = WebServiceClientUtil.create(
            OrderService.class,
            "http://remote-host:8080/services/order?wsdl",
            new ClientLoginInterceptor("ws-client", "secret"));
        return service.createOrder(50.0);
    }
}
```

### 使用 XmlUtil 處理 XML

```java
import com.zipe.util.XmlUtil;

public class XmlExample {

    public String toXml(Object obj) {
        return XmlUtil.toXml(obj);
    }

    public <T> T fromXml(String xml, Class<T> clazz) {
        return XmlUtil.fromXml(xml, clazz);
    }
}
```

## 常見情境

### 情境一：回傳含特殊字元的內容

當回傳值可能含 `<`、`&` 等特殊字元（例如 HTML 片段）時，啟用 CDATA 攔截器可避免 XML 解析錯誤：

```yaml
zipe:
  web-service:
    enable-cdata: true
```

### 情境二：對接政府或第三方 SOAP 服務

由對方提供的 WSDL 產生介面後，以 `WebServiceClientUtil.create` 建立 proxy 即可呼叫；若對方要求逾時較長，於設定中調高 `receive-timeout`。

### 情境三：除錯 SOAP 訊息

開啟 `in-logging` 與 `out-logging`，於日誌中檢視完整的請求與回應 SOAP 封包，快速定位欄位對應或命名空間問題。

## 常見問題

- **取不到 WSDL（404）**：確認 `path` 與 `address` 設定，且 `publish-wsdl` 為 `true`；WSDL 路徑為 `{path}{address}?wsdl`。
- **客戶端解析失敗**：多半是命名空間不一致，請核對服務端介面與客戶端 stub 的 `targetNamespace`。
- **CDATA 內容被跳脫**：確認 `enable-cdata` 已啟用，且攔截器正確套用於目標端點。
- **`javax` / `jakarta` 類別找不到**：Spring Boot 3.x 須使用 `jakarta.jws.*`，請移除舊版 `javax` 相依。

:::tip 最佳實踐
服務介面應保持穩定，避免任意調整方法簽章或命名空間，以免破壞既有客戶端。對於對外服務，建議以版本化的端點路徑（如 `/services/order/v1`）管理相容性。
:::
