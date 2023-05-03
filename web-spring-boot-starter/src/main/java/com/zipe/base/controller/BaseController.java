package com.zipe.base.controller;

import com.zipe.util.string.StringConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;

/**
 * 基礎控制類別
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 04:05
 */
public abstract class BaseController {

    protected Environment env;

    private final MessageSource messageSource;

    protected HttpServletRequest request;

    @Autowired
    BaseController(Environment env, MessageSource messageSource, HttpServletRequest request){
        this.env = env;
        this.messageSource = messageSource;
        this.request = request;
    }

    protected HttpServletResponse response;
    protected Locale currentLocale;
    protected String defaultMsg;

    /**
     * 初始頁面
     *
     * @return
     */
    public abstract ModelAndView initPage ();

    protected String getMessage(String key, String... args) {
        if (StringUtils.isBlank(key)) {
            return StringConstant.BLANK;
        }

        return messageSource.getMessage(key, args, currentLocale);
    }

}
