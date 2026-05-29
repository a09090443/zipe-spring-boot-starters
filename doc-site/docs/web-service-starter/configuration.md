---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `web-service-spring-boot-starter` 的可設定屬性，集中於 `zipe.web-service.*`，用於設定 CXF 的服務路徑、端點與 CDATA 行為。

## WebService 屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.web-service.path` | String | `/services` | CXF Servlet 的路徑前綴 | 否 |
| `zipe.web-service.address` | String | 無 | 服務發布的相對位址 | 否 |
| `zipe.web-service.enable-cdata` | Boolean | `false` | 是否啟用 CDATA 攔截器 | 否 |
| `zipe.web-service.publish-wsdl` | Boolean | `true` | 是否公開 WSDL | 否 |
| `zipe.web-service.in-logging` | Boolean | `false` | 是否記錄入站訊息 | 否 |
| `zipe.web-service.out-logging` | Boolean | `false` | 是否記錄出站訊息 | 否 |

## 客戶端屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.web-service.client.connection-timeout` | Long | `30000` | 連線逾時（毫秒） | 否 |
| `zipe.web-service.client.receive-timeout` | Long | `60000` | 接收逾時（毫秒） | 否 |
| `zipe.web-service.client.username` | String | 無 | 客戶端認證帳號 | 否 |
| `zipe.web-service.client.password` | String | 無 | 客戶端認證密碼 | 否 |

## 完整 application.yml 範例

```yaml
zipe:
  web-service:
    path: /services
    address: /user
    enable-cdata: true
    publish-wsdl: true
    in-logging: true
    out-logging: true
    client:
      connection-timeout: 30000
      receive-timeout: 60000
      username: ws-client
      password: ${WS_CLIENT_PASSWORD}
```

:::warning CDATA 攔截器
`enable-cdata` 啟用後，回應中的字串內容會被包裹於 `<![CDATA[ ... ]]>`。若對接的客戶端不預期 CDATA，可能造成解析差異；請與對方確認後再行啟用。
:::

:::note 訊息日誌
`in-logging` 與 `out-logging` 會輸出完整的 SOAP 訊息，對除錯非常有用，但訊息可能包含機敏資料。請僅在開發或除錯環境啟用，正式環境建議關閉。
:::

:::info 逾時設定
跨網段或經由 VPN 呼叫外部 WebService 時，網路延遲較高，建議適度調高 `receive-timeout`，避免大型回應尚未接收完成即逾時失敗。
:::
