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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * 登出成功處理器。
 *
 * <p>繼承 {@link SimpleUrlLogoutSuccessHandler}，在使用者成功登出後執行以下動作：
 * <ol>
 *   <li>清除 Session 中的使用者資訊</li>
 *   <li>若啟用登入記錄功能，呼叫自訂 {@link CustomLogonLogRecord} Bean 記錄登出日誌</li>
 *   <li>重新導向至登入成功後的預設頁面（取自 {@link SecurityPropertyConfig#getLoginSuccessUri()}）</li>
 * </ol>
 *
 * @author : Gary Tsai
 **/
@Slf4j
public class LogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    /** Security 相關屬性設定，包含登出後的跳轉 URI 與登入日誌記錄選項。 */
    private final SecurityPropertyConfig securityPropertyConfig;

    /**
     * 建構 {@code LogoutSuccessHandler}，並依設定初始化預設跳轉位址。
     *
     * @param securityPropertyConfig Security 屬性設定，包含登出後的跳轉 URI 與日誌記錄相關設定
     */
    @Autowired
    public LogoutSuccessHandler(SecurityPropertyConfig securityPropertyConfig) {
        this.securityPropertyConfig = securityPropertyConfig;
        // 設定登出後的預設跳轉位址，並強制永遠使用此位址
        setDefaultTargetUrl(securityPropertyConfig.getLoginSuccessUri());
        setAlwaysUseDefaultTargetUrl(true);
    }

    /**
     * 登出成功後的回呼方法。
     *
     * <p>依序執行：清除 Session、記錄除錯日誌、選擇性寫入登出紀錄，最後轉交父類別完成跳轉。
     *
     * @param request        HTTP 請求，用於操作 Session 與取得 Context Path
     * @param response       HTTP 回應，用於執行跳轉
     * @param authentication 目前已通過驗證的使用者資訊；若使用者本來就未登入則可能為 {@code null}
     * @throws Exception 當啟用登入記錄但未設定 Custom-Record-Log Bean 名稱時拋出
     */
    @SneakyThrows
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        destroyLoginUserInfo(request);

        if (authentication != null) {
            log.debug("user:{}, logout:{}", authentication.getPrincipal().toString(), request.getContextPath());
        }


        // 若設定要求記錄登出日誌，則透過 ApplicationContext 動態取得自訂記錄 Bean 並寫入日誌
        if(Boolean.TRUE.equals(securityPropertyConfig.getRecordLogEnable())){
            // 未設定 Bean 名稱時提早拋出例外，避免後續空指標錯誤
            if(StringUtils.isBlank(securityPropertyConfig.getCustomRecordLogBean())){
                throw new Exception("The Custom-Record-Log must have value while Record-Log-Enable = true");
            }
            CustomLogonLogRecord logRecord = (CustomLogonLogRecord) ApplicationContextHelper.getBean(securityPropertyConfig.getCustomRecordLogBean());
            // FIXME(待修 Bug)：登出成功卻呼叫 recordFailureLog()，應為 recordLogoutSuccessLog()。
            //   否則 CustomLogonLogRecord.recordLogoutSuccessLog 永遠不會被觸發，稽核日誌無法區分「登出」與「登入失敗」。
            logRecord.recordFailureLog(UserInfoUtil.loginUserId());
        }

        super.onLogoutSuccess(request, response, authentication);
    }

    /**
     * 清除 Session 中以使用者 ID 為鍵的屬性，防止殘留的使用者狀態污染下一次登入。
     *
     * @param request HTTP 請求，用於存取目前的 Session
     */
    private void destroyLoginUserInfo(HttpServletRequest request) {
        String userId = UserInfoUtil.loginUserId();
        request.getSession().removeAttribute(userId);
    }

}
