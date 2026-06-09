package com.zipe.base.controller;

import com.zipe.util.string.StringConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.ModelAndView;

// FIXME(待修 Bug)：Spring Boot 3.x 應使用 jakarta.servlet.*。目前 import javax.servlet（因遞移相依仍可編譯），
//   但執行期容器提供的是 jakarta 型別，繼承此類別的 Controller 注入 request/response 會失敗。應改為 jakarta.servlet.*。
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 基礎控制類別，供各業務 Controller 繼承使用。
 * <p>
 * 提供國際化訊息取得（{@link #getMessage}）、Spring {@link Environment} 存取，
 * 以及子類別常用的 {@link HttpServletRequest}、{@link HttpServletResponse}、
 * {@link Locale} 等受保護欄位，並定義初始頁面抽象方法。
 * </p>
 *
 * @author : Gary Tsai
 */
public abstract class BaseController {

    /** Spring 環境物件，供讀取設定屬性（properties / yaml）使用。 */
    private Environment env;

    /** Spring 國際化訊息來源，用於依 Locale 解析訊息鍵值。 */
    private MessageSource messageSource;

    /** 當前 HTTP 請求物件，由子類別或攔截器注入後使用。 */
    protected HttpServletRequest request;

    /** 當前 HTTP 回應物件，由子類別或攔截器注入後使用。 */
    protected HttpServletResponse response;

    /** 當前請求的語系，用於國際化訊息解析。 */
    protected Locale currentLocale;

    /** 預設訊息字串，可於子類別初始化時設定為備用顯示文字。 */
    protected String defaultMsg;

    /**
     * 初始頁面
     *
     */
    public abstract ModelAndView initPage();

    /**
     * 依據訊息鍵值與可變參數，從 {@link MessageSource} 取得對應語系的國際化訊息字串。
     * <p>
     * 若 {@code key} 為空白字串，直接回傳空字串常數，避免向 MessageSource 發出無效查詢。
     * </p>
     *
     * @param key  訊息鍵值（對應 i18n properties 中的 key）
     * @param args 訊息佔位符的替換參數，可傳入零個或多個
     * @return 已解析的國際化訊息字串；若 {@code key} 為空白則回傳空字串
     */
    protected String getMessage(String key, String... args) {
        // key 為空白時直接回傳空字串，避免 MessageSource 拋出 NoSuchMessageException
        if (StringUtils.isBlank(key)) {
            return StringConstant.BLANK;
        }

        return messageSource.getMessage(key, args, currentLocale);
    }

    /**
     * 取得 Spring {@link Environment} 物件，供子類別讀取設定屬性。
     *
     * @return 當前的 {@link Environment} 實例
     */
    protected Environment getEnv() {
        return env;
    }

    /**
     * 由 Spring 自動注入 {@link Environment}。
     * <p>
     * 宣告為 {@code final} 防止子類別覆寫，確保注入行為一致。
     * </p>
     *
     * @param env Spring 容器提供的 {@link Environment} 實例
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public final void setEnv(Environment env) {
        this.env = env;
    }

    /**
     * 取得 {@link MessageSource} 物件，供子類別直接存取國際化訊息來源。
     *
     * @return 當前的 {@link MessageSource} 實例
     */
    protected MessageSource getMessageSource() {
        return messageSource;
    }

    /**
     * 由 Spring 自動注入 {@link MessageSource}。
     * <p>
     * 宣告為 {@code final} 防止子類別覆寫，確保注入行為一致。
     * </p>
     *
     * @param messageSource Spring 容器提供的 {@link MessageSource} 實例
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public final void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
}
