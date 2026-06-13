# Spring Boot 3.5.14 → 4.0.0 升級計畫

## 1. 簡介與目標

本文件彙整 `zipe-spring-boot-starters` 多模組專案從 **Spring Boot 3.5.14 升級至 4.0.0** 的完整衝擊分析與可執行步驟，作為後續逐步施工的依據。

升級目標：

- 將根 `pom.xml` 的 `spring-boot-starter-parent` 由 `3.5.14` 升至 `4.0.0`，並讓所有 reactor 子模組（base / db / job / logon / web / web-service）順利建構、測試通過。
- 完成 `javax.* → jakarta.*` 命名空間殘留遷移、HttpClient 4 → 5 遷移、CXF 4.0 → 4.2 跨版本升級等破壞性變更。
- 對無法原地升級的 `keycloak-spring-boot-starter` 做出 go/no-go 決策。
- 同步 `doc-site/` 文件與各 README 寫死的版本號與套件路徑。

> 本文件凡標註「需查證」之處，皆須於實際執行階段以 `mvn dependency:tree` 或查 Maven Central / SB4 BOM 確認後再定案。

---

## 2. 目標版本現況（Spring Boot 4.0.0）

| 項目 | 內容 |
|---|---|
| GA 日期 | 2025-11-20 正式發布 |
| Spring Framework | 6 → **7**（SF7） |
| Java baseline | **17+**（與本專案現況一致，無須提版） |
| Jakarta EE | 10 → **11**（Servlet 6.1、Jakarta Mail、Jakarta XML 等） |
| Spring Security | 6 → **7**（SS7） |
| Hibernate ORM | 6.6 → **7.1**（JPA / ORM） |
| 模組化 | autoconfigure 與 web/tomcat/jdbc/quartz 等自動配置類別拆分到更細的 jar，**多數型別的套件路徑位移** |
| Null-safety | 改採 **JSpecify**（`@Nullable`/`@NonNull`），編譯期提示 |
| HttpClient | 預設改用 **HttpClient 5**（`org.apache.hc.client5`），BOM 自 3.1 起即不再管理 HttpClient 4 |
| starter 改名 | `spring-boot-starter-web` 標記 deprecated，建議改 `spring-boot-starter-webmvc` |
| Thymeleaf（利多） | SB4 thymeleaf 仍相依 `thymeleaf-spring6:3.1.3.RELEASE`，**無 `thymeleaf-spring7`**，故 `org.thymeleaf.spring6.*` 不需改 |

---

## 3. 整體衝擊分級表

| 模組 | 風險 | 主因 |
|---|---|---|
| `root-pom`（根 pom.xml） | **高** | 第三方版本治理中心；CXF 4.0→4.2、httpclient 4→5、javax.servlet/javax.mail/velocity 1.7 等 javax 或停更套件需升版/替換；版本號規則 3.5.14.1 → 4.0.0.x 與發布 workflow 連動 |
| `base-spring-boot-starter` | **高** | 核心被依賴層；velocity 1.7（2010）/ logback-ext-spring 0.1.5（2014）古董套件去留；VelocityUtil 殘留 javax.servlet；jasperreports 6.21→7.0；com.sun.mail:javax.mail 多餘依賴移除 |
| `db-spring-boot-starter` | 中 | Hibernate 6.6→7.1 手動組裝 EntityManagerFactory、HikariCP 5→7、HibernateProperties 套件位移；p6spy / jt400 相容性 |
| `job-spring-boot-starter` | 中 | SB4 模組化：`QuartzDataSource`、`DataSourceProperties` 套件搬移；Quartz 升 2.5.1（由 BOM 帶動） |
| `logon-spring-boot-starter` | 中 | SS7：`DaoAuthenticationProvider` 無參數建構子棄用、`requestMatchers` 改 `PathPatternRequestMatcher` |
| `web-spring-boot-starter` | 中 | SB4 模組化套件位移：`ConfigurableServletWebServerFactory`、`TomcatServletWebServerFactory`、`ServletWebServerFactoryAutoConfiguration`（移除/改名） |
| `web-service-spring-boot-starter` | **高** | CXF 4.0→4.2 跨版本；SoapUtil HttpClient 4→5 全檔改寫；jaxws-ri 對齊 Jakarta EE 11 |
| `keycloak-spring-boot-starter` | **高** | 不在 reactor、parent 仍停在 2.4.4 / Java 11 / Keycloak 13；嵌入式架構已被上游移除，無法原地升級，需 go/no-go 決策 |

---

## 4. 各模組升級明細

### 4.1 root-pom（`D:/projects/zipe-spring-boot-starters/pom.xml`）— 風險：高

#### 總結

根 pom 是整個 reactor 的 parent 與依賴版本治理中心。衝擊集中三塊：(1) parent 與 `dependencyManagement` 大量第三方版本需重新盤點，含 javax 命名空間或不相容 SF7 的依賴須升版/替換；(2) 版本號規則 `3.5.14.1` 須改採 `4.0.0.x` 並同步調整發布 workflow；(3) release profile 外掛相對安全，僅 `central-publishing-maven-plugin 0.4.0` 偏舊。SB4 BOM 將接管更多依賴（httpclient5、commons-io、commons-collections4、poi、commons-beanutils），多筆自訂版本宣告可移除。最高風險為 CXF 升 4.2、httpclient 4→5、velocity 1.7 / logback-ext-spring / javax.servlet-api 等停更或 javax 套件。

#### 依賴異動表

| 依賴 | 現況 | 動作 | 目標 | 備註 |
|---|---|---|---|---|
| `spring-boot-starter-parent` | 3.5.14 | 升版 | 4.0.0 | 連帶 BOM 整批升級 |
| `org.apache.cxf:cxf-spring-boot-starter-jaxws` | 4.0.0 | 升版 | 4.2.x | 須 4.2 才支援 SF7/SB4/SS7；4.1 仍綁 SF6.1 不可用 |
| `org.apache.cxf:cxf-rt-databinding-jaxb` | 4.0.0 | 升版 | 4.2.x | 須與 CXF starter 同版號 |
| `org.apache.httpcomponents:httpclient` | 4.5.14 | 替換 | `httpcomponents.client5:httpclient5`（BOM 管理） | SB4 預設 HC5；使用端 import 需遷移 |
| `javax.servlet:javax.servlet-api` | 4.0.1 | 移除 | `jakarta.servlet:jakarta.servlet-api`（BOM 6.1.0） | javax 命名空間，改 jakarta 並建議移除自訂宣告 |
| `com.sun.mail:javax.mail` | 1.6.2 | 替換 | `jakarta.mail-api` + `angus-mail`（BOM） | javax 命名空間，使用端 import 遷移 |
| `net.sf.jasperreports:jasperreports` | 6.21.3 | 升版 | 7.0.x | 7.0 起才提供 Jakarta 相容；PDF 後端 iText 需評估 |
| `com.lowagie:itext` | `[1.02b,2.1.7]` | 確認相容 | 釘死 2.1.7 | Maven 4 嚴格解析避免 range 不確定性 |
| `org.apache.velocity:velocity` | 1.7 | 升版 | `velocity-engine-core` 2.3+ | 停更，artifactId 改名 |
| `org.logback-extensions:logback-ext-spring` | 0.1.5 | 確認相容 | 評估移除 | 2015 停更，SB 已內建 logback 整合 |
| `org.apache.poi:poi / poi-ooxml` | 5.2.5 | 確認相容 | BOM 管理 | 建議移除自訂版本由 BOM 統一 |
| `org.apache.commons:commons-collections4` | 4.5.0 | 確認相容 | BOM 管理（需查證） | 與 Jakarta 無關 |
| `commons-io:commons-io` | 2.22.0 | 確認相容 | BOM 管理 | 建議移除自訂版本 |
| `commons-beanutils:commons-beanutils` | 1.11.0 | 確認相容 | 確認 groupId（vs commons-beanutils2） | 停更，需查證 |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | 確認相容 | 4.12.0 / BOM | 純函式庫，風險低 |
| `p6spy:p6spy` | 3.9.1 | 確認相容 | 3.9.1 | JDBC proxy，與 Jakarta 無關 |
| `net.sf.jt400:jt400` | 20.0.6 | 確認相容 | 20.0.6 或更新 | 純 Java JDBC 驅動 |
| `com.sun.xml.ws:jaxws-ri` | 4.0.1 | 升版 | 對齊 CXF 4.2 | 避免 JAX-WS RI 與 CXF 衝突 |
| `org.sonatype.central:central-publishing-maven-plugin` | 0.4.0 | 升版 | 0.8.x（需查證） | 相容 Maven Central 新發布 API |
| `org.apache.maven.plugins:maven-javadoc-plugin` | 3.6.3 | 升版 | 3.11.x（需查證） | 支援 JDK 17+ javadoc |
| `org.apache.maven.plugins:maven-gpg-plugin` | 3.2.8 | 確認相容 | 3.2.8 | 已為最新 |
| `org.apache.maven.plugins:maven-source-plugin` | 3.4.0 | 確認相容 | 3.4.x | 風險低 |

#### 破壞性變更

- `spring-boot-starter-parent` 3.5.14→4.0.0：SF6→7、Jakarta EE 10→11、Security 6→7、Hibernate 6→7，所有子模組繼承此 parent。
- `javax.* → jakarta.*`：根 pom 直接宣告的 `javax.servlet:javax.servlet-api`、`com.sun.mail:javax.mail` 必須改 jakarta 座標。
- HttpClient 4（`org.apache.httpcomponents:httpclient`）→ HttpClient 5（`httpclient5`，`org.apache.hc.client5`）：API 不相容。
- Apache CXF 4.0.0 不相容 SF7，必須升至 4.2.x（含 starter-jaxws 與 rt-databinding-jaxb 同步）。
- JasperReports 6.21.3→7.0：需 Jakarta 相容版，PDF 匯出若需新版 iText 須引入 `jasperreports-pdf-lib7`。
- velocity 1.7 → 2.x：artifactId 改 `velocity-engine-core`，屬破壞性座標變更。
- SB4 BOM 接管更多依賴（poi、commons-io、commons-collections4 等），自訂版本若與 BOM 不一致將造成版本漂移。

#### 需修改的程式碼檔案

