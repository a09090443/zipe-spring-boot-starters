package com.zipe.handler;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.service.CustomLogonLogRecord;
import com.zipe.util.ApplicationContextHelper;
import com.zipe.util.UserInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * 登入成功處理器。
 *
 * <p>繼承 {@link SavedRequestAwareAuthenticationSuccessHandler}，在使用者通過 Spring Security
 * 驗證後執行後續動作：記錄除錯日誌、選擇性呼叫自訂登入紀錄服務，最後將使用者導向
 * 設定的預設成功頁面或原先欲存取的受保護資源。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/20 上午 11:43
 **/
@Slf4j
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    /** Security 相關設定屬性，包含登入成功後的導向 URI 與登入日誌記錄選項。 */
    private final SecurityPropertyConfig securityPropertyConfig;

    /**
     * 建構子，依據設定初始化預設導向 URI。
     *
     * <p>{@code setAlwaysUseDefaultTargetUrl(false)} 表示若請求中帶有原先欲存取的 URL，
     * 則優先導向該 URL，而非固定跳轉到預設頁面。</p>
     *
     * @param securityPropertyConfig Security 設定屬性物件
     */
    public LoginSuccessHandler(SecurityPropertyConfig securityPropertyConfig) {
        setDefaultTargetUrl(securityPropertyConfig.getLoginSuccessUri());
        setAlwaysUseDefaultTargetUrl(false);
        this.securityPropertyConfig = securityPropertyConfig;
    }

    /**
     * 使用者驗證成功後的回呼方法。
     *
     * <p>若設定啟用登入日誌記錄（{@code record-log-enable=true}），會從 Spring 容器中
     * 取得自訂的 {@link CustomLogonLogRecord} Bean 並呼叫其記錄方法；若未設定
     * Bean 名稱則拋出例外以防止靜默失敗。最後呼叫父類別完成重導向。</p>
     *
     * @param request        HTTP 請求物件
     * @param response       HTTP 回應物件
     * @param authentication 已通過驗證的 Authentication 物件，包含使用者主體資訊
     * @throws Exception 當啟用日誌記錄但未設定 customRecordLogBean 名稱時拋出
     */
    @SneakyThrows
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        log.debug("User:{}, login uri:{}", UserInfoUtil.loginUserId(), request.getContextPath());
        log.debug("Client ip:{}", getIpAddress(request));
        if(securityPropertyConfig.getRecordLogEnable()){
            // 啟用日誌記錄時，必須指定自訂 Bean 名稱，否則無法完成記錄作業
            if(StringUtils.isBlank(securityPropertyConfig.getCustomRecordLogBean())){
                throw new Exception("The Custom-Record-Log must have value while Record-Log-Enable = true");
            }
            // 透過 ApplicationContext 動態取得使用者自訂的登入紀錄實作
            CustomLogonLogRecord logRecord = (CustomLogonLogRecord) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomRecordLogBean());
            logRecord.recordLoginSuccessLog(UserInfoUtil.loginUserId());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * 從 HTTP 請求中解析用戶端真實 IP 位址。
     *
     * <p>依序檢查常見的反向代理標頭（{@code x-forwarded-for}、{@code Proxy-Client-IP} 等），
     * 若均無有效值才退回使用 {@link HttpServletRequest#getRemoteAddr()}，
     * 以確保在多層代理環境下仍能取得正確的用戶端 IP。</p>
     *
     * @param request HTTP 請求物件
     * @return 用戶端 IP 位址字串
     */
    private String getIpAddress(HttpServletRequest request) {
        // 優先從標準的 X-Forwarded-For 標頭取得，此標頭在負載平衡器或 CDN 後方最常被設置
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // WebLogic 反向代理所使用的標頭
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 所有代理標頭均無效時，直接取 Socket 層的連線來源 IP
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
