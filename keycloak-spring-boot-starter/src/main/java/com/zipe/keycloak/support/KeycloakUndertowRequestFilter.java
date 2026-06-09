package com.zipe.keycloak.support;

import org.keycloak.common.ClientConnection;
import org.keycloak.services.filters.AbstractRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

/**
 * 整合 Undertow 的 Keycloak Servlet 請求過濾器。
 *
 * <p>繼承 Keycloak 的 {@code AbstractRequestFilter}，負責在每個請求進入
 * Keycloak 嵌入式服務前，設定字元編碼並建立 {@link ClientConnection} 物件，
 * 以供 Keycloak 辨識用戶端來源位址。
 * 同時實作標準 Servlet {@link Filter} 介面，可直接掛載於 Undertow 容器的過濾鏈中。
 * </p>
 */
public class KeycloakUndertowRequestFilter extends AbstractRequestFilter implements Filter {

    /**
     * 執行請求過濾邏輯。
     *
     * <p>將請求字元編碼強制設為 UTF-8，接著透過 {@link #createClientConnection(HttpServletRequest)}
     * 建立用戶端連線資訊，並呼叫父類別的 {@code filter} 方法將後續鏈式處理委派出去。</p>
     *
     * @param servletRequest  原始 Servlet 請求
     * @param servletResponse 原始 Servlet 回應
     * @param filterChain     後續過濾器鏈
     * @throws UnsupportedEncodingException 若設定字元編碼時不支援所指定的編碼（實際上 UTF-8 永遠支援，宣告僅為相容 API）
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws UnsupportedEncodingException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        // 強制以 UTF-8 解析請求參數，避免中文等多位元組字元亂碼
        request.setCharacterEncoding("UTF-8");

        filter(createClientConnection(request), (session) -> {
            try {
                filterChain.doFilter(servletRequest, servletResponse);
            } catch (Exception e) {
                // 將受檢例外包裝為非受檢例外，以符合 lambda 的函式介面簽章
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 根據 HTTP 請求建立 Keycloak 所需的 {@link ClientConnection} 物件。
     *
     * <p>子類別可覆寫此方法以提供自訂的連線資訊實作。</p>
     *
     * @param request 當前 HTTP 請求
     * @return 封裝了用戶端位址資訊的 {@link ClientConnection} 實例
     */
    protected ClientConnection createClientConnection(HttpServletRequest request) {
        return new UndertowClientConnection(request);
    }

    /**
     * 過濾器初始化，本實作無需任何初始化操作。
     *
     * @param filterConfig 過濾器設定（未使用）
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // NOOP
    }

    /**
     * 過濾器銷毀，本實作無需任何清理操作。
     */
    @Override
    public void destroy() {
        // NOOP
    }

    /**
     * 基於 {@link HttpServletRequest} 的 Keycloak {@link ClientConnection} 實作。
     *
     * <p>封裝了從 HTTP 請求中取得用戶端來源位址的邏輯，
     * 並優先採用 {@code X-Forwarded-For} 標頭，以便在反向代理（Reverse Proxy）
     * 環境中正確辨識真實用戶端 IP。</p>
     */
    public static class UndertowClientConnection implements ClientConnection {

        /** 用於取得連線資訊的原始 HTTP 請求。 */
        private final HttpServletRequest request;

        /**
         * 以指定的 HTTP 請求建立 {@code UndertowClientConnection} 實例。
         *
         * @param request 當前 HTTP 請求，不可為 {@code null}
         */
        public UndertowClientConnection(HttpServletRequest request) {
            this.request = request;
        }

        /**
         * 取得用戶端的遠端 IP 位址。
         *
         * <p>若請求標頭中存在 {@code X-Forwarded-For}，則以其值作為真實來源 IP，
         * 適用於請求經過負載均衡器或反向代理轉發的場景；
         * 否則直接回傳 Servlet 容器所提供的遠端位址。</p>
         *
         * @return 用戶端遠端 IP 位址字串
         */
        @Override
        public String getRemoteAddr() {
            String forwardedFor = request.getHeader("X-Forwarded-For");

            // 優先使用代理傳遞的真實用戶端 IP
            if (forwardedFor != null) {
                return forwardedFor;
            }

            return request.getRemoteAddr();
        }

        /**
         * 取得用戶端的遠端主機名稱。
         *
         * @return 遠端主機名稱字串
         */
        @Override
        public String getRemoteHost() {
            return request.getRemoteHost();
        }

        /**
         * 取得用戶端的遠端連接埠號碼。
         *
         * @return 遠端連接埠號碼
         */
        @Override
        public int getRemotePort() {
            return request.getRemotePort();
        }

        /**
         * 取得伺服器端本地 IP 位址。
         *
         * @return 本地 IP 位址字串
         */
        @Override
        public String getLocalAddr() {
            return request.getLocalAddr();
        }

        /**
         * 取得伺服器端本地連接埠號碼。
         *
         * @return 本地連接埠號碼
         */
        @Override
        public int getLocalPort() {
            return request.getLocalPort();
        }
    }
}