- `pom.xml`（parent 區塊 line 7-12）：`spring-boot-starter-parent` 版本 3.5.14 → 4.0.0。
- `pom.xml`（line 16 `<version>`）：專案版本號 3.5.14.1 → 4.0.0.x（沿用「SB 版本 + 遞增序號」規則）；同步調整以 git Release tag 驅動版號並回寫 pom 的 workflow。
- `pom.xml`（properties line 67 + dependencyManagement line 152-156）：移除 `javax.servlet-api.version`，改 `jakarta.servlet-api` 或交由 BOM。
- `pom.xml`（properties line 71 + dependencyManagement line 177-181）：`com.sun.mail:javax.mail` → `jakarta.mail-api` + `angus-mail` 或交由 BOM。
- `pom.xml`（properties line 82 + dependencyManagement line 219-223）：移除 `httpclient` 4.5.14，改 `httpclient5`（交由 BOM）。
- `pom.xml`（properties line 79-81）：`cxf-spring-boot-starter-jaxws.version`、`cxf-rt-databinding-jaxb.version` 4.0.0 → 4.2.x；`jaxws-ri.version` 對齊。
- `pom.xml`（properties line 66 + dependencyManagement line 147-151）：velocity 1.7 → `velocity-engine-core` 2.3+。
- `pom.xml`（properties line 68-70 + dependencyManagement line 157-171）：jasperreports 6.21.3 → 7.0.x；poi 評估交由 BOM。
- `pom.xml`（properties line 62-64 + dependencyManagement line 127-141）：盤點 commons-collections4 / commons-io / commons-beanutils 交由 BOM 與否。
- `pom.xml`（properties line 65 + dependencyManagement line 142-146）：評估移除 logback-ext-spring。
- `pom.xml`（properties line 88）：central-publishing-maven-plugin 0.4.0 → 0.8.x；maven-javadoc-plugin 3.6.3 → 3.11.x。
- `.github/workflows/*.yml`：確認 tag 命名、版號擷取邏輯、回寫格式適用 4.0.0.x。

#### 升級步驟

- [ ] 建立升級分支，避免直接動 master。
- [ ] 根 pom 將 `spring-boot-starter-parent` 改 4.0.0，`<version>` 改 4.0.0.x。
- [ ] 執行 `mvn dependency:tree` 與比對 `spring-boot-dependencies 4.0.0` BOM，列出被 BOM 接管的自訂版本（預期 poi、commons-io、commons-collections4、httpclient5、jstl、jdbc driver 等）。
- [ ] 移除已由 BOM 管理的自訂版本宣告（properties + dependencyManagement）。
- [ ] 處理 `javax.* → jakarta.*`：javax.servlet-api、com.sun.mail:javax.mail 改 jakarta 座標。
- [ ] httpclient 4 → httpclient5：移除舊座標，並通報 web-service-starter 等使用端遷移 import。
- [ ] CXF 升 4.2.x（starter-jaxws、rt-databinding-jaxb、jaxws-ri 同步對齊）。
- [ ] jasperreports 升 7.0.x，velocity 改 velocity-engine-core 2.3+，評估移除 logback-ext-spring。
- [ ] release profile 外掛升版（central-publishing、javadoc）。
- [ ] 各子模組逐一 `mvn -pl <module> -am clean install`，最後根目錄 `mvn clean install` 全量驗證。
- [ ] 調整 `.github/workflows` 發布版號規則並以 dry run 驗證 tag→版號回寫流程。

#### 驗證方式

- [ ] 根目錄 `mvn clean install` 全量建構通過（6 個 reactor 模組皆 BUILD SUCCESS）。
- [ ] `mvn dependency:tree` 確認無殘留 `javax.servlet` / `org.apache.http`(httpclient4) / `com.sun.mail:javax.mail`。
- [ ] `mvn -Prelease clean verify` 在不 deploy 下驗證 release profile（javadoc/gpg/central-publishing）外掛可正常執行。
- [ ] `mvn dependency:analyze` 檢查版本衝突或未宣告/多餘依賴。
- [ ] 以 starters_example 引入新版 starter 做煙霧測試（啟動、SOAP/WebService、報表匯出、Velocity 模板）。
- [ ] CI 上以測試 Release tag 驗證版號回寫 workflow 產出 4.0.0.x 正確。

---

### 4.2 base-spring-boot-starter — 風險：高

#### 總結

base-starter 是所有下游模組的共用基礎層，衝擊以「古董第三方套件」與「殘留 javax 依賴」為主，而非 Spring API 本身。郵件部分早在 SB3 已遷移到 `jakarta.mail`（`MailServiceImpl`/`MailService` 全部 import `jakarta.mail.*`/`jakarta.activation.*`），因此 `com.sun.mail:javax.mail:1.6.2` 是程式碼完全沒用到的多餘依賴，可直接移除（mail 由 `spring-boot-starter-mail` 提供）。真正的 javax→jakarta 痛點只剩 `VelocityUtil.java` 一處（import `javax.servlet.http.HttpServletRequest`）。velocity 1.7（2010）與 logback-ext-spring 0.1.5（2014）兩個古董是最高風險：velocity 需升 `velocity-engine-core` 2.4.x（artifact 改名、移除 commons-lang 2.x 硬依賴、WebappLoader 移到 velocity-tools），logback-ext-spring 經 grep 確認程式碼完全未使用，應直接移除。jasperreports 6.21.3 不支援 jakarta，須升 7.x。POI/commons/gson/okhttp 屬低風險。整體屬高風險是因它是核心被依賴模組，且涉及兩個無人維護的古董套件去留決策。

#### 依賴異動表

| 依賴 | 現況 | 動作 | 目標 | 備註 |
|---|---|---|---|---|
| `com.sun.mail:javax.mail` | 1.6.2 | 移除 | 刪除（不需替代） | 程式碼已全用 `jakarta.mail.*`，mail 由 `spring-boot-starter-mail` 提供 |
| `javax.servlet:javax.servlet-api` | 4.0.1 | 替換 | `jakarta.servlet:jakarta.servlet-api:6.1.0`（BOM） | 唯一使用者 `VelocityUtil.java:9`；scope 視情況設 provided |
| `org.apache.velocity:velocity` | 1.7 | 升版 | `velocity-engine-core:2.4.1` | artifact 改名；移除 commons-lang 2.x；`WebappLoader` 移到 velocity-tools |
| `org.logback-extensions:logback-ext-spring` | 0.1.5 | 移除 | 刪除（不需替代） | grep 確認 src 內無任何使用 |
| `net.sf.jasperreports:jasperreports` | 6.21.3 | 升版 | 7.0.x | 7.0 起支援 jakarta；exclusions 與 PDF 後端需重檢 |
| `com.lowagie:itext` | `[1.02b,2.1.7]` | 需查證 | 釘死 2.1.7 或隨 jasperreports 7.x | PDF 輸出後端，與 jakarta 無關 |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | 確認相容 | 4.12.0 或升 5.x | 不受 SB4 HttpClient5 變更影響 |
| `org.apache.poi:poi / poi-ooxml` | 5.2.5 | 確認相容 | 5.2.5 或 5.3.x | Jakarta 中性，低風險 |
| `commons-beanutils:commons-beanutils` | 1.11.0 | 確認相容 | 1.11.0 | `ExcelUtil` 用 `BeanComparator`，低風險 |
| commons-collections4 / commons-io / commons-lang3 / gson / aspectjweaver / logback | 混合 | 確認相容 | lang3/gson/aspectj/logback 交由 BOM；collections4/io 維持自宣告 | 純工具庫，低風險 |

#### 破壞性變更

- `VelocityUtil.java:9` 的 `import javax.servlet.http.HttpServletRequest` 改 `jakarta.servlet.http.HttpServletRequest`；第 218 行字串常數 `"javax.servlet.ServletContext"` 改 `"jakarta.servlet.ServletContext"`。
- velocity 1.7 → `velocity-engine-core` 2.4.x：artifactId 改名（groupId 仍 `org.apache.velocity`），移除 commons-lang 2.x / commons-collections 硬依賴。
- `VelocityUtil.initWebPath()` 用的 `org.apache.velocity.tools.view.servlet.WebappLoader` 不在 core，需引入 `velocity-tools-view`（且需 jakarta 版 servlet）或廢棄 web 樣板模式（**高風險不確定點**）。
- jasperreports 6.21.3（javax-only）→ 7.0.x（jakarta）：主版號跳躍，現有 exclusions（jackson、commons-beanutils、spring-context、commons-lang、com.lowagie:itext）座標可能失效需重檢。
- `com.sun.mail:javax.mail:1.6.2` 移除：若有下游誤用其 `javax.mail.*` 類別會編譯失敗（屬下游風險）。
- logback-ext-spring 移除：若下游有 `LogbackConfigurer` 載入設定會失效（本模組未使用）。

#### 需修改的程式碼檔案

- `base-spring-boot-starter/src/main/java/com/zipe/util/VelocityUtil.java`：第 9 行 import 改 jakarta；第 218 行字串常數改 jakarta；第 118 行 `webapp.resource.loader.class` 的 `WebappLoader` 確認對應類別或廢棄 `initWebPath`；Javadoc 內 `{@link javax.servlet.ServletContext}`（行 108、109、111、205）改 jakarta。
- `base-spring-boot-starter/src/main/java/com/zipe/util/doc/JasperReportUtil.java`：確認 jasperreports 7.x 的 `JasperCompileManager`/`JasperFillManager`/`JasperExportManager` 等 API；`exportReportToPdf` 的 PDF 後端若 7.x 不再內建 `com.lowagie:itext` 需調整。
- `base-spring-boot-starter/pom.xml`：移除 `com.sun.mail:javax.mail`、`logback-ext-spring`；`javax.servlet-api` 改 `jakarta.servlet-api`（版本交 BOM）；velocity 改 `velocity-engine-core`（視 initWebPath 決定是否加 velocity-tools）；jasperreports exclusions 重檢。
- `pom.xml`（根）：parent 升 4.0.0；移除 `mail.version`、`logback-ext-spring.version`、`javax.servlet-api.version` 三 property 及對應 dependencyManagement；velocity.version 改 `velocity-engine-core` 2.4.x；jasperreports.version 改 7.0.x；itext.version 釘死 2.1.7。

#### 升級步驟

- [ ] 確認 javax 殘留範圍：已盤點 — 僅 `VelocityUtil.java` 一處 javax.servlet（其餘 javax.crypto/naming/print/net.ssl 均為 JDK 內建，不需動）。
- [ ] 根 pom parent 升 4.0.0，java.version 保持 17。
- [ ] 清理多餘依賴：移除 `com.sun.mail:javax.mail` 與 logback-ext-spring，同步刪根 pom property 與 dependencyManagement。
- [ ] javax.servlet-api 換 jakarta.servlet-api（BOM 6.1.0），修改 `VelocityUtil.java`。
- [ ] velocity 升 `velocity-engine-core` 2.4.1，處理 `WebappLoader`（velocity-tools 或廢棄 initWebPath），確認移除 commons-lang 2.x 後不再 `NoClassDefFoundError`。
- [ ] jasperreports 升 7.0.x，重檢 exclusions 與 PDF 後端，itext 釘死 2.1.7。
- [ ] POI/commons/gson/okhttp 維持現狀，由整體建構驗證。
- [ ] `mvn -pl base-spring-boot-starter -am clean install` 驗證編譯與測試。
- [ ] 依 doc-sync 規則同步 `doc-site/docs/base-starter/` 與各 README，重建 llms.txt。

#### 驗證方式

