# zipe-spring-boot-starters

自製的 Spring Boot Starter 集合專案，提供各種常用功能的自動配置模組，供各業務系統引入使用。

## 專案總覽

```
zipe-spring-boot-starters/
├── base-spring-boot-starter/       # 基本工具功能
├── db-spring-boot-starter/         # 資料庫動態連接
├── job-spring-boot-starter/        # Quartz 排程
├── logon-spring-boot-starter/      # 登入認證 (Spring Security / LDAP)
├── web-service-spring-boot-starter/ # CXF WebService
├── web-spring-boot-starter/        # 前端顯示 (JSP / Thymeleaf)
├── keycloak-spring-boot-starter/   # Keycloak 嵌入式服務
├── starters_example/               # 整合測試範例專案
├── example/                        # 測試工具範例
├── example-keycloak/               # Keycloak 範例
└── example-kotlin/                 # Kotlin 範例
```

---

## 各 Starter 說明

### 1. base-spring-boot-starter

基本功能的 Starter，提供各種通用工具類別。

```
base-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── autoconfiguration/
    │   └── BaseAutoConfiguration.java          # 自動配置入口
    ├── config/
    │   ├── MailPropertyConfig.java              # 郵件設定
    │   ├── ThreadPoolTaskExecutorConfig.java    # 執行緒池設定
    │   └── VelocityPropertyConfig.java          # Velocity 樣板設定
    ├── model/
    │   └── Mail.java                            # 郵件資料模型
    ├── service/
    │   ├── MailService.java                     # 郵件服務介面
    │   └── impl/MailServiceImpl.java            # 郵件服務實作
    └── util/
        ├── ApplicationContextHelper.java        # Spring Context 取得工具
        ├── LdapUtil.java                        # LDAP 工具
        ├── MapUtils.java                        # Map 工具
        ├── VelocityUtil.java                    # Velocity 樣板工具
        ├── YamlPropertySourceFactory.java       # YAML 屬性來源工廠
        ├── bean/
        │   ├── BeanUtil.java                    # Bean 轉換工具
        │   ├── DateSerializer.java              # 日期序列化
        │   ├── EnumAdapterFactory.java          # Enum 轉換工廠
        │   └── LowerCaseKeyDeserializer.java    # 小寫鍵反序列化
        ├── classloader/
        │   ├── CustomClassLoader.java           # 自訂類別載入器
        │   ├── FileClassLoader.java             # 檔案類別載入器
        │   └── JarClassLoader.java              # JAR 類別載入器
        ├── crypto/
        │   ├── AesUtil.java                     # AES 加解密
        │   ├── Base64Util.java                  # Base64 編解碼
        │   ├── Crypto.java                      # 加解密介面
        │   ├── CryptoUtil.java                  # 加解密工具
        │   ├── DESedeUtil.java                  # 3DES 加解密
        │   ├── HexUtil.java                     # 16進位工具
        │   └── Md5Util.java                     # MD5 雜湊
        ├── doc/
        │   ├── ExcelUtil.java                   # Excel 操作工具
        │   ├── JasperReportUtil.java            # JasperReport 報表工具
        │   └── (ExcelCell/ExcelLog/ExcelSheet 等 Annotation)
        ├── file/
        │   └── FileUtil.java                    # 檔案操作工具
        ├── http/
        │   └── OkHttpUtil.java                  # HTTP 請求工具 (OkHttp)
        ├── print/
        │   └── PrintUtils.java                  # 列印工具
        ├── string/
        │   ├── CommonStringUtil.java            # 字串工具
        │   ├── RandomUtil.java                  # 亂數產生工具
        │   └── StringConstant.java              # 字串常數
        ├── time/
        │   └── DateTimeUtils.java               # 日期時間工具
        └── validation/
            ├── RegexUtils.java                  # 正規表達式工具
            └── Validation.java                  # 資料驗證工具
```

**主要功能：** 郵件發送、加解密工具 (AES/3DES/MD5/Base64)、Excel/JasperReport 文件處理、HTTP 請求、字串/日期工具、Bean 轉換、類別動態載入

---

### 2. db-spring-boot-starter

資料庫功能的 Starter，支援單一或多個資料來源的動態切換。

