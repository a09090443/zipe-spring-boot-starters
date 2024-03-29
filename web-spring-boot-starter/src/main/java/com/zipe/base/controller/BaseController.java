package com.zipe.base.controller;

import com.zipe.util.string.StringConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;

/**
 * 基礎控制類別
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 04:05
 */
@Component
public abstract class BaseController {

    protected Environment env;

    private final MessageSource messageSource;

    protected HttpServletRequest request;

    protected HttpServletResponse response;

    protected Locale currentLocale;

    @Autowired
    protected BaseController(Environment env, MessageSource messageSource,
        HttpServletRequest request, HttpServletResponse response) {
        this.env = env;
        this.messageSource = messageSource;
        this.request = request;
        this.response = response;
        this.currentLocale = LocaleContextHolder.getLocale();
    }


    /**
     * 初始頁面
     *
     */
    public abstract ModelAndView initPage ();

    protected String getMessage(String key, String... args) {
        if (StringUtils.isBlank(key)) {
            return StringConstant.BLANK;
        }

        return messageSource.getMessage(key, args, currentLocale);
    }

}