- [ ] `mvn -pl base-spring-boot-starter -am clean install` 成功（AesUtilTest/DESedeUtilTest/Md5UtilTest/OkHttpUtilTest 全綠）。
- [ ] `dependency:tree` 確認已無 javax.servlet-api、com.sun.mail:javax.mail、logback-ext-spring、velocity(1.7)、commons-lang 2.x；確認 jakarta.servlet-api 6.1.0、velocity-engine-core 2.4.x、jasperreports 7.x 正確進場。
- [ ] `VelocityUtil` 執行期：`generateContent()` 渲染樣板成功；若保留 initWebPath，於 servlet 環境驗證 WebappLoader 與 jakarta ServletContext。
- [ ] `MailService` 四種寄送在 SB4 + `jakarta.mail`（angus-mail）下對 SMTP 發信成功。
- [ ] `JasperReportUtil.exportPdfFile` 以範例 .jrxml 產生 PDF 成功。
- [ ] starters_example 以 SB4 啟動，確認 `BaseAutoConfiguration` 註冊的 Bean 正常。
- [ ] 文件同步：doc-site 執行 `npm run build` 重新產生 llms.txt / llms-full.txt。

---

### 4.3 db-spring-boot-starter — 風險：中

#### 總結

db-starter 主要使用 Spring JDBC（`AbstractRoutingDataSource`、`NamedParameterJdbcDaoSupport`、`BeanPropertyRowMapper`、`JdbcTemplate`）、Spring ORM/JPA（`LocalContainerEntityManagerFactoryBean`、`HibernateJpaVendorAdapter`、`JpaTransactionManager`）與 Hibernate 常數（`AvailableSettings.HBM2DDL_AUTO`），這些 API 在 SF7/Hibernate 7 大多保留。原始碼本身無 javax.* 問題（`javax.sql.DataSource` 屬 Java SE，非 Jakarta EE），也無 HttpClient 4。衝擊集中三點：(1) Hibernate 7.1 後手動組裝 EntityManagerFactory 與 hbm2ddl 行為需驗證；(2) `NamedParameterJdbcDaoSupport`/`BeanPropertyRowMapper` 在 SF7 行為/棄用變化需確認；(3) p6spy 3.9.1、jt400 20.0.6 兩個非 BOM 套件需確認相容性。多資料來源動態切換機制（`@DS` / `AbstractRoutingDataSource` / ThreadLocal / `@Around @Order(-1)`）在 SF7 不受影響。

#### 依賴異動表

| 依賴 | 現況 | 動作 | 目標 | 備註 |
|---|---|---|---|---|
| `spring-boot-starter-parent`（根） | 3.5.14 | 升版 | 4.0.0 | 連動 data-jpa/jdbc/autoconfigure 至 SF7 + Hibernate 7.1 + HikariCP 7.0 |
| `spring-boot-starter-data-jpa` | BOM | 確認相容 | 4.0.0 (BOM) | Hibernate 6.6→7.1，驗證手動 EMF 路徑 |
| `spring-boot-starter-data-jdbc` | BOM | 確認相容 | 4.0.0 (BOM) | 未直接用 Repository API，衝擊低 |
| `spring-boot-starter-jdbc` | BOM | 確認相容 | 4.0.0 (BOM) | spring-jdbc 7 + HikariCP 7.0 |
| `com.zaxxer:HikariCP` | ~5.x (BOM) | 確認相容 | 7.0 (BOM) | 核心 setter 保留，需回歸測試連線池與 AS400 `VALUES 1` |
| `org.hibernate.orm:hibernate-core` | 6.6.x (BOM) | 確認相容 | 7.1 (BOM) | `HBM2DDL_AUTO` 常數仍在；預設行為變更需驗證 |
| `com.microsoft.sqlserver:mssql-jdbc` | ~12.x (BOM) | 確認相容 | 13.2 (BOM) | 確認 spy.properties driverlist 類別路徑不變 |
| `com.mysql:mysql-connector-j` | ~9.x (BOM) | 確認相容 | BOM（需查證版號） | 驅動屬性相容性高 |
| `org.mariadb.jdbc:mariadb-java-client` | ~3.x (BOM) | 確認相容 | BOM（需查證版號） | JDBC 層相容 |
| `p6spy:p6spy` | 3.9.1 | 確認相容 | 3.9.x 最新 | JDBC proxy，與 jakarta 無關；升級後實測 SQL 攔截日誌 |
| `net.sf.jt400:jt400` | 20.0.6 | 升版 | 21.0.6（需查證） | JDBC 4.2 驅動，運作於 java.sql/javax.sql 層；驅動類別路徑不變 |
| `io.github.a09090443:base-spring-boot-starter` | 3.5.14.1 | 升版 | 隨專案版本 | 先確認 base-starter 完成升級 |
| `org.projectlombok:lombok` | BOM | 確認相容 | BOM | `@Data`/`@Slf4j` |
| `com.h2database:h2`（test） | BOM | 確認相容 | BOM | 測試用 |

#### 破壞性變更

- Hibernate 6.6→7.1：`multiEntityManager()` 手動建立 `LocalContainerEntityManagerFactoryBean` + `HibernateJpaVendorAdapter`，Hibernate 7 的方言自動偵測、hbm2ddl 與部分 `AvailableSettings` 預設值有調整，需驗證 `additionalProperties()` 的 `none` 預設與 EMF 啟動行為。
- Hibernate 7 移除/調整部分 legacy 設定鍵：本模組僅用 `HBM2DDL_AUTO`（仍保留），風險集中於 JPA bootstrap 流程。
- HikariCP 5→7：核心 setter 保留，需確認 `baseHikariConfig()` 全部參數與 AS400 `createAs400DataSource()` 的 `VALUES 1` 測試查詢。
- mssql-jdbc 升 13.2：major 跳動，回歸測試 SQL Server 連線。
- SF7 對 `@ConfigurationProperties` 綁定更嚴格：`DataSourcePropertyConfig` 同時標 `@Configuration` + `@ConfigurationProperties` 並內部 `@Bean("dynamicDataSource")` 自我複製，建議驗證屬性綁定與 Bean 註冊順序無 regression。

#### 需修改的程式碼檔案

- `db-spring-boot-starter/.../autoconfiguration/DataSourceConfigAutoConfiguration.java`：重點驗證檔。`javax.sql.DataSource` 不需動；驗證 `HBM2DDL_AUTO`、手動組裝 EMF、`hbm2ddl=none` 預設、`HibernateProperties`（`org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties`）在 SB4 是否更名/移動套件。
- `db-spring-boot-starter/.../base/database/BaseDataSourceConfig.java`：HikariConfig setter 大量使用，HikariCP 7 保留，升級後以連線池啟動測試確認。
- `db-spring-boot-starter/.../jdbc/BaseJDBC.java`：`NamedParameterJdbcDaoSupport`/`JdbcTemplate`/`BeanPropertyRowMapper`/`MapSqlParameterSource`/`DataAccessException`。SF7 保留；`BeanPropertyRowMapper` 未棄用可暫不改。
- `db-spring-boot-starter/.../base/database/DynamicDataSource.java`：`AbstractRoutingDataSource` 保留；注意 SF7 JSpecify 標註下 `determineCurrentLookupKey()` 可回傳 null 的語意。
- `db-spring-boot-starter/.../base/aspect/DynamicDataSourceAspect.java`：`@Around @Order(-1)` 切面免改，回歸測試巢狀 `@DS` 與事務。
- `db-spring-boot-starter/.../base/config/P6SpyLogger.java`：`MessageFormattingStrategy` 在 3.9.x 穩定，免改。
- `db-spring-boot-starter/.../base/config/DataSourcePropertyConfig.java`：驗證 `dynamic.*` 綁定與 `dynamicDataSource` Bean 無 regression。
- `db-spring-boot-starter/src/main/resources/spy.properties`：確認三個驅動類別全限定名未變更。
- `db-spring-boot-starter/pom.xml`：座標無需改動（皆由根 pom 管理）。

#### 升級步驟

- [ ] 前置：先完成 base-starter SB4 升級。
- [ ] 根 pom parent 改 4.0.0，java.version 維持 17。
- [ ] 檢視第三方 property：p6spy.version（維持 3.9.1 或升 3.9.x 最新）、jt400.version（評估 20.0.6→21.0.6）。
- [ ] `mvn -pl db-spring-boot-starter -am clean compile`，優先處理編譯錯誤（重點 `HibernateProperties` 套件位置、`AvailableSettings`、`AbstractRoutingDataSource` 泛型/null-safety 警告）。
- [ ] 若 `HibernateProperties` import 路徑變更，更新 `DataSourceConfigAutoConfiguration`。
- [ ] 啟動模組測試（含 H2），驗證可建立 `DynamicDataSource`、EMF、`JpaTransactionManager`、`JdbcTemplate` 等 Bean。
- [ ] 真實 MySQL/MSSQL（及 AS400）冒煙測試：HikariCP 7 連線池、AS400 `VALUES 1`、`@DS` 切換、巢狀 `@DS` + `@Transactional`。
- [ ] 驗證 p6spy 攔截日誌正常輸出。
- [ ] 依 doc-sync 同步 `doc-site/docs/db-starter/` 與 README，重建 llms.txt。

#### 驗證方式

- [ ] `mvn -pl db-spring-boot-starter -am clean install` 編譯與單元測試全綠（含 ConditionsTest、BaseJDBCConditionsIntegrationTest）。
- [ ] starters_example 或測試 context 啟動，確認兩個 AutoConfiguration 生效，所有 Bean 建立成功。
- [ ] 整合測試：H2 與至少一個真實 DB 驗證 BaseJDBC 的 update / queryForBean / queryForList / queryForMap / 分頁 / Conditions 與升級前一致。
- [ ] 多資料來源切換：`@DS` 切換、巢狀還原、`@DS` + `@Transactional`（驗 `@Order(-1)` 早於事務），ThreadLocal 在執行緒池重用下無殘留。
- [ ] HikariCP 7 連線池冒煙：一般 DB（`SELECT 1`）與 AS400（`VALUES 1`）皆能取得連線。
- [ ] p6spy 日誌驗證。
- [ ] 若升 jt400 21.x，AS400 環境連線冒煙。

---

### 4.4 job-spring-boot-starter — 風險：中

#### 總結

job-starter 升級衝擊集中在「SB4 程式碼模組化造成的 import 套件搬移」，而非命名空間或 HttpClient。所有依賴（quartz、-web、-jdbc、h2）皆由 SB4 BOM 管理，pom.xml 無需補版本號；Quartz 由 BOM 升 2.5.1。唯一需改程式碼的是 `DataSourceAutoConfiguration.java`：`QuartzDataSource` 與 `DataSourceProperties` 兩個 import 套件路徑在 SB4 已改變。其餘核心類別（`QuartzJobBean`、所有 `org.quartz.*`、`jakarta.annotation.PostConstruct`）皆不受影響。整體屬「低程式碼改動、需精準對應套件名」的中等風險。

