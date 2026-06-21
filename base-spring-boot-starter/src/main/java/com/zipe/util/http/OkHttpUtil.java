package com.zipe.util.http;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 基於 OkHttp3 封裝的 HTTP 請求工具類別。
 *
 * <p>採用單例模式管理 {@link OkHttpClient} 實例，提供同步與非同步兩種呼叫方式，
 * 支援 GET、POST（表單、JSON、XML）等常見請求型態。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * OkHttpUtil http = OkHttpUtil.getInstance();
 * Response resp = http.getData("https://example.com/api");
 * }</pre>
 */
@Slf4j
public class OkHttpUtil {

    /** 讀取逾時時間（秒） */
    public final static int READ_TIMEOUT = 100;

    /** 連線逾時時間（秒） */
    public final static int CONNECT_TIMEOUT = 60;

    /** 寫入逾時時間（秒） */
    public final static int WRITE_TIMEOUT = 60;

    /** JSON 媒體類型，用於建立 JSON 格式的 {@link RequestBody} */
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /** XML 媒體類型，用於建立 XML 格式的 {@link RequestBody} */
    public static final MediaType XML = MediaType.parse("application/xml; charset=utf-8");

    /** 用於建立 {@link OkHttpClient} 時的同步鎖定物件，確保執行緒安全 */
    private static final byte[] LOCKER = new byte[0];

    /** 單例實例，程式啟動時即完成初始化 */
    private static OkHttpUtil mInstance = new OkHttpUtil();

    /** 共用的 OkHttpClient 實例，延遲初始化，第一次呼叫時才建立 */
    private OkHttpClient mOkHttpClient;

    /**
     * 自訂網路回呼介面，用於非同步請求的結果通知。
     *
     * <p>呼叫端實作此介面，分別處理成功與失敗兩種情境。</p>
     */
    public interface NetCall {
        /**
         * 請求成功時呼叫。
         *
         * @param call     原始的 OkHttp {@link Call} 物件
         * @param response 伺服器回應物件
         * @throws IOException 處理回應內容時若發生 I/O 錯誤則拋出
         */
        void success(Call call, Response response) throws IOException;

        /**
         * 請求失敗時呼叫（含網路中斷、逾時等情況）。
         *
         * @param call 原始的 OkHttp {@link Call} 物件
         * @param e    造成失敗的例外
         */
        void failed(Call call, IOException e);
    }

    /**
     * 私有建構子，防止外部直接實例化；請使用 {@link #getInstance()} 取得唯一實例。
     */
    private OkHttpUtil() {
    }