```
db-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── autoconfiguration/
    │   ├── DataSourceAspectAutoConfiguration.java   # AOP 切面自動配置
    │   └── DataSourceConfigAutoConfiguration.java   # 資料來源自動配置
    ├── base/
    │   ├── annotation/
    │   │   ├── AnnotationHelper.java                # Annotation 工具
    │   │   ├── DS.java                              # 指定資料來源 Annotation
    │   │   └── DynamicDS.java                       # 動態資料來源 Annotation
    │   ├── aspect/
    │   │   └── DynamicDataSourceAspect.java         # 動態切換 AOP 切面
    │   ├── config/
    │   │   ├── DataSourcePropertyConfig.java        # 資料來源屬性設定
    │   │   └── P6SpyLogger.java                     # P6Spy SQL 日誌
    │   ├── database/
    │   │   ├── BaseDataSourceConfig.java            # 基礎資料來源設定
    │   │   ├── DataSourceHolder.java                # ThreadLocal 資料來源持有者
    │   │   └── DynamicDataSource.java               # 動態資料來源實作
    │   └── model/
    │       └── DynamicDataSourceConfig.java         # 動態資料來源設定模型
    ├── common/model/
    │   └── SqlQuery.java                            # SQL 查詢模型
    ├── enums/
    │   └── ResourceEnum.java                        # 資源類型 Enum
    └── jdbc/
        ├── BaseJDBC.java                            # 基礎 JDBC 操作類別
        └── criteria/
            ├── Conditions.java                      # 查詢條件
            ├── Paging.java                          # 分頁條件
            ├── Pair.java                            # 鍵值對
            └── SQL.java                             # SQL 建構器
```

**主要功能：** 多資料來源動態切換 (`@DS` Annotation)、基礎 JDBC 封裝、SQL 查詢條件建構、P6Spy SQL 監控日誌

---

### 3. job-spring-boot-starter

排程功能的 Starter，基於 Quartz 框架，支援資料庫或記憶體兩種儲存模式。

```
job-spring-boot-starter/
└── src/main/java/com/zipe/
    └── quartz/
        ├── autoconfiguration/
        │   ├── DataSourceAutoConfiguration.java     # Quartz 資料來源自動配置
        │   └── InitialJobAutoConfiguration.java     # 初始化排程自動配置
        ├── base/
        │   └── BaseJob.java                         # 排程基礎類別
        ├── config/
        │   ├── QuartzDataSourceProperties.java      # Quartz 資料來源屬性
        │   └── QuartzJobPropertyConfig.java         # Quartz 排程屬性設定
        ├── controller/
        │   └── QuartzController.java                # 排程管理 REST API
        ├── enums/
        │   ├── ScheduleEnum.java                    # 排程類型 Enum
        │   └── ScheduleJobStatusEnum.java           # 排程狀態 Enum
        ├── job/
        │   ├── HelloWorldJob.java                   # 範例排程 (Hello World)
        │   ├── QuartzJobFactory.java                # Quartz Job 工廠
        │   └── TestJob.java                         # 測試排程
        ├── model/
        │   └── Job.java                             # 排程資料模型
        ├── util/
        │   └── QuartzJobUtil.java                   # 排程管理工具
        └── vo/
            └── ScheduleJobVO.java                   # 排程 View Object
```

**主要功能：** Quartz 排程管理 (新增/修改/刪除/暫停/恢復)、支援 JDBC 或記憶體 JobStore、排程 REST API、可繼承 `BaseJob` 自訂業務邏輯

---

### 4. logon-spring-boot-starter

登入認證功能的 Starter，整合 Spring Security，支援一般表單登入、LDAP 驗證、以及自訂驗證流程。

```
logon-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── autoconfiguration/
    │   └── SecurityConfiguration.java               # Spring Security 主設定
    ├── base/service/
    │   └── SecurityBaseService.java                 # 安全性基礎服務
    ├── config/
    │   ├── LdapPropertyConfig.java                  # LDAP 屬性設定
    │   ├── SecurityInitializer.java                 # Security 初始化
    │   └── SecurityPropertyConfig.java              # Security 屬性設定
    ├── enums/
    │   ├── UserEnum.java                            # 使用者狀態 Enum
    │   └── VerificationTypeEnum.java                # 驗證類型 Enum (DB/LDAP/Custom)
    ├── exception/
    │   ├── LdapException.java                       # LDAP 例外
    │   └── UserNotActivatedException.java           # 使用者未啟用例外
    ├── handler/
    │   ├── LoginFailureHandler.java                 # 登入失敗處理器
    │   ├── LoginSuccessHandler.java                 # 登入成功處理器
    │   └── LogoutSuccessHandler.java                # 登出成功處理器
    ├── model/
    │   └── LdapUser.java                            # LDAP 使用者模型
    ├── service/
    │   ├── BasicUserServiceImpl.java                # 基本使用者服務實作
    │   ├── CommonLoginProcess.java                  # 共用登入處理流程
    │   ├── CustomLogonLogRecord.java                # 自訂登入紀錄介面
    │   └── LdapUserDetailsService.java              # LDAP UserDetails 服務
    ├── util/
    │   └── UserInfoUtil.java                        # 使用者資訊工具
    └── vo/
        └── SysUserVO.java                           # 系統使用者 View Object
```