#### 依賴異動表

| 依賴 | 現況 | 動作 | 目標 | 備註 |
|---|---|---|---|---|
| `spring-boot-starter-quartz` | 3.5.14 (BOM, Quartz 2.3.x) | 確認相容（BOM 帶動） | 4.0.0 (Quartz 2.5.1) | pom 無版本號，隨 parent 升級 |
| `spring-boot-starter-web` | 3.5.14 (BOM) | 確認相容 | 4.0.0 (BOM) | 僅用 `@RestController`/`@PostMapping`/`ResponseEntity`，穩定 API |
| `spring-boot-starter-jdbc` | 3.5.14 (BOM) | 確認相容 | 4.0.0 (BOM) | HikariCP 隨 BOM |
| `com.h2database:h2` | 3.5.14 (BOM, provided) | 確認相容 | 4.0.0 (BOM) | JDBC JobStore 測試用 |
| `io.github.a09090443:base-spring-boot-starter` | 3.5.14.1 | 升版 | 隨專案版本 | 用 `DateTimeUtils`，先確認 base-starter 升級 |
| `org.projectlombok:lombok` | BOM (optional) | 確認相容 | BOM | annotation processing 確認 |
| `org.junit.jupiter:junit-jupiter` | BOM (test) | 確認相容 | BOM | JUnit 5 |

#### 破壞性變更

- SB4 模組化：`org.springframework.boot.autoconfigure.quartz.QuartzDataSource` → `org.springframework.boot.quartz.autoconfigure.QuartzDataSource`（型別不變、套件路徑變）。影響 `DataSourceAutoConfiguration.java` 第 10 行 import 與第 96 行 `@QuartzDataSource`。
- SB4 模組化：`org.springframework.boot.autoconfigure.jdbc.DataSourceProperties` → `org.springframework.boot.jdbc.autoconfigure.DataSourceProperties`。影響第 9 行 import 與第 53、57、99 行使用。
- 非破壞：`org.springframework.scheduling.quartz.QuartzJobBean`（spring-context-support）套件不變，`QuartzJobFactory.java:7` 免改。
- 非破壞：所有 `org.quartz.*` API 在 Quartz 2.5.1 維持相容。
- 非破壞：`javax.sql.DataSource`（第 18 行）為 JDK 標準，不需遷移。

#### 需修改的程式碼檔案

- `job-spring-boot-starter/.../quartz/autoconfiguration/DataSourceAutoConfiguration.java`：第 10 行 import 改 `org.springframework.boot.quartz.autoconfigure.QuartzDataSource`；第 9 行改 `org.springframework.boot.jdbc.autoconfigure.DataSourceProperties`；第 25-26 行 Javadoc `{@link}` 全名更新。`@QuartzDataSource`、`@Primary`、`@Bean`、`@ConfigurationProperties` 用法不變。

#### 升級步驟

- [ ] 前置：根 pom 升 4.0.0，且 base-starter 完成升級可在本地 repository 取得。
- [ ] （可選）過渡：暫加 `spring-boot-starter-classic` 讓舊套件路徑可解析先通過編譯，再逐一修正 import；穩定後移除。
- [ ] 修正 `DataSourceAutoConfiguration.java` 兩個 import 與 Javadoc 全名連結。
- [ ] `mvn -pl job-spring-boot-starter -am clean install`，確認無 package not found。
- [ ] 執行單元測試（QuartzJobUtilTest、QuartzJobPropertyConfigTest）確認 Quartz 2.5.1 下行為一致。
- [ ] 依 doc-sync 檢查 `doc-site/docs/job-starter/` 與 README。

#### 驗證方式

- [ ] `mvn -pl job-spring-boot-starter -am clean install` 建構成功。
- [ ] `dependency:tree` 確認 quartz 解析至 4.0.0、Quartz 2.5.1，-web/-jdbc/h2 由 BOM 正確帶入。
- [ ] 單元測試全綠：QuartzJobUtilTest（白名單載入 / RCE 防護 / 非 Job 型別拒絕）、QuartzJobPropertyConfigTest。
- [ ] 整合啟動：`spring.quartz.enable=true` 確認 `InitialJobAutoConfiguration` 由 quartz-jobs.properties 建立排程；`spring.quartz.job-store-type=jdbc` 確認 `@QuartzDataSource` Bean 建立 HikariDataSource。
- [ ] REST API：`quartz.controller.enabled=true` 後呼叫 `/quartz/register`、`/pause`、`/resume`、`/run`、`/delete` 確認回應 200。

---

### 4.5 logon-spring-boot-starter — 風險：中

#### 總結

整體風險中。Security DSL 已是 SS7 推薦的 `SecurityFilterChain` + `HttpSecurity` lambda 寫法，且 Servlet API 早已遷移 jakarta.*，無大規模重寫風險。主要衝擊三處：(1) `SecurityConfiguration.java` BASIC 模式 `new DaoAuthenticationProvider()` 無參數建構子 + `setUserDetailsService()` 在 SS7 已棄用，需改建構子注入；(2) `requestMatchers` 在 SS7 預設改 `PathPatternRequestMatcher`，需確認 allow-uris 的 Ant 樣式仍可解析；(3) 依賴版本由根 pom BOM 統一管理。LDAP 走 JDK 原生 JNDI（`javax.naming.*`，屬 java.naming 模組，非 Jakarta EE），不受遷移影響。`SecurityInitializer` 繼承 `AbstractSecurityWebApplicationInitializer` 在 SS7 仍保留但僅適用傳統 WAR 部署。

#### 依賴異動表

| 依賴 | 現況 | 動作 | 目標 | 備註 |
|---|---|---|---|---|
| `spring-boot-starter-parent`（根） | 3.5.14 | 升版 | 4.0.0 | 連動 security→SS7、web→SF7 |
| `spring-boot-starter-security` | BOM (SS6.x) | 確認相容 | SS7.0.x (BOM) | `DaoAuthenticationProvider`、`requestMatchers` API 變更 |
| `spring-boot-starter-web` | BOM | 確認相容 | BOM (SF7) | jakarta.servlet 已使用 |
| `spring-boot-autoconfigure` | BOM | 確認相容 | BOM | `AutoConfiguration.imports` 機制維持 |
| `spring-security-test` | BOM (test) | 確認相容 | BOM | 隨 SS7，重跑測試 |
| `org.projectlombok:lombok` | BOM | 確認相容 | 需查證 | `@Slf4j`/`@Data`/`@SneakyThrows` |
| `io.github.a09090443:base-spring-boot-starter` | 3.5.14.1 | 升版 | 4.0.0.x | 先確保 base-starter 升級 |
| `org.apache.commons:commons-lang3` | BOM | 確認相容 | BOM | 間接依賴 |

#### 破壞性變更

- `DaoAuthenticationProvider` 無參數建構子在 SS7 已棄用（spring-security#15973），推薦 `new DaoAuthenticationProvider(userDetailsService)` 再 `setPasswordEncoder()`。`authenticationProvider()` BASIC 分支會出現棄用警告甚至編譯失敗。
- SS7 路徑比對預設改 `PathPatternRequestMatcher`，不再支援 `AntPathRequestMatcher`/`MvcRequestMatcher`，且要求 URI 為絕對路徑（不含 context root）。`requestMatchers(switchSecurity())` 傳入的 allow-uris 若含 Ant 樣式需確認語意一致。
- `HeadersConfigurer.FrameOptionsConfig` 的 `and()` 在 7.0 移除；本模組已用 `frameOptions(Customizer)` lambda，不受影響。
- javax.servlet→jakarta.servlet：本模組已完成（handler/service 皆 import `jakarta.servlet.*`）。
- `LdapUserDetailsService` 用的 `javax.naming.*` 屬 JDK java.naming，非 Jakarta EE，不需改。

#### 需修改的程式碼檔案

