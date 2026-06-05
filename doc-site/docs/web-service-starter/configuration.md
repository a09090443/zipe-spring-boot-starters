---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `web-service-spring-boot-starter` 的可設定屬性，前綴為 `web.service.*`，用於設定 CXF Servlet 路徑與各 SOAP 端點的 Bean 對應關係。

:::info 屬性綁定類別
所有屬性由 `com.zipe.config.WebServicePropertyConfig`（`@ConfigurationProperties(prefix = "web.service")`）負責綁定。IDE（IntelliJ IDEA / VS Code + Spring Boot Extension）可透過 `spring-configuration-metadata.json` 提供自動補全。
:::

## 全域屬性

| 屬性鍵 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `web.service.uri-mapping` | `String` | `/webservice/*` | CXF Servlet 的 URL 前綴（含萬用字元 `*`）。所有透過本 starter 發布的端點，URL 均以此前綴起頭。 |

## 端點清單屬性（`web.service.map`）

`map` 為一個以**邏輯名稱**為 key 的 `Map`，每個 value 為一個 `Service` POJO，描述一個 SOAP 端點的發布設定。

| 屬性鍵 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `web.service.map.<name>.beanName` | `String` | 無 | 實作服務的 Spring Bean 名稱（`@Component` 的 value），`WebServiceRegisterAutoConfiguration` 以此從 `ApplicationContext` 取出 implementor。 | 是 |
| `web.service.map.<name>.uri-mapping` | `String` | 無 | 此端點在 CXF Servlet 下的相對路徑（如 `/user`）。最終 WSDL URL 為 `{web.service.uri-mapping（去除*）}{uri-mapping}?wsdl`。 | 是 |

其中 `<name>` 為任意邏輯名稱，僅作識別用，不影響執行行為。

## 完整 application.yml 範例

```yaml
web:
  service:
    uri-mapping: /webservice/*          # CXF Servlet URL 前綴，預設值
    map:
      user:                             # 邏輯名稱（任意）
        beanName: userServiceImpl       # Spring Bean 名稱（與 @Component value 一致）
        uri-mapping: /user              # 端點相對路徑 → WSDL 在 /webservice/user?wsdl
      order:                            # 可同時發布多個端點
        beanName: orderServiceImpl
        uri-mapping: /order             # WSDL 在 /webservice/order?wsdl
```

## WSDL URL 路徑計算規則

WSDL URL 由以下兩部分組成：

```
{web.service.uri-mapping（去除末尾的 /*）} + {map.<name>.uri-mapping} + ?wsdl
```

以預設設定為例：

| 設定值 | 說明 |
|---|---|
| `uri-mapping: /webservice/*` | CXF Servlet 前綴（`/*` 為 Servlet 萬用字元，取路徑時去除） |
| `map.user.uri-mapping: /user` | 端點相對路徑 |
| 最終 WSDL URL | `http://host:port/webservice/user?wsdl` |

## CDATA 攔截器說明

`CdataContentInterceptor`（入站）與 `ResponseCdataInterceptor`（出站）**對所有端點無條件掛載**，不需要任何設定即自動生效。

:::warning CDATA 攔截器無法透過設定關閉
目前兩個 CDATA 攔截器在 `WebServiceRegisterAutoConfiguration` 中為硬編碼行為，無法透過 `application.yml` 針對個別端點開啟或關閉。若端點欄位不含特殊字元，攔截器仍會執行（效能影響輕微，但無法禁用）。

若需要欄位級別的精確控制，請在 JAXB 模型欄位上使用 `@XmlJavaTypeAdapter(CdataAdapter.class)`，不依賴攔截器層級。
:::

攔截器的轉換規則（兩者相同，出站版額外多 `&amp;` → `&`）：

| 來源（轉義後） | 目標（恢復原始值） |
|---|---|
| `&lt;![CDATA[` | `<![CDATA[` |
| `]]&gt;` | `]]>` |
| `&lt;` | `<` |
| `&gt;` | `>` |
| `&amp;`（僅出站） | `&` |

## EndpointImpl 內建配置

`WebServiceRegisterAutoConfiguration` 在發布每個端點時，固定套用以下 JAXBDataBinding 設定：

| 設定項目 | 值 | 說明 |
|---|---|---|
| `set-jaxb-validation-event-handler` | `"false"` | 忽略空命名空間驗證事件，避免不必要的解析例外 |
| `JAXBDataBinding.unwrapJAXBElement` | `true` | 自動解開 `JAXBElement` 包裝 |
| `JAXBDataBinding.mtomEnabled` | `true` | 啟用 MTOM，支援二進位附件 |

以上設定目前為硬編碼，無法透過 `application.yml` 調整。

:::note 訊息日誌
若需要在開發或除錯環境輸出完整的 SOAP 請求與回應封包，可透過 CXF 的標準 Logging Feature 手動配置，本 starter 目前未提供設定開關。請注意 SOAP 訊息可能包含敏感資料，正式環境不建議啟用。
:::
