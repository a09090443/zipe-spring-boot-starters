---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `web-spring-boot-starter` 的可設定屬性，集中於 `zipe.web.*`，涵蓋視圖引擎選擇、視圖路徑、靜態資源與 Tomcat 設定。

## 視圖屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.web.view-type` | String | `thymeleaf` | 視圖引擎：`jsp` 或 `thymeleaf` | 否 |
| `zipe.web.prefix` | String | `/WEB-INF/th/` | 視圖檔案路徑前綴 | 否 |
| `zipe.web.suffix` | String | `.html` | 視圖檔案副檔名 | 否 |
| `zipe.web.cache` | Boolean | `true` | 是否快取視圖樣板 | 否 |
| `zipe.web.encoding` | String | `UTF-8` | 視圖編碼 | 否 |

## 靜態資源屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.web.static-path-pattern` | String | `/static/**` | 靜態資源 URL 樣式 | 否 |
| `zipe.web.static-locations` | List | `classpath:/static/` | 靜態資源實體位置 | 否 |

## Tomcat 屬性

| 屬性名 | 型別 | 預設值 | 說明 | 必填 |
|---|---|---|---|---|
| `zipe.web.tomcat.uri-encoding` | String | `UTF-8` | URI 編碼 | 否 |
| `zipe.web.tomcat.max-threads` | Integer | `200` | 最大工作執行緒數 | 否 |
| `zipe.web.tomcat.connection-timeout` | Integer | `20000` | 連線逾時（毫秒） | 否 |

## 完整 application.yml 範例

Thymeleaf 設定範例：

```yaml
zipe:
  web:
    view-type: thymeleaf
    prefix: /WEB-INF/th/
    suffix: .html
    cache: false
    encoding: UTF-8
    static-path-pattern: /static/**
    static-locations:
      - classpath:/static/
    tomcat:
      uri-encoding: UTF-8
      max-threads: 200
      connection-timeout: 20000
```

JSP 設定範例：

```yaml
zipe:
  web:
    view-type: jsp
    prefix: /WEB-INF/jsp/
    suffix: .jsp
    cache: true
    encoding: UTF-8
```

:::warning 視圖引擎二選一
`view-type` 一次僅能指定一種引擎。同時啟用 JSP 與 Thymeleaf 會造成視圖解析器互相干擾，導致頁面渲染結果不如預期。
:::

:::note 開發階段關閉快取
開發階段建議將 `cache` 設為 `false`，修改樣板後即可直接重新整理瀏覽器看到效果，不需重啟應用；正式環境則應設為 `true` 以提升效能。
:::

:::info JSP 的容器需求
JSP 需要支援 JSP 編譯的 Servlet 容器。使用內嵌 Tomcat 並打包為 JAR 時，JSP 可能無法運作，建議改用 WAR 封裝或改採 Thymeleaf。
:::
