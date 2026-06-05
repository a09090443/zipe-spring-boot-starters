---
id: configuration
title: 配置參考
sidebar_position: 3
---

# 配置參考

本頁列出 `web-spring-boot-starter` 的所有可設定屬性，屬性前綴為 `web.*`（注意：**不是** `zipe.web.*`）。屬性分為四個子組：靜態資源、JSP、Thymeleaf 設定，由 `WebPropertyConfig` 聚合。

:::warning 正確的屬性前綴
本模組使用 `web.*` 作為頂層屬性前綴，例如 `web.thymeleaf.enable`、`web.resource.pathPattern`。**請勿使用** `zipe.web.*`、`web.view-type`、`web.prefix` 等不存在的屬性名稱，這些屬性設定後不會生效。
:::

## 靜態資源屬性（`web.resource.*`）

| 屬性鍵 | 預設值 | 說明 |
|---|---|---|
| `web.resource.pathPattern` | `/static/**` | 靜態資源對外暴露的 URL 路徑模式 |
| `web.resource.location` | `/WEB-INF/static/` | 靜態資源實體目錄（相對於 webapp root） |

## JSP 屬性（`web.jsp.*`）

| 屬性鍵 | 預設值 | 說明 |
|---|---|---|
| `web.jsp.enable` | `false` | 是否啟用 JSP ViewResolver；明確設為 `true` 才會注冊 Bean |
| `web.jsp.viewNames` | `jsp/*` | glob 模式，只有符合此模式的視圖名稱才由 JSP 解析。預設僅 `jsp/` 目錄下的視圖名稱使用 JSP |
| `web.jsp.stuff` | `.jsp` | JSP 視圖檔案副檔名（對應 `InternalResourceViewResolver.suffix`） |

**視圖名稱對應規則（JSP）：**

視圖名稱 `"jsp/hello"` → 實體路徑 `/WEB-INF/jsp/hello.jsp`

公式：固定 prefix `/WEB-INF/` + 視圖名稱 + `web.jsp.stuff`

## Thymeleaf 屬性（`web.thymeleaf.*`）

| 屬性鍵 | 預設值 | 說明 |
|---|---|---|
| `web.thymeleaf.enable` | `true` | 是否啟用 Thymeleaf ViewResolver；明確設為 `true` 才會注冊 Bean |
| `web.thymeleaf.viewNames` | `html/*,vue/*,templates/*,th/*` | glob 模式（逗號分隔），只有符合此模式的視圖名稱才由 Thymeleaf 解析 |
| `web.thymeleaf.stuff` | `.html` | Thymeleaf 模板副檔名（對應 `ThymeleafViewResolver` 的 suffix） |
| `web.thymeleaf.templateMode` | `HTML` | Thymeleaf 模板模式；常用值：`HTML`、`XML`、`TEXT` |

**視圖名稱對應規則（Thymeleaf）：**

視圖名稱 `"th/message"` → 實體路徑 `/WEB-INF/th/message.html`

公式：固定 prefix `/WEB-INF/` + 視圖名稱 + `web.thymeleaf.stuff`

**viewNames 的 glob 模式說明：**

`viewNames` 屬性使用 Ant-style glob，`*` 匹配單層目錄。例如設定 `html/*` 表示只匹配 `html/` 開頭的視圖名稱（如 `html/index`、`html/list`），不匹配 `html/sub/page`（需改為 `html/**`）。

## 完整 application.yml 範例

### Thymeleaf 設定

```yaml
web:
  resource:
    pathPattern: /static/**
    location: /WEB-INF/static/
  thymeleaf:
    enable: true
    viewNames: "html/*,vue/*,templates/*,th/*"
    stuff: .html
    templateMode: HTML
  jsp:
    enable: false
```

### JSP 設定

```yaml
web:
  resource:
    pathPattern: /static/**
    location: /WEB-INF/static/
  jsp:
    enable: true
    viewNames: "jsp/*"
    stuff: .jsp
  thymeleaf:
    enable: false
```

### JSP 與 Thymeleaf 共存設定

```yaml
web:
  resource:
    pathPattern: /static/**
    location: /WEB-INF/static/
  jsp:
    enable: true
    viewNames: "jsp/*"        # JSP 解析器（order=1）處理 jsp/ 開頭的視圖名稱
    stuff: .jsp
  thymeleaf:
    enable: true
    viewNames: "html/*,th/*"  # Thymeleaf 解析器（order=2）處理 html/ 與 th/ 開頭
    stuff: .html
    templateMode: HTML
```

## 視圖解析器優先順序

當 JSP 與 Thymeleaf 同時啟用時，兩個 ViewResolver 依以下 order 排序：

| ViewResolver | order | 對應屬性 | 實體路徑規則 |
|---|---|---|---|
| `InternalResourceViewResolver`（JSP） | 1 | `web.jsp.viewNames` | `/WEB-INF/` + 視圖名稱 + `web.jsp.stuff` |
| `ThymeleafViewResolver` | 2 | `web.thymeleaf.viewNames` | `/WEB-INF/` + 視圖名稱 + `web.thymeleaf.stuff` |

DispatcherServlet 依 order 由小到大嘗試各 ViewResolver；第一個 viewNames 匹配成功的解析器負責渲染。**必須確保兩者的 viewNames 不重疊**，否則 JSP 解析器（order=1）會搶先命中本應由 Thymeleaf 渲染的視圖。

## 模板檔案目錄結構

所有視圖模板應放在 `src/main/webapp/WEB-INF/` 下：

```
src/main/webapp/
└── WEB-INF/
    ├── static/          # 靜態資源（對應 web.resource.location）
    │   ├── css/
    │   └── js/
    ├── jsp/             # JSP 模板（對應 web.jsp.viewNames = "jsp/*"）
    │   └── hello.jsp
    ├── html/            # Thymeleaf 模板（html/* 模式）
    │   └── hello.html
    └── th/              # Thymeleaf 模板（th/* 模式）
        └── message.html
```

:::warning 視圖引擎二選一或正確分流
若同時啟用兩種引擎，必須透過 `viewNames` glob 模式確保每個視圖名稱只被一個解析器命中。最簡單的做法是：JSP 視圖名稱一律以 `jsp/` 開頭，Thymeleaf 視圖名稱一律以 `html/` 或 `th/` 開頭，與預設值一致。
:::

:::warning Thymeleaf 快取設定
`web-spring-boot-starter` 內部將 Thymeleaf 的 `cacheable` 設為 `false`，適合開發環境（修改模板後重新整理即可看到效果）。

**生產環境請加入以下設定以啟用快取：**

```yaml
spring:
  thymeleaf:
    cache: true
```

未啟用快取時，每次請求都重新解析模板，會有明顯的性能開銷。
:::

:::info JSP 的容器與打包需求
JSP 需要支援 JSP 編譯的 Servlet 容器（`tomcat-embed-jasper`）。若以 JAR 方式打包並使用嵌入式 Tomcat，JSP 渲染需要在 `classpath` 可存取的位置放置模板（通常需要 WAR 封裝）。新專案建議優先採用 Thymeleaf。
:::
