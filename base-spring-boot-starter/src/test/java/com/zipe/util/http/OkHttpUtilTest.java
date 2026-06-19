package com.zipe.util.http;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import javax.net.ssl.X509TrustManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 驗證 OkHttpUtil 不再停用 TLS 憑證與主機名稱驗證。
 */
class OkHttpUtilTest {

    /**
     * 不可使用「全信任」的自訂 hostnameVerifier；應採用 OkHttp 預設的
     * OkHostnameVerifier，否則任何主機名稱都會被接受而導致中間人攻擊。
     */
    @Test
    void usesDefaultHostnameVerifier() throws Exception {
        OkHttpClient client = OkHttpUtil.getInstance().getOkHttpClient();

        String verifierClass = client.hostnameVerifier().getClass().getName();
        assertTrue(verifierClass.contains("OkHostnameVerifier"),
                "應使用 OkHttp 預設 hostnameVerifier，實際為: " + verifierClass);
    }

    /**
     * 不可安裝接受所有憑證的自訂 TrustManager。安全的預設作法是不覆寫
     * sslSocketFactory（此時 OkHttp 的 x509TrustManager() 為 null，改用平台
     * 預設信任鏈）；若有自訂 TrustManager，其接受的 CA 不可為空。
     */
    @Test
    void doesNotTrustAllCertificates() throws Exception {
        OkHttpClient client = OkHttpUtil.getInstance().getOkHttpClient();

        X509TrustManager trustManager = client.x509TrustManager();
        if (trustManager != null) {
            assertTrue(trustManager.getAcceptedIssuers().length > 0,
                    "自訂 TrustManager 不可為全信任（接受的 CA 不可為空）");
        }
    }
}
