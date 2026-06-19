package com.zipe.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import java.util.Objects;

/**
 * WebService 客戶端呼叫工具。
 *
 * <p>封裝 Apache CXF {@link JaxWsDynamicClientFactory} 的動態客戶端建立流程，
 * 支援無需認證及需要帳號/密碼認證兩種 SOAP 呼叫模式。
 * 透過設定 {@code wsdlUrl}、{@code methodName} 與 {@code params} 後，
 * 呼叫 {@link #invoke()} 或 {@link #invoke(String, String)} 即可取得遠端服務回應。</p>
 *
 * @author Gary Tsai
 */
@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class WebServiceClientUtil {

    /** 目標 WebService 的 WSDL 位址（例如：{@code http://host/service?wsdl}）。 */
    private String wsdlUrl;

    /** 要呼叫的遠端服務方法名稱。 */
    private String methodName;

    /** 傳入遠端方法的參數陣列；若無參數可設為 {@code null} 或空陣列。 */
    private Object[] params;

    /**
     * 以無認證方式呼叫 WSDL 服務。
     *
     * <p>內部委派給 {@link #invoke(String, String)}，帳號與密碼均傳入 {@code null}。</p>
     *
     * @return 遠端方法回傳的結果物件陣列
     * @throws Exception 呼叫失敗時拋出，包含 CXF 用戶端建立或遠端呼叫錯誤
     */
    public Object[] invoke() throws Exception {
        return this.invoke(null, null);
    }

    /**
     * 以帳號密碼認證方式呼叫 WSDL 服務。
     *
     * <p>當 {@code username} 不為空白時，會在 CXF 出站攔截鏈中加入
     * {@link ClientLoginInterceptor}，於 SOAP 請求標頭附上認證資訊。</p>
     *
     * @param username 登入帳號；傳入 {@code null} 或空白字串表示不需認證
     * @param password 登入密碼；{@code username} 為空白時此參數無作用
     * @return 遠端方法回傳的結果物件陣列
     * @throws Exception 呼叫失敗時拋出，包含 CXF 用戶端建立或遠端呼叫錯誤
     */
    public Object[] invoke(String username, String password) throws Exception {
        // 建立動態用戶端工廠並依 WSDL 位址產生客戶端實例
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        Client client = dcf.createClient(wsdlUrl);
        if (StringUtils.isNotBlank(username)) {
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