**主要功能：** Spring Security 表單登入、LDAP 目錄服務驗證、自訂驗證類型切換 (`VerificationTypeEnum`)、登入成功/失敗處理器、自訂登入日誌記錄介面

---

### 5. web-service-spring-boot-starter

WebService 功能的 Starter，基於 Apache CXF，提供 SOAP WebService 的服務端與客戶端整合。

```
web-service-spring-boot-starter/
└── src/main/java/com/zipe/
    ├── adapt/
    │   └── CdataAdapter.java                        # CDATA XML 轉換器
    ├── autoconfiguration/
    │   ├── CxfConfigAutoConfiguration.java          # CXF 框架自動配置
    │   └── WebServiceRegisterAutoConfiguration.java # WebService 自動註冊
    ├── config/
    │   ├── Service.java                             # 服務設定 Annotation
    │   └── WebServicePropertyConfig.java            # WebService 屬性設定
    ├── interceptor/
    │   ├── CdataContentInterceptor.java             # CDATA 內容攔截器 (入)
    │   └── ResponseCdataInterceptor.java            # CDATA 回應攔截器 (出)
    ├── model/
    │   └── User.java                                # 使用者模型 (範例)
    ├── service/
    │   ├── UserService.java                         # WebService 介面 (範例)
    │   └── impl/UserServiceImpl.java                # WebService 實作 (範例)
    └── util/
        ├── ClientLoginInterceptor.java              # 客戶端登入攔截器
        ├── SoapUtil.java                            # SOAP 訊息工具
        ├── WebServiceClientUtil.java                # WebService 客戶端工具
        └── XmlUtil.java                             # XML 處理工具
```

**主要功能：** CXF SOAP WebService 服務端自動註冊、WebService 客戶端呼叫工具、CDATA 內容攔截器、客戶端認證攔截器、XML 工具

---

### 6. web-spring-boot-starter

前端顯示功能的 Starter，提供 JSP 與 Thymeleaf 兩種視圖引擎，可透過設定切換。

```
web-spring-boot-starter/
└── src/main/
    ├── java/com/zipe/
    │   ├── advice/
    │   │   └── ResponseResultBodyAdvice.java        # 統一回應包裝 Advice
    │   ├── annotation/
    │   │   └── ResponseResultBody.java              # 統一回應 Annotation
    │   ├── autoconfiguration/
    │   │   ├── TomcatAutoConfiguration.java         # Tomcat 自動配置
    │   │   ├── ViewResolverAutoConfiguration.java   # 視圖解析器自動配置
    │   │   └── WebAutoConfiguration.java            # Web 自動配置
    │   ├── base/controller/
    │   │   └── BaseController.java                  # 基礎 Controller
    │   ├── config/
    │   │   ├── JspConfig.java                       # JSP 設定
    │   │   ├── ThymeleafConfig.java                 # Thymeleaf 設定
    │   │   ├── WebPropertyConfig.java               # Web 屬性設定
    │   │   └── WebResourceConfig.java               # 靜態資源設定
    │   ├── controller/
    │   │   ├── RestfulController.java               # REST API Controller (範例)
    │   │   └── WebController.java                   # Web 頁面 Controller (範例)
    │   ├── dto/
    │   │   ├── Result.java                          # 統一回應 DTO
    │   │   └── User.java                            # 使用者 DTO
    │   ├── enums/
    │   │   └── ResultStatus.java                    # 回應狀態 Enum
    │   ├── exception/
    │   │   ├── IResultStatus.java                   # 回應狀態介面
    │   │   └── ResultException.java                 # 自訂例外
    │   └── util/
    │       └── DateFormatter.java                   # 日期格式化工具
    └── webapp/WEB-INF/
        ├── html/hello.html                          # HTML 靜態頁面
        ├── jsp/
        │   ├── hello.jsp                            # JSP 頁面
        │   └── test.jsp                             # JSP 測試頁面
        └── th/
            ├── message.html                         # Thymeleaf 頁面
            └── test.html                            # Thymeleaf 測試頁面
```

**主要功能：** JSP / Thymeleaf 視圖引擎切換、統一 REST 回應格式 (`@ResponseResultBody`)、基礎 Controller、靜態資源配置、Tomcat 嵌入式伺服器設定

---

### 7. keycloak-spring-boot-starter

Keycloak 嵌入式服務的 Starter，允許在 Spring Boot 應用中直接嵌入 Keycloak 身份認證伺服器。