- `logon-spring-boot-starter/.../autoconfiguration/SecurityConfiguration.java`：BASIC/default 分支（約 187-190 行）改建構子注入 `new DaoAuthenticationProvider(basicUserServiceImpl())`，保留 `setPasswordEncoder()`；驗證 `requestMatchers(switchSecurity()).permitAll()` 在 `PathPatternRequestMatcher` 下行為，必要時調整樣式。其餘 DSL 為 SS7 相容寫法，預期免改。
- `logon-spring-boot-starter/.../config/SecurityInitializer.java`：無需修改；確認文件說明與部署方式一致。
- `logon-spring-boot-starter/.../service/LdapUserDetailsService.java`：無需修改（javax.naming 為 JDK 模組）。
- `logon-spring-boot-starter/.../service/CommonLoginProcess.java`：無需修改；確認 `UsernamePasswordAuthenticationToken` 三參數建構子行為。
- `logon-spring-boot-starter/.../service/BasicUserServiceImpl.java`：無需修改。
- `logon-spring-boot-starter/.../handler/LoginSuccessHandler.java`、`LoginFailureHandler.java`、`LogoutSuccessHandler.java`：無需修改（已用 jakarta.servlet）。
- `logon-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：無需修改。

#### 升級步驟

- [ ] 先完成 base-starter SB4 升級。
- [ ] 根 pom parent 升 4.0.0，保留 java.version=17。
- [ ] `mvn -pl logon-spring-boot-starter -am clean compile`，收集 SS7 棄用警告與編譯錯誤。
- [ ] 修改 `SecurityConfiguration.java`：BASIC 分支改建構子注入。
- [ ] 檢視 requestMatchers 與 allow-uris 在 `PathPatternRequestMatcher` 下相容性，必要時調整。
- [ ] 重新編譯確認無棄用警告，執行單元測試（SecurityPropertyConfigTest、LdapUserDetailsServiceTest）。
- [ ] starters_example 啟動驗證三種模式（BASIC/LDAP/CUSTOM）登入、登出、Session 上限、X-Frame-Options。
- [ ] 依 doc-sync 同步 `doc-site/docs/logon-starter/` 與 README。

#### 驗證方式

- [ ] `mvn -pl logon-spring-boot-starter -am clean install` 成功且無 SS 棄用警告殘留（特別針對 `DaoAuthenticationProvider`）。
- [ ] SecurityPropertyConfigTest、LdapUserDetailsServiceTest 全數通過。
- [ ] `verification-type=BASIC`：admin（含 CommonLoginProcess 當日動態密碼）登入成功、未授權路徑導向登入頁。
- [ ] `verification-type=LDAP`：`LdapUserDetailsService` 走 JNDI 驗證成功並正確移除域名。
- [ ] `verification-type=CUSTOM`：自訂 AuthenticationProvider 套用，未設定時拋出預期例外。
- [ ] X-Frame-Options 標頭依 frame-options-mode（SAMEORIGIN/DENY/DISABLE）正確輸出。
- [ ] `maximumSessions(2)` 上限、登出後 JSESSIONID 清除與重導向。
- [ ] `PathPatternRequestMatcher` 預設下 allow-uris 各路徑 permitAll 行為與升級前一致。

---

### 4.6 web-spring-boot-starter — 風險：中

#### 總結

最大風險集中在「SB4 模組化造成的套件位移」，而非命名空間或第三方相容性。經查證確認：(1) JSP/JSTL 已用 jakarta、本模組無 javax.* 殘留，且程式碼/JSP 皆未使用 JSTL taglib，JSP 面向幾乎零衝擊；(2) **關鍵利多**——SB4 thymeleaf 仍相依 `thymeleaf-spring6:3.1.3.RELEASE`（無 `thymeleaf-spring7`），故 `ViewResolverAutoConfiguration` 內 `org.thymeleaf.spring6.*` import 依然有效不需改；(3) `@ResponseResultBody` 機制屬 SF7 穩定 API。真正會編譯失敗的是 `TomcatAutoConfiguration` 與 `ViewResolverAutoConfiguration` 內三個被搬家或移除的型別：`ConfigurableServletWebServerFactory`、`TomcatServletWebServerFactory`、`ServletWebServerFactoryAutoConfiguration`。

#### 依賴異動表

| 依賴 | 現況 | 動作 | 目標 | 備註 |
|---|---|---|---|---|
| `spring-boot-starter-web` | 3.5.14 (BOM) | 升版並建議改名 | 4.0.0；建議 `spring-boot-starter-webmvc` | SB4 標 deprecated；仍透過 tomcat starter 帶入 Tomcat，短期可不換名 |
| `spring-boot-starter-thymeleaf` | 3.5.14 (BOM) | 升版（確認相容） | 4.0.0 | 改依 `spring-boot-thymeleaf:4.0.0` → `thymeleaf-spring6:3.1.3.RELEASE`，程式無需改 |
| `spring-boot-autoconfigure` | 3.5.14 (BOM) | 升版 | 4.0.0 | 部分 web server 配置移至 spring-boot-web-server / spring-boot-tomcat |
| `org.apache.tomcat.embed:tomcat-embed-jasper` | BOM (provided) | 確認相容 | SB4 BOM (Tomcat 11) | 驗證 JSP 編譯在 Tomcat 11 embedded |
| `jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api` | BOM | 確認相容 | SB4 BOM | 已是 jakarta |
| `org.glassfish.web:jakarta.servlet.jsp.jstl` | BOM | 確認相容 | SB4 BOM（需查證） | 若 BOM 移除管理，需補明確版本 |
| `io.github.a09090443:base-spring-boot-starter` | 3.5.14.1 | 升版 | 隨專案版本 | 內部相依 |
| `org.projectlombok:lombok` | BOM | 確認相容 | SB4 BOM | `@Data`/`@Slf4j` |

#### 破壞性變更

- 套件位移（編譯錯誤）：`ConfigurableServletWebServerFactory` 由 `org.springframework.boot.web.servlet.server` 移至 `org.springframework.boot.web.server.servlet`。影響 `ViewResolverAutoConfiguration.java:11`。
- 套件位移（編譯錯誤）：`TomcatServletWebServerFactory` 由 `org.springframework.boot.web.embedded.tomcat` 移至 `org.springframework.boot.tomcat.servlet`。影響 `TomcatAutoConfiguration.java:9` 與第 37、45 行。
- 類別移除/改名（編譯錯誤）：`ServletWebServerFactoryAutoConfiguration` 在 SB4 不存在，改為 `org.springframework.boot.web.server.autoconfigure.servlet.ServletWebServerConfiguration`。影響 `TomcatAutoConfiguration.java:8` 與第 24 行 `@AutoConfigureBefore`。
- `WebServerFactoryCustomizer` 套件未變（仍 `org.springframework.boot.web.server`），但其泛型參數 `ConfigurableServletWebServerFactory` 已位移。
- `spring-boot-starter-web` 標 deprecated（建議 `spring-boot-starter-webmvc`）。
- 非破壞：`@ResponseResultBody`/`ResponseResultBodyAdvice` 用的 SF7 API 穩定。
- 非破壞（已排除）：Thymeleaf 仍用 spring6，`org.thymeleaf.spring6.*` 不需改。

#### 需修改的程式碼檔案

- `web-spring-boot-starter/.../autoconfiguration/TomcatAutoConfiguration.java`：(1) 第 8 行 → `org.springframework.boot.web.server.autoconfigure.servlet.ServletWebServerConfiguration`；(2) 第 9 行 → `org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory`；(3) 第 24 行 `@AutoConfigureBefore(ServletWebServerConfiguration.class)`。第 45 行 `extends` 與第 58 行 `postProcessContext` 不變。
- `web-spring-boot-starter/.../autoconfiguration/ViewResolverAutoConfiguration.java`：第 11 行 → `org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory`。第 10 行 `WebServerFactoryCustomizer`、第 17-19 行 `org.thymeleaf.spring6.*`、第 155 行 `setRegisterDefaultServlet(true)`、第 172 行 `CookieLocaleResolver` 皆不需改。
- `web-spring-boot-starter/.../autoconfiguration/WebAutoConfiguration.java`：無強制改動；驗證 `configureDefaultServletHandling`/`addResourceHandlers` 簽章是否有 JSpecify 警告。
- `web-spring-boot-starter/.../advice/ResponseResultBodyAdvice.java`：無強制改動；驗證 `@ResponseResultBody` 對 String/物件/例外三路徑包裝一致。
- `web-spring-boot-starter/.../base/controller/BaseController.java`：無強制改動（已用 jakarta / SF7 API）。
- `web-spring-boot-starter/pom.xml`：無座標寫死需改；建議評估改 `spring-boot-starter-webmvc`。
- `web-spring-boot-starter/src/main/webapp/WEB-INF/jsp/(hello.jsp,test.jsp)`：無需改（僅 `<%@ page %>` 與 EL，未用 JSTL taglib）。

#### 升級步驟

- [ ] 確認根 pom parent 升 4.0.0、java.version 17。
- [ ] 修改 `TomcatAutoConfiguration.java`：更新 `ServletWebServerFactoryAutoConfiguration → ServletWebServerConfiguration`（import 與 `@AutoConfigureBefore`）與 `TomcatServletWebServerFactory` 套件。
- [ ] 修改 `ViewResolverAutoConfiguration.java`：更新 `ConfigurableServletWebServerFactory` 套件（thymeleaf.spring6 / WebServerFactoryCustomizer 不動）。
- [ ] `mvn -pl web-spring-boot-starter -am clean compile`，逐一補齊其餘位移 import。
- [ ] （可選）評估 `spring-boot-starter-web → spring-boot-starter-webmvc`，確認 Tomcat 仍在 classpath。
- [ ] 確認 JSTL 兩座標仍由 SB4 BOM 管理；若 `org.glassfish.web:jakarta.servlet.jsp.jstl` 已不管理則補版本。
- [ ] starters_example 以 `web.jsp.enable=true`、`web.thymeleaf.enable=true` 啟動，驗證 /jsp、/thymeleaf、/rest 系列。
- [ ] 依 doc-sync 檢視 `doc-site/docs/web-starter/`、README、llms.txt。

#### 驗證方式

- [ ] `mvn -pl web-spring-boot-starter -am clean install` 成功，無 ClassNotFound / package does not exist。
- [ ] `GET /web/jsp` 渲染 hello.jsp；`GET /web/thymeleaf` 渲染 hello.html。
- [ ] `GET /web/rest/hello`、`/rest/result`、`/rest/testString`、`/rest/testInt`、`/rest/testUser` 回傳統一 Result JSON。
- [ ] `GET /web/rest/helloError`、`/rest/helloMyError` 由 ResponseResultBodyAdvice 攔截，回傳 500 與 ResultException 對應狀態。
- [ ] 靜態資源 `/web/static/**` 可存取。
- [ ] `?language=en` 驗證 LocaleChangeInterceptor 與 CookieLocaleResolver、Cookie localeCookie 寫入。
- [ ] `dependency:tree` 確認 thymeleaf-spring6、tomcat-embed-jasper、jakarta JSTL 版本符合 SB4 BOM，無 javax.* 殘留。
- [ ] 啟動日誌確認 `TomcatAutoConfiguration` 自訂 factory 生效（JAR Manifest 掃描已停用）。

---

### 4.7 web-service-spring-boot-starter — 風險：高

#### 總結

全專案升級 SB4 的最高風險模組。核心衝擊三點：(1) Apache CXF 必須從 4.0.0 跨版本升 4.2.x（4.2.0 於 2026-02 GA），因只有 4.2.x 才支援 SF7/SB4/Jakarta EE 11、SS7、Hibernate 7.2、Jackson 3.0，現用 4.0.0 僅對應 Jakarta EE 9.1 無法在 SB4 運作；(2) `SoapUtil.java` 直接以 HttpClient 4.x（`org.apache.http.*`）撰寫，SB4 BOM 自 3.1 起即移除 HttpClient 4 改管 HttpClient 5，須改為 httpclient5 且全檔 import 與 API 重寫；(3) jaxws-ri 4.0.1 與 cxf-rt-databinding-jaxb 須隨 CXF 4.2 對齊 Jakarta EE 11。CXF 業務碼（攔截器、EndpointImpl、JAXBDataBinding、JaxWsDynamicClientFactory）API 大致穩定。註：原始碼 `javax.xml.*` 屬 JDK java.xml 模組非 Jakarta EE，無須遷移；模組內 `jakarta.jws`/`jakarta.xml.bind` 已是 jakarta。

#### 依賴異動表

| 依賴 | 現況 | 動作 | 目標 | 備註 |
|---|---|---|---|---|
| `org.apache.cxf:cxf-spring-boot-starter-jaxws` | 4.0.0 | 升版（跨次版本 4.0→4.2） | 4.2.x（建議 4.2.1 或最新 patch） | 4.2.x 才支援 SF7/SB4；4.0.x 僅 Jakarta EE 9.1 |
| `org.apache.cxf:cxf-rt-databinding-jaxb` | 4.0.0 | 升版（須與 starter 同版） | 4.2.x | 各 rt 模組版本必須一致；建議合併單一 `cxf.version` |
| `org.apache.httpcomponents:httpclient`（HC4） | 4.5.14 | 替換為 HC5 | `httpcomponents.client5:httpclient5`（BOM） | SB4 僅管 HC5；SoapUtil 需配合改寫 |
| `com.sun.xml.ws:jaxws-ri`（type=pom） | 4.0.1 | 需查證升版 | 對齊 Jakarta EE 11 / CXF 4.2（或移除改由 CXF 傳遞） | 升級後查 `dependency:tree` 是否版本重複 |
| `spring-boot-autoconfigure` / `configuration-processor` | parent 3.5.14 | 升版 | 4.0.0 | `ServletRegistrationBean` 套件可能位移 |
| `io.github.a09090443:base-spring-boot-starter` | 3.5.14.1 | 確認相容 | 4.0.0.x | 等 base-starter 完成 |

#### 破壞性變更

- CXF 4.0.0 → 4.2.x：4.2 改以 Jakarta EE 11 為基準，與現用 4.0.0（EE 9.1）不相容；混用會啟動時 `NoClassDefFoundError`/`NoSuchMethodError`。
- `SoapUtil.java` 直接依賴 HC4 API（`org.apache.http.client.methods.CloseableHttpResponse`/`HttpPost`、`entity.ContentType`/`StringEntity`、`impl.client.CloseableHttpClient`/`HttpClients`、`util.EntityUtils`）全部改寫為 HC5（`org.apache.hc.client5.http.*` 與 `org.apache.hc.core5.http.*`）。
- HC5 API 行為變更：`StringEntity` 改 `new StringEntity(xml, ContentType.APPLICATION_XML)`（ContentType 移至 `org.apache.hc.core5.http`）；`CloseableHttpResponse` 改 `ClassicHttpResponse` 且建議用 `execute(post, responseHandler)`；`EntityUtils.toString` 移至 `org.apache.hc.core5.http.io.entity.EntityUtils` 且宣告 `ParseException`。
- SB BOM 不再提供 HC4 版本管理（自 3.1 起）。
- jaxws-ri 與 cxf-rt-databinding-jaxb 若未與 CXF 4.2 對齊將造成 JAXB/JAX-WS RI 版本衝突。

#### 需修改的程式碼檔案

- `web-service-spring-boot-starter/.../util/SoapUtil.java`：**唯一需改碼的檔案**。第 3-9 行 `org.apache.http.*` import 全部換 HC5：`org.apache.hc.client5.http.classic.methods.HttpPost`、`org.apache.hc.client5.http.impl.classic.CloseableHttpClient`/`HttpClients`、`org.apache.hc.core5.http.io.entity.StringEntity`/`EntityUtils`、`org.apache.hc.core5.http.ContentType`/`ClassicHttpResponse`。`doPostWithXml()`（第 145-173 行）改寫：`CloseableHttpResponse` 改 `ClassicHttpResponse` 或改用 `execute(httpPost, HttpClientResponseHandler)` lambda；`EntityUtils.toString` 處理 `ParseException`。第 15-27 行 javax.xml.* 與 java.io.* 不需更動。
- `web-service-spring-boot-starter/.../util/WebServiceClientUtil.java`：預期無需改碼；實測 `JaxWsDynamicClientFactory` 動態客戶端建立。
- `web-service-spring-boot-starter/.../util/ClientLoginInterceptor.java`：預期無需改碼（`javax.xml.namespace.QName` 屬 JDK）；實測 SOAP Header 注入。
- `web-service-spring-boot-starter/.../interceptor/CdataContentInterceptor.java`：預期無需改碼；確認 `org.apache.cxf.helpers.IOUtils` 在 4.2 仍存在。
- `web-service-spring-boot-starter/.../interceptor/ResponseCdataInterceptor.java`：預期無需改碼；實測回應 CDATA 還原。
- `web-service-spring-boot-starter/.../autoconfiguration/WebServiceRegisterAutoConfiguration.java`：預期無需改碼；驗證 `JAXBDataBinding` 的 `setUnwrapJAXBElement`/`setMtomEnabled`/`setNamespaceMap` 在 4.2 行為一致。
- `web-service-spring-boot-starter/.../autoconfiguration/CxfConfigAutoConfiguration.java`：須驗證 `org.springframework.boot.web.servlet.ServletRegistrationBean` 套件在 SB4 是否移至新模組；若變更則改 import。
- `web-service-spring-boot-starter/.../adapt/CdataAdapter.java`、`model/User.java`、`service/UserService.java`、`service/impl/UserServiceImpl.java`：無需改碼（已用 jakarta），確認 CXF 4.2 提供對應 API 版本。
- `pom.xml`（根）：cxf 兩 property 升 4.2.x（建議合併 `cxf.version`）；移除 `httpclient.version`、改 `httpclient5`（或交由 BOM）；檢視 `jaxws-ri.version`。
- `web-service-spring-boot-starter/pom.xml`：第 59-62 行 `httpclient` 改 `httpclient5`；CXF 座標不變；確認 jaxws-ri 是否可移除。

#### 升級步驟

- [ ] 確保根 pom parent 升 4.0.0、base-starter 完成升級。
- [ ] 查 Maven Central 確認 cxf-spring-boot-starter-jaxws 最新 4.2.x patch 版號。
- [ ] 根 pom 將 cxf 兩 property 同步改 4.2.x（建議重構單一 `cxf.version`）。
- [ ] 根 pom 移除 `httpclient.version`，dependencyManagement 改 `httpclient5`（或移除交 BOM）。
- [ ] `web-service-spring-boot-starter/pom.xml` 的 httpclient 座標改 httpclient5。
- [ ] 改寫 `SoapUtil.java`：替換 import、調整 `StringEntity`/`ClassicHttpResponse`/`EntityUtils.toString`（建議 `execute` + `HttpClientResponseHandler`），處理 `ParseException`。
- [ ] `mvn -pl web-service-spring-boot-starter -am clean install`，逐一修正（重點 `ServletRegistrationBean` 套件、CXF helper、JAX-WS RI 衝突）。
- [ ] `dependency:tree` 檢查 jaxws-ri / jakarta.xml.bind / com.sun.xml 版本重複；必要時調整或移除 jaxws-ri。
- [ ] 依 doc-sync 同步 `doc-site/docs/web-service-starter/`（quickstart httpclient5、CXF 4.2；examples SoapUtil；architecture），重建 llms.txt。

#### 驗證方式

- [ ] `mvn -pl web-service-spring-boot-starter -am clean install` 編譯與單元測試（ClientTest、XxeSecurityTest）通過。
- [ ] `dependency:tree` 確認 CXF 全為 4.2.x、無 HC4 殘留、jakarta.xml.bind / JAX-WS RI 無衝突。
- [ ] 啟動 starters_example 或本模組，確認 `CXFServlet` 註冊、`WebServiceRegisterAutoConfiguration` 將 `@WebService` 端點發布無例外。
- [ ] `SoapUtil.doPostWithXml` 對實際 SOAP 端點發送並取回回應字串（HC5 路徑）。
- [ ] 驗證 `CdataContentInterceptor`（入站）與 `ResponseCdataInterceptor`（出站）CDATA 還原。
- [ ] `WebServiceClientUtil.invoke(user, pwd)` 動態客戶端搭配 `ClientLoginInterceptor` 注入 SecurityHeader 正常呼叫。
- [ ] 重跑 XxeSecurityTest 確認 XXE 防護未失效。

---

### 4.8 keycloak-spring-boot-starter — 風險：高

#### 總結

整個專案中最特殊、風險最高的模組：**不在 reactor 內**（根 pom 第 48-53 行未包含它），且 pom.xml 仍 parent 至 `spring-boot-starter-parent 2.4.4`、java.version=11、Keycloak 13.0.1，與專案 3.5.14 基線完全脫節，等同被凍結、未跟上 SB3 遷移的孤兒模組。它採 thomasdarimont「embedded-spring-boot-keycloak-server」做法，直接在 JVM 內嵌 Keycloak 內部 SPI（`keycloak-services`/`keycloak-server-spi`/`keycloak-model-infinispan`），透過 RESTEasy 3（Classic）+ Undertow + Infinispan 11 + JGroups 4 手工拼裝伺服器。此架構在 Keycloak 17（2022）起隨 WildFly 發行版棄用並移除，現代 Keycloak（26.x）只剩 Quarkus 發行版、改 RESTEasy Reactive、移除 `javax.ws.rs.core.Context` 注入點、全面 jakarta 化——本模組依賴的 `KeycloakApplication`、`AbstractRequestFilter`、`Platform`/`PlatformProvider`、`HttpServlet30Dispatcher`、`ResteasyProviderFactory` 等內部 API 已不存在或語意完全改變。**這不是換命名空間就能升級的模組；相容 SB4 等於要把整個嵌入式伺服器重寫，且上游已無對應可嵌入 artifact。**

> **強烈建議**：將此模組從 SB4 升級範圍「整批排除/延後」，改以獨立的 standalone Keycloak（Quarkus 容器）+ 標準 OAuth2/OIDC client（`spring-boot-starter-oauth2-client` / `oauth2-resource-server`）取代，而非原地升級。

#### 依賴異動表

| 依賴 | 現況 | 動作 | 目標 | 備註 |
|---|---|---|---|---|
| `spring-boot-starter-parent`（parent） | 2.4.4 | 升版 | 4.0.0 | 跨四個大版本，含 javax→jakarta、spring.factories→imports、Security 6→7 |
| `java.version` | 11 | 升版 | 17 | SB4 baseline；Keycloak 13 未針對 17 測試 |
| `org.keycloak:keycloak-services` 等一整組 | 13.0.1 | 替換 | 無可嵌入版本（26.x Quarkus-only） | 嵌入式路線被移除，應放棄改 standalone + oauth2 client |
| `org.jboss.resteasy:resteasy-*` | 3.15.1.Final | 替換 | RESTEasy 6.x（jakarta） | Classic 內部 API 在 Reactive/6.x 不存在，等同重寫 |
| `org.infinispan:infinispan-core` | 11.0.9.Final | 升版 | 15.x（需查證） | API 變動，須配合 Keycloak/JGroups |
| `org.jgroups:jgroups` | 4.2.11.Final | 升版 | 5.x（需查證） | Infinispan 15 需 JGroups 5.x |
| `io.smallrye:smallrye-metrics` | 2.4.6 | 替換 | Micrometer | 嵌入式廢除後失去意義 |
| `com.google.auto.service:auto-service` | 1.1.1 | 確認相容 | 1.1.1 | annotation processor；但其註冊的 SPI 目標型別隨 Keycloak 廢除失效 |
| `spring-boot-starter-undertow` | parent 2.4.4 | 確認相容 | 4.0.0 | 嵌入式廢除後無存在價值 |
| `com.h2database:h2` | parent | 確認相容 | parent | 目前未標 test scope（pom 256 行被註解），應縮回 test |
| `com.zipe:base-spring-boot-starter` | 2.4.4.1 | 升版 | 對齊 reactor 版本 | 目前透過 GitHub raw repo 拉舊版，須改對齊 reactor |
| `javax.annotation:javax.annotation-api` | SB2.4 提供 | 替換 | jakarta.annotation-api 3.x | `DynamicJndiContextFactoryBuilder` 用 PostConstruct |

#### 破壞性變更

- 命名空間 javax→jakarta：`KeycloakUndertowRequestFilter`、`EmbeddedKeycloakApplication`、`EmbeddedKeycloakAutoConfiguration`（含字串常數 `"javax.ws.rs.Application"`）、`DynamicJndiContextFactoryBuilder`、`InfinispanCacheManagerProvider`、`KeycloakInitialContext` 全部需改 jakarta.*。
- spring.factories 舊格式失效：`EnableAutoConfiguration` 那段須遷至 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`；`ApplicationListener` 那段可續留 spring.factories。
- SF7/SB4 底層 servlet 型別改 jakarta：`WebApplicationContextUtils.getRequiredWebApplicationContext(ServletContext)` 之 `ServletContext` 變 `jakarta.servlet.ServletContext`。
- Keycloak 嵌入式架構整體被上游移除：`KeycloakApplication`、`AbstractRequestFilter`、`Platform`/`PlatformProvider`、`ApplianceBootstrap`、`ExportImportManager`、`ManagedCacheManagerProvider` 等內部 SPI 在 26.x 不提供，六個核心類別將整批編譯失敗且無替代 API。
- RESTEasy Classic→Reactive：`HttpServlet30Dispatcher`、`ResteasyProviderFactory`、`Dispatcher.getDefaultContextObjects()`、`ResteasyContextParameters` 均為 Classic 內部 API。
- `@Context` 注入移除：`EmbeddedKeycloakApplication(@Context ServletContext)` 注入方式新版不再支援。
- Infinispan / JGroups API 變動：`new ParserRegistry().parse(URL)`、`new DefaultCacheManager(holder, false)` 在 Infinispan 15 建構子調整；infinispan.xml schema 可能需更新。
- GitHub raw Maven repo 依賴（pom 38-48 行）拉 base-starter 2.4.4.1（javax 年代），須切回 reactor jakarta 版。
- `<finalName>job-spring-boot-starter</finalName>`（pom 293 行）為複製貼上殘留，應一併修正。

#### 需修改的程式碼檔案

- `keycloak-spring-boot-starter/pom.xml`：parent 改 4.0.0（或 inherit reactor 根 pom）；java.version 11→17；移除 raw.github.com repositories；base-starter 對齊 reactor；finalName 修正。**核心問題是 Keycloak 13 整組依賴在 SB4/Jakarta 世界無相容替代，無法只靠改版號完成。**
- `keycloak-spring-boot-starter/src/main/resources/META-INF/spring.factories`：`EnableAutoConfiguration=EmbeddedKeycloakAutoConfiguration` 遷至 `AutoConfiguration.imports`；ApplicationListener 段可續留。
- `.../support/KeycloakUndertowRequestFilter.java`：javax.servlet→jakarta；父類 `AbstractRequestFilter` 不存在，需改寫或刪除。
- `.../EmbeddedKeycloakApplication.java`：javax→jakarta；`@Context` 注入已移除需改寫；`KeycloakApplication`/`ApplianceBootstrap`/`ExportImportManager` 等不可用，整類需重新設計或廢除。
- `.../autoconfiguration/EmbeddedKeycloakAutoConfiguration.java`：javax→jakarta；字串 `"javax.ws.rs.Application"`→`"jakarta.ws.rs.Application"`；`HttpServlet30Dispatcher`/`ResteasyContextParameters` 無 Reactive 版，兩個 Bean 需重寫；Infinispan 建構 API 調整。
- `.../support/Resteasy3Provider.java`：依賴 RESTEasy 3 Classic + `@AutoService` 註冊 SPI，新世代無此 API，須刪除或重寫。
- `.../support/SpringBootPlatformProvider.java`：依賴 `PlatformProvider` SPI，現代不提供，需廢除。
- `.../support/DynamicJndiContextFactoryBuilder.java`：`javax.annotation.PostConstruct`→jakarta；`javax.naming.*` 為 JDK 不需改命名空間；`javax.sql.DataSource` 維持 javax.sql（屬 java.sql，不在 jakarta 範圍）。
- `.../support/InfinispanCacheManagerProvider.java`：依賴 `ManagedCacheManagerProvider` SPI 與 `javax.naming.NamingException`，需查證新版是否存在。
- `.../support/KeycloakInitialContext.java`：`javax.naming.*` 屬 JDK 不變；用途隨架構廢除失效。
- `.../support/SpringBootConfigProvider.java`：依賴 `org.keycloak.Config`，隨架構廢除需重評。
- `.../scripting/EmbeddedScriptBasedComponentRegistrar.java`：大量內部 SPI（`AuthenticatorSpi`、`PolicySpi`、`ProtocolMapperSpi`、`Profile.Feature.SCRIPTS`），新版變動極大，須整體重寫或廢除。
- `.../EmbeddedKeycloakServer.java`：依賴 `Profile`/`Version`，隨架構廢除需重新設計。
- `keycloak-spring-boot-starter/src/main/resources/infinispan.xml`：Infinispan 11→15 與 JGroups 4→5 後 schema 可能需更新。

#### 升級步驟

- [ ] **步驟 0（決策關卡，最重要）**：go/no-go 決策——確認業務上是否仍需「在 SB 程序內嵌 Keycloak」。鑑於上游已於 Keycloak 17 移除嵌入式路線、26.x 為 Quarkus-only，原地升級不可行。建議：(A) Keycloak 以獨立 standalone（Quarkus 容器/官方 image）部署，本專案改用 `spring-boot-starter-oauth2-client` / `oauth2-resource-server`；或 (B) 暫時凍結此模組、不納入 SB4 升級批次。
- [ ] 步驟 1（採方案 A，建議）：廢除六大嵌入式類別與 SPI 註冊，新建以標準 OIDC client 為核心的精簡 starter，依賴改 SB4 BOM 管理的 oauth2 starter，移除所有嵌入式依賴。
- [ ] 步驟 2（若堅持原地升級，不建議）：pom parent 改 4.0.0、java.version 17、移除 raw.github repo、base-starter 對齊 reactor。
- [ ] 步驟 3：spring.factories 的 `EnableAutoConfiguration` 遷至 `AutoConfiguration.imports`。
- [ ] 步驟 4：全模組 javax.servlet.* / javax.ws.rs.* → jakarta.*（javax.sql、javax.naming 屬 JDK 不遷移）。
- [ ] 步驟 5：嘗試升 Keycloak 並替換內部 API——預期卡關，此即驗證方案 A 為唯一可行路線。
- [ ] 步驟 6：升 Infinispan 15 / JGroups 5，更新 infinispan.xml 並驗證叢集快取。
- [ ] 步驟 7：因不在 reactor，須 `mvn -f keycloak-spring-boot-starter/pom.xml clean install` 單獨建構，評估是否納入 reactor。
- [ ] 步驟 8：建立 `doc-site/docs/keycloak-starter/` 文件，記錄架構變更與遷移決策。

#### 驗證方式

- [ ] 單模組建構：`mvn -f keycloak-spring-boot-starter/pom.xml clean install`。
- [ ] 啟動煙霧：以 example-keycloak 或最小 Boot app 引入啟動，確認 `AutoConfiguration.imports` 載入、Keycloak context path（預設 /auth）可回應 well-known OIDC discovery。
- [ ] 若採方案 A：以 `spring-boot-starter-oauth2-client` 對接 standalone Keycloak，驗證 authorization code 登入流程與 token 驗證。
- [ ] jakarta 遷移驗證：grep 確認無 javax.servlet / javax.ws.rs 殘留（javax.sql、javax.naming 例外）。
- [ ] spring.factories 驗證：確認 AutoConfiguration 改由 `AutoConfiguration.imports` 生效。
- [ ] 依賴衝突檢查：`dependency:tree` 確認無 javax/jakarta servlet 雙版本、無 RESTEasy 3 與 6 並存、base-starter 為 jakarta 版。
- [ ] 文件同步：建立/更新 `doc-site/docs/keycloak-starter/` 並 `npm run build` 重新產生 llms.txt / llms-full.txt。

---

## 5. 建議升級順序（依相依拓樸）

```
根 pom（parent 4.0.0 + 版本號規則 + 依賴治理）
  └─ base-spring-boot-starter（核心被依賴層，最先升）
        ├─ db-spring-boot-starter ─┐
        ├─ job-spring-boot-starter ─┤ 可平行（皆只依賴 base）
        ├─ web-spring-boot-starter ─┤
        └─ logon-spring-boot-starter┘
              └─ web-service-spring-boot-starter（CXF/HttpClient 高風險，獨立後段）
                    └─ starters_example（整合驗證）
                          └─ keycloak-spring-boot-starter（獨立評估，go/no-go，不阻塞主線）
```

- [ ] **第 1 階段 — 根 pom**：升 parent 至 4.0.0、調整版本號規則 3.5.14.1 → 4.0.0.x、盤點並移除被 BOM 接管的自訂版本、處理 javax→jakarta 與 httpclient/CXF/velocity/jasperreports 等 property，並同步發布 workflow。
- [ ] **第 2 階段 — base**：核心被依賴層，須最先升級且建構成功，否則下游無法解析。
- [ ] **第 3 階段 — db / job / web / logon（可平行）**：四者皆只依賴 base，互不相依，可並行升級與驗證。
- [ ] **第 4 階段 — web-service**：CXF 4.2 與 HttpClient 5 為高風險，建議獨立於後段處理，避免干擾其他模組驗證。
- [ ] **第 5 階段 — starters_example 整合驗證**：以升級後的各 starter 做端到端煙霧測試（啟動、SOAP/WebService、報表、Velocity、登入、排程、視圖）。
- [ ] **第 6 階段 — keycloak（獨立評估）**：不在 reactor、不阻塞主線。先做 go/no-go 決策（建議方案 A：standalone + oauth2 client），再決定重寫或退役。

---

## 6. 待查證的未知數彙總

### 根 pom / 跨模組

- [ ] SB4 `spring-boot-dependencies 4.0.0` BOM 實際管理哪些座標與版本（commons-io / commons-collections4 / commons-beanutils / poi / okhttp / httpclient5 / jakarta-servlet / jakarta-mail）。
- [ ] `central-publishing-maven-plugin` 0.4.0 之後的最新穩定版號。
- [ ] 發布 workflow（`.github/workflows`）目前版號擷取/回寫腳本內容與 tag 規則調整範圍。

### base

- [ ] `velocity-engine-core` 2.4.x 中 `initWebPath` 所需 `WebappLoader` 的確切座標與類別（`velocity-tools-view` 是否提供 jakarta 相容版，或此功能須廢除）。
- [ ] jasperreports 7.0.x 精確 GA 版號、模組拆分、PDF 後端是否仍依賴 `com.lowagie:itext` 或改 OpenPDF；現有 exclusions 是否仍有效。
- [ ] `com.lowagie:itext` 版本範圍 `[1.02b,2.1.7]` 在 Maven 4 嚴格解析下的行為。
- [ ] 是否有下游依賴 base 傳遞而來的 `com.sun.mail:javax.mail` 或 logback-ext-spring。

### db

- [ ] SB4 BOM 對 mysql-connector-j 與 mariadb-java-client 的確切版本。
- [ ] `org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties` 在 SB4 是否變更套件位置。
- [ ] p6spy 是否已釋出對 Hibernate 7 / SB4 明確背書的版本；3.9.1 對 Java 17 實測相容性。
- [ ] jt400 21.0.6 的 Java baseline 與 20.x→21.x breaking change。
- [ ] Hibernate 7.1 預設方言自動偵測與 hbm2ddl 行為變更對手動組裝路徑的影響。
- [ ] SF7 JSpecify 對 `AbstractRoutingDataSource.determineCurrentLookupKey()`（可回傳 null）是否產生編譯警告。

### job

- [ ] quartz starter 在 SB4 是否額外帶入新 transitive 套件（Quartz 2.5.1 對 c3p0/HikariCP 依賴調整）。
- [ ] Quartz 2.5.1 相對 2.3.x 是否有行為差異（misfire、JDBC JobStore schema）。
- [ ] SB4 對 `@ConditionalOnExpression` 與 `@PropertySource` 載入順序是否有行為調整。

### logon

- [ ] `DaoAuthenticationProvider` 無參數建構子在 SS7.0 GA 是「標 @Deprecated 仍可編譯」或「已完全移除」。
- [ ] allow-uris 實務上是否使用 Ant 萬用樣式；`PathPatternRequestMatcher` 對部分 Ant 樣式語意差異。
- [ ] Lombok 對 SB4 / JDK 編譯期相容版本。
- [ ] spring-security-test 在 SS7 是否有測試 API 破壞性變更。

### web

- [ ] SB4 BOM 是否仍管理 `org.glassfish.web:jakarta.servlet.jsp.jstl` 與 `jakarta.servlet.jsp.jstl-api`。
- [ ] `ServletWebServerConfiguration` 與 `@AutoConfigureBefore` 語意是否與舊 `ServletWebServerFactoryAutoConfiguration` 完全等價。
- [ ] 改用 `spring-boot-starter-webmvc` 後 `spring-boot-starter-tomcat` 是否仍為傳遞相依。
- [ ] Tomcat 11 embedded + tomcat-embed-jasper 對既有 JSP 編譯相容性。
- [ ] `CookieLocaleResolver` 在 SF7 是否標記 deprecated。

### web-service

- [ ] cxf-spring-boot-starter-jaxws / cxf-rt-databinding-jaxb 確切可用 4.2.x patch 版號。
- [ ] jaxws-ri 4.0.1 是否需升版或可移除改由 CXF 4.2 傳遞。
- [ ] `org.springframework.boot.web.servlet.ServletRegistrationBean` 套件/artifact 在 SB4 是否變動。
- [ ] `org.apache.cxf.helpers.IOUtils` 在 CXF 4.2 是否仍在相同套件。
- [ ] CXF 4.2 的 `JAXBDataBinding setNamespaceMap` / `EndpointImpl` 發布行為是否與 4.0 一致。
- [ ] `EntityUtils.toString` 在 HC5 的編碼參數簽章（接受 `Charset` 而非字串）。

### keycloak

- [ ] 現代 Keycloak（26.x）是否還提供任何可嵌入 JVM 的 library artifact（依現有資料判斷為否）。
- [ ] RESTEasy Reactive 是否有對應 `HttpServlet30Dispatcher` / `ResteasyProviderFactory` 的替代 API（預期無）。
- [ ] Infinispan 15 + JGroups 5 與所選 Keycloak 版本的相容矩陣與 infinispan.xml schema 變更。
- [ ] 業務端是否真的依賴「嵌入式」特性（決定走方案 A 或退役）。
- [ ] 此模組是否仍被 example-keycloak 實際引用，退役是否有下游衝擊。

---

## 7. 整體驗證策略

### 7.1 建構驗證

- [ ] 各模組逐一 `mvn -pl <module> -am clean install`（依升級順序：base → db/job/web/logon → web-service）。
- [ ] 根目錄 `mvn clean install` 全量建構通過（6 個 reactor 模組皆 BUILD SUCCESS）。
- [ ] `mvn dependency:tree` 全專案檢查：無殘留 `javax.servlet` / `org.apache.http`（HC4）/ `com.sun.mail:javax.mail` / `velocity(1.7)` / `logback-ext-spring`；確認 CXF 全為 4.2.x、httpclient5、jakarta.servlet-api 6.1.0、velocity-engine-core 2.4.x、jasperreports 7.x 正確進場。
- [ ] `mvn dependency:analyze` 檢查版本衝突、未宣告/多餘依賴。

### 7.2 單元/模組測試

- [ ] base：AesUtilTest / DESedeUtilTest / Md5UtilTest / OkHttpUtilTest 全綠。
- [ ] db：ConditionsTest / BaseJDBCConditionsIntegrationTest 全綠；多資料來源切換、HikariCP 7、p6spy 日誌驗證。
- [ ] job：QuartzJobUtilTest / QuartzJobPropertyConfigTest 全綠；JobStore 與 REST API 驗證。
- [ ] logon：SecurityPropertyConfigTest / LdapUserDetailsServiceTest 全綠；BASIC/LDAP/CUSTOM 三模式。
- [ ] web：JSP / Thymeleaf / `/rest` 系列 / 靜態資源 / Locale 切換。
- [ ] web-service：ClientTest / XxeSecurityTest 全綠；SOAP 收送與 CDATA 攔截器、XXE 防護。

### 7.3 整合驗證（starters_example）

- [ ] 以升級後各 starter 啟動 starters_example，端到端煙霧測試：應用啟動、SOAP/WebService 呼叫、JasperReports 報表匯出、Velocity 模板渲染、登入流程、Quartz 排程、JSP/Thymeleaf 視圖、`@ResponseResultBody` 統一回應。

### 7.4 發布與 CI 驗證

- [ ] `mvn -Prelease clean verify` 在不 deploy 下驗證 release profile（javadoc / gpg / central-publishing）外掛可正常執行。
- [ ] CI 上以一個測試 Release tag 驗證版號回寫 workflow 產出 `4.0.0.x` 正確。

### 7.5 文件同步（doc-sync）

- [ ] 依 doc-sync 規則同步各 `doc-site/docs/<module>/`（quickstart 依賴座標、configuration、architecture）、根與各模組 README 寫死的版本號與套件路徑。
- [ ] doc-site 執行 `npm run build` 重新產生根目錄 `llms.txt` / `llms-full.txt`。
- [ ] 為 keycloak 建立/更新 `doc-site/docs/keycloak-starter/`，記錄架構變更與遷移決策。

---

## 8. 執行進度與補充發現（live log）

> 本節於實際施工時更新，補記原始分析未預見的事項與已完成項目。

### 8.1 重要補充發現

- **Jackson 2 → 3（原分析未預見的核心項）**：SB4 的 `spring-boot-starter-json`
  改帶 **Jackson 3**（`tools.jackson.core:jackson-databind:3.0.2`），舊
  `com.fasterxml.jackson.databind`（Jackson 2）已不在編譯 classpath（僅保留
  `jackson-annotations:2.20`）。受影響並已遷移：
  - `base-spring-boot-starter`：`BeanUtil`、`DateSerializer`、`LowerCaseKeyDeserializer`
  - `web-spring-boot-starter`：`ResponseResultBodyAdvice`
  - 遷移要點：package 改 `tools.jackson.*`；`ObjectMapper` 不可變，改以
    `JsonMapper.builder().addModule(...).build()`；`SerializerProvider` →
    `SerializationContext`；checked `JsonProcessingException` → unchecked `JacksonException`。
- **SB4 模組化套件位移（已實測各 jar 確認新路徑）**：
  - `HibernateProperties` → `org.springframework.boot.hibernate.autoconfigure`
  - `QuartzDataSource` → `org.springframework.boot.quartz.autoconfigure`
  - `DataSourceProperties` → `org.springframework.boot.jdbc.autoconfigure`
  - `TomcatServletWebServerFactory` → `org.springframework.boot.tomcat.servlet`
  - `ConfigurableServletWebServerFactory` → `org.springframework.boot.web.server.servlet`
  - `ServletWebServerFactoryAutoConfiguration` → 改名
    `org.springframework.boot.web.server.autoconfigure.servlet.ServletWebServerConfiguration`

### 8.2 已完成（feature/upgrade-spring-boot-4）

- [x] **Phase 1 根 pom**：parent 4.0.0、版本 4.0.0.1、六子模組對齊；SB4 BOM 解析驗證通過。
- [x] **Phase 2 base-starter**：Jackson 3 + jakarta.servlet 遷移；移除 logback-ext-spring /
      com.sun.mail:javax.mail（未使用）。建構 + 12 測試綠燈。
- [x] **Phase 3 db / job / web / logon**：模組化 import 修正、SS7 DaoAuthenticationProvider、
      web 的 Jackson 3。建構 + 14 測試綠燈。

### 8.3 決策記錄

- **velocity 1.7 / jasperreports 6.21.3 暫不升級**：兩者於 SB4 / Java 17 仍可編譯運行且測試
  綠燈，升級為行為敏感且零測試覆蓋，故保留現狀，待整合驗證階段若有實際問題再處理。
- **velocity 升 2.x 取消**：原計畫的設定鍵改寫與 velocity-tools `WebappLoader`（本即未引入）
  風險高、無測試，暫不執行。
- **keycloak go/no-go = NO-GO（凍結 legacy）**：嵌入式 Keycloak Server 架構自 Keycloak 17
  （2022-06）改 Quarkus 後已被上游移除，Keycloak 13 + RESTEasy 3.15 + Infinispan 11 全為 javax
  （Jakarta EE 8），無法升級至 SB4 / Jakarta EE 11。決定保留模組現狀（本在 reactor 外、SB 2.4.4 /
  Java 11），於 README 標記 LEGACY / 未支援 SB4。日後若需 SB4 整合 Keycloak，應改用獨立 Quarkus
  伺服器 + Spring Security OAuth2/OIDC（另案新開發）。

### 8.4 整合驗證發現與處置

- **logon BASIC 認證回歸（已修復）**：整合測試（MySQL+PostgreSQL 容器）發現 controller
  端點回 401。根因非原先預判的 `PathPatternRequestMatcher`，而是 `BasicUserServiceImpl`
  以 `User.builder()` 建立 UserDetails 時僅設 `passwordEncoder` 未設 `password()`，SS7 下
  儲存密碼無法比對 → BadCredentials。已補 `.password(passwd)` 修復；example controller 測試
  改以 `httpBasic("admin","admin")` 端到端驗證 BASIC 登入，現 200 通過。
- **starters_example 整合結果**：18 測試 15 通過。多資料源動態切換、跨 DB 類型切換、
  JasperReport、Excel 匯出、Crypto、BASIC 登入皆綠燈。剩 3 個為環境性（非升級回歸）：
  `TestImportExcel`（讀硬編路徑 `d:\tmp`）、`ExampleWebServiceTest.getUserByClientUtil`
  （需 localhost:8080 活著的 SOAP 端點）。
- **doc-sync 版本號（已完成）**：發布版號採 `4.0.0.0`，已同步 doc-site/docs、README 與
  根 `llms.txt` / `llms-full.txt`。實際發布版仍由 release tag 決定。