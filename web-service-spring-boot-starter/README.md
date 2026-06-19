# web-service-spring-boot-starter

基於 Apache CXF 的 SOAP WebService 整合模組，支援服務端自動註冊與客戶端呼叫工具。

## 主要功能

- CXF SOAP WebService 服務端自動註冊（設定檔驅動）
- WebService 客戶端呼叫工具（`WebServiceClientUtil`）
- CDATA 內容攔截器（處理特殊 XML 內容）
- 客戶端登入認證攔截器
- XML 處理工具

## 引入依賴

```xml
<dependency>
    <groupId>io.github.a09090443</groupId>
    <artifactId>web-service-spring-boot-starter</artifactId>
    <version>4.0.0.1</version>
</dependency>
```

## 基本設定

```properties
# web-service.properties
# Bean 名稱對應 URI 路徑
web-service.map.user.bean-name=userWebService
web-service.map.user.uri-mapping=/ws/user
```

## 使用方式

```java
// 定義 WebService 介面
@WebService
public interface UserService {
    User getUser(Long id);
}

// 實作並以 Bean 名稱註冊
@Service("userWebService")
public class UserServiceImpl implements UserService {
    public User getUser(Long id) {
        // 實作邏輯
    }
}
```
