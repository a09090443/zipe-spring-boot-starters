package com.zipe.util.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttpUtil {
    public final static int READ_TIMEOUT = 100;
    public final static int CONNECT_TIMEOUT = 60;
    public final static int WRITE_TIMEOUT = 60;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType XML = MediaType.parse("application/xml; charset=utf-8");
    private static final byte[] LOCKER = new byte[0];
    private static OkHttpUtil mInstance = new OkHttpUtil();
    private OkHttpClient mOkHttpClient;

    private static final String KEY_STORE_TYPE_P12 = "PKCS12";//證書類型
    private static final String KEY_STORE_PASSWORD = "csrysd200628";//ca.p12證書密碼（客戶端證書密碼）

    /**
     * 自定义网络回调接口
     */
    public interface NetCall {
        void success(Call call, Response response) throws IOException;

        void failed(Call call, IOException e);
    }

    private OkHttpUtil() {
    }

    private OkHttpClient getOkHttpClient() throws Exception {
        if (mOkHttpClient != null) {
            return mOkHttpClient;
        }
        // 啟用https, 客戶端證書(雙向認證，需銀行提供客戶端證書)
        // KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE_P12);
        // keyStore.load(new FileInputStream("ca.p12"), KEY_STORE_PASSWORD.toCharArray());

        // KeyManagerFactory證書管理類
        // KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        // keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());

        TrustManager[] trustManagers = new TrustManager[]{new TrustAllCerts()};

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        //讀取超時
        clientBuilder.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        //連接超時
        clientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        //寫入超時
        clientBuilder.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);
        //支持HTTPS請求，跳過證書驗證
        clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0]);
        clientBuilder.hostnameVerifier((hostname, session) -> true);
        return mOkHttpClient = clientBuilder.build();
    }

    /**
     * 單例模式獲取OkHttpUtil
     */
    public static OkHttpUtil getInstance() {
        return mInstance;
    }

    /**
     * get請求，同步方式，獲取網絡數據，是在主線程中執行的，需要新起線程，將其放到子線程中執行
     *
     * @param url
     */
    public Response getData(String url) throws Exception {
        //1 建立Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().url(url).build();
        //2 將Request封裝為Call
        Call call = getOkHttpClient().newCall(request);
        //3 執行Call，得到response
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * post請求，同步方式，提交數據，是在主線程中執行的，需要新起線程，將其放到子線程中執行
     *
     * @param url
     * @param bodyParams
     */
    public Response postData(String url, Map<String, String> bodyParams) throws Exception {
        //1 建立RequestBody
        RequestBody body = setRequestBody(bodyParams);
        //2 建立Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).url(url).build();
        //3 將Request封裝為Call
        Call call = getOkHttpClient().newCall(request);
        //4 執行Call，得到response
        Response response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * get請求，異步方式，獲取網絡數據，是在子線程中執行的，需要切換到主線程才能更新UI
     *
     * @param url
     * @param netCall
     */
    public void getDataAsyn(String url, final NetCall netCall) throws Exception {
        //1 建立Request
        Request.Builder builder = new Request.Builder();
        Request request = builder.get().url(url).build();
        //2 將Request封裝為Call
        Call call = getOkHttpClient().newCall(request);
        //3 執行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                netCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                netCall.success(call, response);

            }
        });
    }

    /**
     * post請求，異步方式，提交數據，是在子線程中執行的，需要切換到主線程才能更新UI
     *
     * @param url
     * @param bodyParams
     * @param netCall
     */
    public void postDataAsyn(String url, Map<String, String> bodyParams, final NetCall netCall) throws Exception {
        //1 建立RequestBody
        RequestBody body = setRequestBody(bodyParams);
        //2 建立Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).url(url).build();
        //3 將Request封裝為Call
        Call call = getOkHttpClient().newCall(request);
        //4 執行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                netCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                netCall.success(call, response);

            }
        });
    }

    /**
     * post的請求參數，構造RequestBody
     *
     * @param bodyParams
     */
    private RequestBody setRequestBody(Map<String, String> bodyParams) {
        RequestBody body = null;
        FormBody.Builder formEncodingBuilder = new FormBody.Builder();
        if (bodyParams != null) {
            Iterator<String> iterator = bodyParams.keySet().iterator();
            String key = "";
            while (iterator.hasNext()) {
                key = iterator.next().toString();
                formEncodingBuilder.add(key, bodyParams.get(key));
                // log.info("post_Params=== {} ==== {}" + key, bodyParams.get(key));
            }
        }
        body = formEncodingBuilder.build();
        return body;

    }

    public String postXml(String url, String xml) throws Exception {
        RequestBody body = RequestBody.create(xml, XML);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = getOkHttpClient().newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    public String postJson(String url, String json) throws Exception {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = getOkHttpClient().newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    public void postJsonAsyn(String url, String json, final NetCall netCall) throws Exception {
        RequestBody body = RequestBody.create(json, JSON);
        //2 建立Request
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.post(body).url(url).build();
        //3 將Request封裝為Call
        Call call = getOkHttpClient().newCall(request);
        //4 執行Call
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                netCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                netCall.success(call, response);

            }
        });
    }

    /**
     * 用於信任所有證書
     */
    class TrustAllCerts implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public static void main(String[] args) throws Exception {
        Response response = OkHttpUtil.getInstance().getData("http://www.baidu.com");
        System.out.println(response.body().string());
    }
}
