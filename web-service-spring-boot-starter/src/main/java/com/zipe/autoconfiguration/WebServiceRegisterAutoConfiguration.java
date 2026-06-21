package com.zipe.autoconfiguration;

import com.zipe.config.WebServicePropertyConfig;
import com.zipe.interceptor.CdataContentInterceptor;
import com.zipe.interceptor.ResponseCdataInterceptor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * WebService 自動配置類別，負責在 Spring Boot 啟動時，
 * 依據 {@link WebServicePropertyConfig} 的設定，
 * 自動將各 WebService 實作類別（Implementor）以 CXF {@link EndpointImpl} 的形式發布至指定的 URI 路徑。
 * <p>
 * 每個端點均會套用以下設定：
 * <ul>
 *   <li>停用 JAXB 驗證事件處理器，避免因空命名空間導致解析失敗</li>
 *   <li>啟用 MTOM 與 JAXB 元素解包</li>
 *   <li>註冊 {@link CdataContentInterceptor}（入站）與 {@link ResponseCdataInterceptor}（出站）攔截器</li>
 * </ul>
 *
 * @author Gary Tsai
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(WebServicePropertyConfig.class)
@EnableConfigurationProperties(WebServicePropertyConfig.class)
public class WebServiceRegisterAutoConfiguration implements InitializingBean, ApplicationContextAware {

    /** Spring 應用程式上下文，用於依名稱取得 WebService 實作 Bean。 */
    private ApplicationContext applicationContext;

    /** CXF 核心匯流排，所有端點均掛載於此 Bus 之上。 */
    private final Bus bus;

    /** WebService 屬性設定，包含各服務的 Bean 名稱與 URI 對應。 */
    private final WebServicePropertyConfig webServicePropertyConfig;

    /**
     * 建構子，透過 Spring 依賴注入取得 CXF Bus 與 WebService 屬性設定。
     *
     * @param bus                    CXF 核心匯流排
     * @param webServicePropertyConfig WebService 屬性設定物件
     */
    @Autowired
    public WebServiceRegisterAutoConfiguration(Bus bus, WebServicePropertyConfig webServicePropertyConfig) {
        this.bus = bus;
        this.webServicePropertyConfig = webServicePropertyConfig;
    }

    /**
     * 在所有屬性注入完成後執行，遍歷設定檔中的 WebService 對應表，
     * 逐一建立並發布 CXF {@link EndpointImpl} 端點。
     * <p>
     * 若某個服務發布失敗（例如 Bean 不存在），
     * 將記錄錯誤日誌並拋出 {@link RuntimeException} 以中止啟動。
     *
     * @throws RuntimeException 當任一端點發布失敗時拋出，包裝原始例外
     */
    @Override
    public void afterPropertiesSet() {

        webServicePropertyConfig.getMap().forEach((k, v) -> {
            EndpointImpl endpoint;
            try {
                Object implementor = applicationContext.getBean(v.getBeanName());
                endpoint = new EndpointImpl(bus, implementor);

                // Configure to ignore empty namespaces
                Map<String, Object> properties = new HashMap<>();
                // 停用 JAXB 驗證事件處理器，防止空命名空間導致 SOAP 訊息解析失敗
                properties.put("set-jaxb-validation-event-handler", "false");

                endpoint.setProperties(properties);

                // Set custom JAXBDataBinding
                JAXBDataBinding jaxbDataBinding = new JAXBDataBinding();
                jaxbDataBinding.setUnwrapJAXBElement(true);
                jaxbDataBinding.setMtomEnabled(true);

                // Configure namespace mapping to ignore empty namespaces
                Map<String, String> nsMap = new HashMap<>();
                // 將空命名空間前置碼對應至 XMLSchema URI，確保 JAXB 序列化/反序列化的相容性
                nsMap.put("", "http://www.w3.org/2001/XMLSchema");
                jaxbDataBinding.setNamespaceMap(nsMap);
                endpoint.setDataBinding(jaxbDataBinding);
                endpoint.setInInterceptors(Arrays.asList(new CdataContentInterceptor()));
                endpoint.setOutInterceptors(Arrays.asList(new ResponseCdataInterceptor()));
                endpoint.publish(v.getUriMapping());
                log.info("Web Service 註冊服務:{}, 對應 URI:{}", v.getBeanName(), v.getUriMapping());
            } catch (Exception e) {
                log.error("Web Service 註冊服務:{}, 失敗", v.getBeanName());
                throw new RuntimeException(e);
            }

        });
    }

    /**
     * 由 Spring 容器回呼，注入 {@link ApplicationContext}，
     * 以便後續依 Bean 名稱取得 WebService 實作物件。
     *
     * @param applicationContext Spring 應用程式上下文
     * @throws BeansException 若注入過程發生錯誤時拋出
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
