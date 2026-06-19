package com.zipe.handler;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.exception.LdapException;
import com.zipe.service.CustomLogonLogRecord;
import com.zipe.util.ApplicationContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * 登入失敗處理器。
 *
 * <p>繼承 {@link SimpleUrlAuthenticationFailureHandler}，在 Spring Security 認證失敗時
 * 記錄失敗原因（使用者不存在、未啟用、密碼錯誤、LDAP 連線異常等），
 * 並在設定啟用登入日誌時呼叫自訂的 {@link CustomLogonLogRecord} Bean 寫入失敗紀錄，
 * 最後將請求轉發至設定的失敗頁面。
 *
 * @author : Gary Tsai
 **/
@Slf4j
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /** Security 相關屬性設定，包含失敗轉址 URI 及登入日誌功能旗標。 */
    private final SecurityPropertyConfig securityPropertyConfig;

    /**
     * 建構 LoginFailureHandler 並初始化失敗轉址設定。
     *
     * <p>從 {@code securityPropertyConfig} 取得登入失敗後的目標 URI，並以
     * <b>Redirect（而非 Forward）</b>導向失敗頁面。</p>
     *
     * <p>不可使用 Server 端 Forward：Spring Security 6/7 會對所有 dispatcher type
     * （含 {@code FORWARD}）套用 filter chain。若 {@code login-failure-uri} 與表單登入的
     * 處理路徑（預設 {@code /login}）相同，forward 會把失敗的 {@code POST /login} 再次
     * 送進 {@code UsernamePasswordAuthenticationFilter}，重新觸發驗證、再次失敗、再次
     * forward……形成無限轉發，最終 {@code getSession} 沿 request wrapper 鏈遞迴拋出
     * {@link StackOverflowError}。改用 Redirect 後，瀏覽器以 GET 重新請求失敗頁，
     * 不會再進入認證 filter，對任何 {@code login-failure-uri} 設定皆安全。</p>
     *
     * @param securityPropertyConfig Security 屬性設定物件，提供失敗 URI 等組態
     */
    @Autowired
    public LoginFailureHandler(SecurityPropertyConfig securityPropertyConfig) {
        this.securityPropertyConfig = securityPropertyConfig;
        setDefaultFailureUrl(securityPropertyConfig.getLoginFailureUri());
        setUseForward(false);
    }

    /**
     * 認證失敗時的核心處理方法。
     *
     * <p>依例外類型分類記錄警告日誌，並在登入日誌功能啟用時，
     * 透過 {@link ApplicationContextHelper} 動態取得自訂日誌記錄 Bean
     * 呼叫其 {@code recordFailureLog} 以持久化失敗事件，
     * 最後委派父類別將請求轉發至失敗頁面。
     *
     * @param request   當前 HTTP 請求，用於取得登入帳號與用戶端 IP
     * @param response  當前 HTTP 回應，傳遞給父類別進行轉址或轉發
     * @param exception Spring Security 拋出的認證例外，類型決定失敗原因分類
     */
    @SneakyThrows
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) {
        String loginId = request.getParameter("username");
        String address = request.getRemoteAddr();
        log.info("Client ip address : {}", address);

        if (exception.getClass().isAssignableFrom(UsernameNotFoundException.class)) {
            // 無此使用者
            log.warn("使用者:{} 不存在", loginId);
        } else if (exception.getClass().isAssignableFrom(DisabledException.class)) {
            // 使用者未啟用
            log.warn("使用者:{} 未啟用", loginId);
        } else if (exception.getClass().isAssignableFrom(BadCredentialsException.class)) {
            // 帳號或密碼錯誤
            log.warn("使用者:{} 帳號或密碼錯誤", loginId);
        } else if (exception.getClass().isAssignableFrom(LdapException.class)) {
            // LDAP 連線愈時
            log.warn("帳號系統連線無回應");
        } else {
            log.warn("登入錯誤");
        }

        // 若設定啟用登入日誌，以 Bean 名稱動態取得自訂記錄實作並寫入失敗紀錄
        if(Boolean.TRUE.equals(securityPropertyConfig.getRecordLogEnable())){
            if(StringUtils.isBlank(securityPropertyConfig.getCustomRecordLogBean())){
                throw new Exception("The Custom-Record-Log must have value while Record-Log-Enable = true");
            }
            CustomLogonLogRecord logRecord = (CustomLogonLogRecord) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomRecordLogBean());
            logRecord.recordFailureLog(loginId);
        }

        super.onAuthenticationFailure(request, response, exception);
    }

}
