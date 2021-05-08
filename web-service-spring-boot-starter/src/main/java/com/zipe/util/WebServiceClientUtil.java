package com.zipe.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import java.util.Objects;

/**
 * WebService客戶端呼叫工具
 *
 * @author Gary Tsai
 */
@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class WebServiceClientUtil {

    private String wsdlUrl;

    private String methodName;

    private Object[] params;

    /**
     * Call wsdl do not need username and password
     *
     * @return
     */
    public Object[] invoke() throws Exception {
        return this.invoke(null, null);
    }

    /**
     * Call wsdl need username and password
     *
     * @param username
     * @param password
     * @return
     */
    public Object[] invoke(String username, String password) throws Exception {
        // 創建動態客戶端
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        Client client = dcf.createClient(wsdlUrl);
        // 需要密碼的情況需要加上用戶名和密碼
        if (StringUtils.isBlank(username)) {
            client.getOutInterceptors().add(new ClientLoginInterceptor(username, password));
        }
        Object[] objects;
        try {
            // invoke("方法名",參數1,參數2,參數3....);
            objects = client.invoke(methodName, Objects.isNull(params) ? new Object[]{} : params);
        } catch (Exception e) {
            log.error("呼叫 {}, method:{} 失敗!!", wsdlUrl, methodName);
            throw e;
        }
        return objects;
    }

}
