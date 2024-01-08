package com.zipe.base.controller;

import com.zipe.util.string.StringConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 基礎控制類別
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 04:05
 */
public abstract class BaseController {

    private Environment env;

    private MessageSource messageSource;

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected Locale currentLocale;
    protected String defaultMsg;

    /**
     * 初始頁面
     *
     */
    public abstract ModelAndView initPage();

    protected String getMessage(String key, String... args) {
        if (StringUtils.isBlank(key)) {
            return StringConstant.BLANK;
        }

        return messageSource.getMessage(key, args, currentLocale);
    }

    protected Environment getEnv() {
        return env;
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public final void setEnv(Environment env) {
        this.env = env;
    }

    protected MessageSource getMessageSource() {
        return messageSource;
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public final void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
}