```
keycloak-spring-boot-starter/
└── src/main/java/com/zipe/
    └── keycloak/
        ├── autoconfiguration/
        │   └── EmbeddedKeycloakAutoConfiguration.java  # Keycloak 嵌入式自動配置
        ├── scripting/
        │   └── EmbeddedScriptBasedComponentRegistrar.java  # 腳本元件註冊
        ├── support/
        │   ├── DynamicJndiContextFactoryBuilder.java   # 動態 JNDI 工廠
        │   ├── InfinispanCacheManagerProvider.java     # Infinispan 快取管理
        │   ├── KeycloakInitialContext.java              # Keycloak 初始化 Context
        │   ├── KeycloakUndertowRequestFilter.java      # Undertow 請求過濾器
        │   ├── PopulateKeycloakPropertiesApplicationListener.java  # 屬性初始化監聽器
        │   ├── Resteasy3Provider.java                  # RESTEasy 3 提供者
        │   ├── SpringBootConfigProvider.java           # Spring Boot 設定提供者
        │   └── SpringBootPlatformProvider.java         # Spring Boot 平台提供者
        ├── EmbeddedKeycloakApplication.java            # Keycloak 應用程式
        ├── EmbeddedKeycloakServer.java                 # Keycloak 伺服器
        ├── KeycloakCustomProperties.java               # 自訂 Keycloak 屬性
        └── KeycloakProperties.java                     # Keycloak 屬性設定
```

**主要功能：** 嵌入式 Keycloak 服務啟動、Spring Boot 整合配置、Infinispan 快取支援、Undertow 請求過濾

---

### 8. starters_example（整合測試範例）

整合所有 Starter 的測試範例專案，示範各 Starter 的使用方式與相互配合。

```
starters_example/
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │   ├── Application.java                     # 應用程式入口
    │   │   ├── config/
    │   │   │   └── LogonLogRecord.java              # 自訂登入日誌 (logon-starter)
    │   │   ├── controller/
    │   │   │   ├── RestfulController.java           # REST API 控制器
    │   │   │   └── WebController.java               # Web 頁面控制器
    │   │   ├── jdbc/
    │   │   │   └── ExampleJdbc.java                 # JDBC 查詢範例 (db-starter)
    │   │   ├── job/
    │   │   │   ├── ExampleAnnotationJob.java        # Annotation 方式排程
    │   │   │   ├── ExampleDbJob.java                # DB 模式排程
    │   │   │   ├── ExampleJob.java                  # 基本排程範例
    │   │   │   └── ExampleXmlJob.java               # XML 設定排程
    │   │   ├── model/
    │   │   │   ├── UserDetail.java                  # 使用者詳細資料模型
    │   │   │   └── UserMain.java                    # 使用者主要資料模型
    │   │   ├── repository/
    │   │   │   ├── UserDetailRepository.java        # 使用者詳細資料 Repository
    │   │   │   └── UserMainRepository.java          # 使用者主要資料 Repository
    │   │   ├── service/
    │   │   │   ├── DBExampleService.java            # 資料庫範例服務介面
    │   │   │   ├── DBExampleServiceImpl.java        # 資料庫範例服務實作
    │   │   │   ├── ExampleService.java              # 通用範例服務介面
    │   │   │   └── ExampleServiceImpl.java          # 通用範例服務實作
    │   │   └── webservice/
    │   │       ├── ExampleWebService.java           # WebService 介面
    │   │       └── impl/ExampleWebServiceImpl.java  # WebService 實作
    │   └── resources/
    │       ├── application.yml                      # 應用程式主設定
    │       ├── data-source.properties               # 資料來源設定
    │       ├── quartz-datasource.properties         # Quartz 資料來源設定
    │       ├── quartz-jobs.properties               # Quartz 排程設定
    │       ├── init/h2/
    │       │   ├── schema.sql                       # H2 建表 SQL
    │       │   └── data.sql                         # H2 初始資料
    │       ├── jasperreport/                        # JasperReport 報表模板
    │       └── logback-spring.xml                   # Logback 日誌設定
    ├── postman/
    │   └── Example.postman_collection.json          # Postman 測試集合
    └── test/java/com/example/
        ├── controller/                              # Controller 整合測試
        ├── service/                                 # Service 單元測試
        └── util/                                    # 工具類別測試 (crypto/excel/jasperreport)
```

**主要功能：** 整合所有 Starter 的完整範例、H2 記憶體資料庫快速啟動、各功能模組測試案例、Postman 測試集合

---

## 技術規格

- **Java 版本：** 17+
- **Spring Boot 版本：** 3.5.x
- **建構工具：** Maven
- **套件管理：** 各 Starter 獨立 pom.xml，發布至本地 Maven Repository

## 常用指令

```bash
# 建構並安裝單一 Starter 至本地 Maven Repository
cd <starter-directory>
./mvnw clean install

# 執行整合範例
cd starters_example
./mvnw spring-boot:run
```