    /**
     * 取得（或延遲建立）共用的 {@link OkHttpClient} 實例。
     *
     * <p>採用雙重鎖定（DCL）簡化版本：方法本身以 {@code synchronized} 修飾，
     * 若 client 尚未建立則依據預設逾時參數完成初始化。</p>
     *
     * @return 已配置好逾時設定的 {@link OkHttpClient}
     */
    synchronized OkHttpClient getOkHttpClient() {
        if (mOkHttpClient != null) {
            return mOkHttpClient;
        }
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        //讀取超時
        clientBuilder.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        //連接超時
        clientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        //寫入超時
        clientBuilder.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);
        // 採用 JDK / 平台預設的 TLS 信任鏈與主機名稱驗證，確保 HTTPS 連線可抵禦中間人攻擊。
        // 若特定情境需使用自簽憑證，請以載入指定 CA / 憑證綁定（pinning）的方式處理，
        // 切勿停用憑證或主機名稱驗證。
        return mOkHttpClient = clientBuilder.build();
    }

    /**
     * 取得 {@link OkHttpUtil} 的單例實例。
     *
     * @return 全域唯一的 {@link OkHttpUtil} 實例
     */
    public static OkHttpUtil getInstance() {
        return mInstance;
    }

    /**
     * 以同步 GET 方式向指定 URL 發送請求並取得回應。
     *
     * <p>此方法會阻塞呼叫執行緒直到收到回應，請勿在主執行緒（UI 執行緒）上呼叫，
     * 應於背景執行緒中使用。</p>
     *
     * @param url 目標資源的完整 URL
     * @return 伺服器回應物件；若發生 I/O 錯誤則回傳 {@code null}
     * @throws Exception 建立請求或取得 client 時若發生例外則拋出
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
     * 以同步 POST 表單方式向指定 URL 提交資料並取得回應。
     *
     * <p>此方法會阻塞呼叫執行緒直到收到回應，請勿在主執行緒（UI 執行緒）上呼叫，
     * 應於背景執行緒中使用。</p>
     *
     * @param url        目標資源的完整 URL
     * @param bodyParams 表單參數，鍵值對應欄位名稱與欄位值；可為 {@code null}
     * @return 伺服器回應物件；若發生 I/O 錯誤則回傳 {@code null}
     * @throws Exception 建立請求或取得 client 時若發生例外則拋出
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
     * 以非同步 GET 方式向指定 URL 發送請求，結果透過回呼通知。
     *
     * <p>此方法立即返回，OkHttp 會在背景執行緒中完成實際的網路請求，
     * 完成後呼叫 {@code netCall} 的對應方法。</p>
     *
     * @param url     目標資源的完整 URL
     * @param netCall 請求結果回呼介面，分別對應成功與失敗情境
     * @throws Exception 建立請求或取得 client 時若發生例外則拋出
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
     * 以非同步 POST 表單方式向指定 URL 提交資料，結果透過回呼通知。
     *
     * <p>此方法立即返回，OkHttp 會在背景執行緒中完成實際的網路請求，
     * 完成後呼叫 {@code netCall} 的對應方法。</p>
     *
     * @param url        目標資源的完整 URL
     * @param bodyParams 表單參數，鍵值對應欄位名稱與欄位值；可為 {@code null}
     * @param netCall    請求結果回呼介面，分別對應成功與失敗情境
     * @throws Exception 建立請求或取得 client 時若發生例外則拋出
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
     * 將表單參數 Map 轉換為 OkHttp 的 {@link RequestBody}。
     *
     * <p>若 {@code bodyParams} 為 {@code null}，則回傳空表單主體。</p>
     *
     * @param bodyParams 表單參數；可為 {@code null}
     * @return 以 URL 編碼格式組裝完成的 {@link RequestBody}
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

    /**
     * 以同步 POST 方式傳送 XML 字串至指定 URL，並以字串形式回傳回應內容。
     *
     * @param url 目標資源的完整 URL
     * @param xml 要傳送的 XML 字串
     * @return 伺服器回應的本文字串
     * @throws IOException 若 HTTP 回應碼非 2xx（請求失敗）則拋出
     * @throws Exception   建立請求或取得 client 時若發生例外則拋出
     */
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
            // 非 2xx 回應視為例外，包含回應物件資訊以便除錯
            throw new IOException("Unexpected code " + response);
        }
    }

    /**
     * 以同步 POST 方式傳送 JSON 字串至指定 URL，並以字串形式回傳回應內容。
     *
     * @param url  目標資源的完整 URL
     * @param json 要傳送的 JSON 字串
     * @return 伺服器回應的本文字串
     * @throws IOException 若 HTTP 回應碼非 2xx（請求失敗）則拋出
     * @throws Exception   建立請求或取得 client 時若發生例外則拋出
     */
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
            // 非 2xx 回應視為例外，包含回應物件資訊以便除錯
            throw new IOException("Unexpected code " + response);
        }
    }

    /**
     * 以非同步 POST 方式傳送 JSON 字串至指定 URL，結果透過回呼通知。
     *
     * <p>此方法立即返回，OkHttp 會在背景執行緒中完成實際的網路請求，
     * 完成後呼叫 {@code netCall} 的對應方法。</p>
     *
     * @param url     目標資源的完整 URL
     * @param json    要傳送的 JSON 字串
     * @param netCall 請求結果回呼介面，分別對應成功與失敗情境
     * @throws Exception 建立請求或取得 client 時若發生例外則拋出
     */
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

}
